package com.comprasco.bakeprofit.ai.pharmacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Limit;

import com.comprasco.bakeprofit.ai.client.GeminiClient;
import com.comprasco.bakeprofit.ai.context.CatalogContextLoader;
import com.comprasco.bakeprofit.ai.dto.AiChatRequest;
import com.comprasco.bakeprofit.ai.dto.AiChatResponse;
import com.comprasco.bakeprofit.ai.recipe.RecipeExtractor;
import com.comprasco.bakeprofit.ai.recipe.RecipeIngredientMatcher;
import com.comprasco.bakeprofit.ai.service.AiChatService;
import com.comprasco.bakeprofit.ai.service.AiChatService.PreparedChat;
import com.comprasco.bakeprofit.ai.service.AiQuotaService;
import com.comprasco.bakeprofit.auth.entity.Plan;
import com.comprasco.bakeprofit.auth.entity.User;
import com.comprasco.bakeprofit.catalog.event.ProductPrescriptionChangedEvent;
import com.comprasco.bakeprofit.catalog.repository.CategoryRepository;
import com.comprasco.bakeprofit.catalog.repository.ProductRepository;
import com.comprasco.bakeprofit.price.dto.CatalogOfferRow;
import com.comprasco.bakeprofit.price.repository.PriceRepository;

/**
 * HU-39 · RF31 · RF63 · RN-33. Prueba el servicio completo con el catálogo real
 * (CatalogContextLoader + PrescriptionFilter) y Gemini simulado: lo que se envía
 * al modelo no contiene productos de fórmula y toda respuesta de droguería
 * termina con el aviso, sea cual sea la consulta.
 */
class PharmacyChatServiceTest {

    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final PriceRepository priceRepository = mock(PriceRepository.class);
    private final GeminiClient geminiClient = mock(GeminiClient.class);
    private final AiQuotaService quotaService = mock(AiQuotaService.class);

    private CatalogContextLoader loader;
    private AiChatService service;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        PrescriptionFilter filter = new PrescriptionFilter(
                new ClassPathResource("ai/rx-active-ingredients.txt"), List.of());
        loader = new CatalogContextLoader(
                categoryRepository, productRepository, priceRepository, filter, Duration.ofMinutes(30));
        service = new AiChatService(loader, geminiClient, quotaService,
                mock(RecipeExtractor.class), mock(RecipeIngredientMatcher.class));

        user = new User();
        user.setId(7L);
        user.setPlan(Plan.FREE);
        when(quotaService.consume(user)).thenReturn(3);
        when(quotaService.limitFor(user)).thenReturn(20);

        lenient().when(categoryRepository.findActiveChildIdsByRootName(anyString())).thenReturn(List.of(10L));
        lenient().when(productRepository.findPricedActiveIdsByCategoryIds(anyCollection(), any(Limit.class)))
                .thenReturn(List.of(1L, 2L, 3L, 4L));
        lenient().when(priceRepository.findCatalogOffersByProductIds(anyCollection())).thenReturn(List.of(
                row(1L, "Acetaminofén 500 mg x 10 tabletas", false),
                row(2L, "Amoxicilina 500 mg x 50 cápsulas", false),   // RX por principio activo
                row(3L, "Jarabe Especial Infantil", true),            // RX marcado en la BD
                row(4L, "Loratadina 10 mg", false)));
    }

    /** Consultas de síntomas (subtarea de HU-39): siempre filtro + aviso. */
    @ParameterizedTest
    @ValueSource(strings = {
            "Me duele la cabeza, ¿qué puedo tomar?",
            "Tengo gripa y congestión",
            "Necesito amoxicilina para la garganta",
            "Tengo dolor en el pecho y me falta el aire",
            "Mi bebé de 8 meses tiene fiebre",
            "Precio del acetaminofén" })
    void symptomQueries_neverSendPrescriptionProducts_andAlwaysEndWithDisclaimer(String query) throws Exception {
        List<String> chunks = new ArrayList<>();
        modelAnswers("Te recomiendo consultar a un médico.");

        PreparedChat prepared = service.prepareChat(request(query), user);
        Object done = service.stream(prepared, chunks::add);

        String prompt = capturedStreamPrompt();
        assertThat(prompt)
                .contains("Acetaminofén 500 mg", "Loratadina 10 mg")
                .doesNotContain("Amoxicilina", "Jarabe Especial Infantil");
        assertThat(chunks.getLast()).isEqualTo("\n\n" + PharmacySection.DISCLAIMER);
        assertThat(((AiChatResponse) done).disclaimer()).isEqualTo(PharmacySection.DISCLAIMER);
    }

    @Test
    void disclaimerIsAddedEvenWhenTheModelGivesNoRecommendation() throws Exception {
        List<String> chunks = new ArrayList<>();
        modelAnswers("No encontré ese producto en el catálogo.");

        service.stream(service.prepareChat(request("¿Tienen ivermectina?"), user), chunks::add);

        assertThat(String.join("", chunks))
                .isEqualTo("No encontré ese producto en el catálogo.\n\n" + PharmacySection.DISCLAIMER);
    }

    @Test
    void brandNameProductMarkedByAdminIsExcludedOnTheNextQuery() throws Exception {
        // "Cronofen" no contiene el principio activo: solo la columna puede excluirlo
        when(priceRepository.findCatalogOffersByProductIds(anyCollection())).thenReturn(List.of(
                row(1L, "Acetaminofén 500 mg x 10 tabletas", false),
                row(5L, "Cronofen 500 mg x 10", false)));
        assertThat(loader.loadForSection(PharmacySection.SLUG)).contains("Cronofen");

        // El admin lo marca: la BD ya lo devuelve marcado y llega el evento
        when(priceRepository.findCatalogOffersByProductIds(anyCollection())).thenReturn(List.of(
                row(1L, "Acetaminofén 500 mg x 10 tabletas", false),
                row(5L, "Cronofen 500 mg x 10", true)));
        loader.onPrescriptionChanged(new ProductPrescriptionChangedEvent(5L, true));

        assertThat(loader.loadForSection(PharmacySection.SLUG))
                .contains("Acetaminofén")
                .doesNotContain("Cronofen");
    }

    @Test
    void nonStreamingChatAlsoEndsWithDisclaimer() throws Exception {
        when(geminiClient.chat(anyString(), anyList())).thenReturn("Acetaminofén en Drogas La Rebaja.");

        AiChatResponse response = service.chat(request("Me duele la cabeza"), user);

        assertThat(response.reply()).endsWith(PharmacySection.DISCLAIMER);
        assertThat(response.disclaimer()).isEqualTo(PharmacySection.DISCLAIMER);
    }

    @Test
    void otherSectionsAreNotFilteredAndHaveNoDisclaimer() throws Exception {
        List<String> chunks = new ArrayList<>();
        modelAnswers("Arroz en D1.");

        PreparedChat prepared = service.prepareChat(
                new AiChatRequest("supermercados", "arroz", List.of()), user);
        Object done = service.stream(prepared, chunks::add);

        assertThat(chunks).containsExactly("Arroz en D1.");
        assertThat(((AiChatResponse) done).disclaimer()).isNull();
        // El filtro RX es solo de droguería
        assertThat(capturedStreamPrompt()).contains("Amoxicilina");
    }

    // ── Helpers ──

    @SuppressWarnings("unchecked")
    private void modelAnswers(String text) throws Exception {
        doAnswer(inv -> {
            ((Consumer<String>) inv.getArgument(2)).accept(text);
            return null;
        }).when(geminiClient).chatStream(anyString(), anyList(), any());
    }

    private String capturedStreamPrompt() throws Exception {
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).chatStream(prompt.capture(), anyList(), any());
        return prompt.getValue();
    }

    private static AiChatRequest request(String message) {
        return new AiChatRequest(PharmacySection.SLUG, message, List.of());
    }

    private static CatalogOfferRow row(Long id, String name, boolean flagged) {
        return new CatalogOfferRow(id, name, null, null, "Drogas La Rebaja", new BigDecimal("2500"),
                "https://tienda.example/" + id, flagged);
    }
}
