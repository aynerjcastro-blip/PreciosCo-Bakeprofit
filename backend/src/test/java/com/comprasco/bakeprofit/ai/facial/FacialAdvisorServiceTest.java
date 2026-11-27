package com.comprasco.bakeprofit.ai.facial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comprasco.bakeprofit.ai.client.GeminiClient;
import com.comprasco.bakeprofit.ai.context.CatalogContextLoader;
import com.comprasco.bakeprofit.ai.dto.AiChatRequest;
import com.comprasco.bakeprofit.ai.dto.AiFacialResponse;
import com.comprasco.bakeprofit.ai.exception.AiInvalidRequestException;
import com.comprasco.bakeprofit.ai.facial.FacialProductFinder.FacialGroup;
import com.comprasco.bakeprofit.ai.facial.FacialProductFinder.FacialOption;
import com.comprasco.bakeprofit.ai.recipe.RecipeExtractor;
import com.comprasco.bakeprofit.ai.recipe.RecipeIngredientMatcher;
import com.comprasco.bakeprofit.ai.service.AiChatService;
import com.comprasco.bakeprofit.ai.service.AiChatService.PreparedChat;
import com.comprasco.bakeprofit.ai.service.AiQuotaService;
import com.comprasco.bakeprofit.auth.entity.Plan;
import com.comprasco.bakeprofit.auth.entity.User;

/**
 * HU-40 · RF32 · RN-17: pruebas de recomendación con la IA simulada. Verifica
 * qué llega al modelo, la tarjeta del evento done, el aviso y la cuota.
 */
class FacialAdvisorServiceTest {

    private final AiQuotaService quota = mock(AiQuotaService.class);
    private final SkinProfileExtractor extractor = mock(SkinProfileExtractor.class);
    private final FacialProductFinder finder = mock(FacialProductFinder.class);
    private final GeminiClient gemini = mock(GeminiClient.class);

    private FacialAdvisorService advisor;
    private AiChatService chat;
    private User user;

    @BeforeEach
    void setUp() {
        advisor = new FacialAdvisorService(quota, extractor, finder);
        chat = new AiChatService(mock(CatalogContextLoader.class), gemini, quota,
                mock(RecipeExtractor.class), mock(RecipeIngredientMatcher.class));
        user = new User();
        user.setId(3L);
        user.setPlan(Plan.FREE);
        when(quota.consume(user)).thenReturn(8);
        when(quota.limitFor(user)).thenReturn(20);
    }

    @Test
    void recommendation_sendsOnlyBudgetFilteredOptions_andEndsWithDisclaimer() throws Exception {
        SkinProfile profile = new SkinProfile(true, "grasa", List.of("acne"), new BigDecimal("100000"),
                List.of(FacialProductType.LIMPIADOR, FacialProductType.SERUM));
        when(extractor.extract(anyList())).thenReturn(profile);
        when(finder.find(profile)).thenReturn(List.of(
                new FacialGroup(FacialProductType.LIMPIADOR,
                        List.of(option(1L, "Cetaphil Oil Control Limpiador", "90000")), null, true),
                new FacialGroup(FacialProductType.SERUM, List.of(),
                        option(2L, "Effaclar Serum", "120000"), true)));
        List<String> chunks = new ArrayList<>();
        modelAnswers("Te recomiendo el limpiador Cetaphil.");

        PreparedChat prepared = advisor.prepare(request("Piel grasa con acné, hasta 100 mil"), user);
        AiFacialResponse done = (AiFacialResponse) chat.stream(prepared, chunks::add);

        assertThat(prepared.systemPrompt())
                .contains("Tipo de piel: grasa", "Presupuesto por producto: $100000")
                .contains("Cetaphil Oil Control Limpiador")
                .contains("SIN OPCIONES DENTRO DEL PRESUPUESTO", "SUPERA el presupuesto");
        assertThat(chunks.getLast()).isEqualTo("\n\n" + FacialSection.DISCLAIMER);
        assertThat(done.disclaimer()).isEqualTo(FacialSection.DISCLAIMER);
        assertThat(done.queriesUsed()).isEqualTo(8);
        assertThat(done.groups()).hasSize(2);
        assertThat(done.groups().get(0).withinBudget()).hasSize(1);
        assertThat(done.groups().get(1).withinBudget()).isEmpty();
        assertThat(done.groups().get(1).cheapestOverBudget().name()).isEqualTo("Effaclar Serum");
        verify(quota, never()).refund(user);
    }

    @Test
    void missingSkinType_asksWithoutCallingTheModelAgain_andDoesNotCharge() throws Exception {
        when(extractor.extract(anyList())).thenReturn(
                new SkinProfile(true, "", List.of("manchas"), null, SkinProfileExtractor.BASIC_ROUTINE));
        List<String> chunks = new ArrayList<>();

        PreparedChat prepared = advisor.prepare(request("Quiero algo para las manchas"), user);
        AiFacialResponse done = (AiFacialResponse) chat.stream(prepared, chunks::add);

        assertThat(String.join("", chunks)).startsWith(FacialSection.ASK_SKIN_TYPE)
                .endsWith(FacialSection.DISCLAIMER);
        verify(quota).refund(user);
        verify(gemini, never()).chatStream(anyString(), anyList(), any());
        assertThat(done.queriesUsed()).isEqualTo(7);
        assertThat(done.groups()).isEmpty();
    }

    @Test
    void notAFacialRequest_isRejected_andRefunded() throws Exception {
        when(extractor.extract(anyList())).thenReturn(
                new SkinProfile(false, "", List.of(), null, SkinProfileExtractor.BASIC_ROUTINE));

        assertThatThrownBy(() -> advisor.prepare(request("¿Cuánto cuesta el arroz?"), user))
                .isInstanceOf(AiInvalidRequestException.class);
        verify(quota).refund(user);
    }

    @Test
    void modelFailure_refundsTheQuery() throws Exception {
        SkinProfile profile = new SkinProfile(true, "seca", List.of(), null,
                List.of(FacialProductType.HIDRATANTE));
        when(extractor.extract(anyList())).thenReturn(profile);
        when(finder.find(profile)).thenReturn(List.of(new FacialGroup(FacialProductType.HIDRATANTE,
                List.of(option(1L, "Crema", "60000")), null, true)));
        doAnswer(inv -> {
            throw new java.io.IOException("Gemini caído");
        }).when(gemini).chatStream(anyString(), anyList(), any());

        PreparedChat prepared = advisor.prepare(request("Piel seca"), user);
        assertThatThrownBy(() -> chat.stream(prepared, s -> { })).isInstanceOf(java.io.IOException.class);
        verify(quota).refund(user);
    }

    @Test
    void optionsTextMarksTypesMissingFromTheCatalog() {
        String text = FacialAdvisorService.optionsText(List.of(
                new FacialGroup(FacialProductType.CONTORNO_OJOS, List.of(), null, false)));

        assertThat(text).contains("Contorno de ojos", "NO HAY PRODUCTOS DE ESTE TIPO EN EL CATÁLOGO");
    }

    @SuppressWarnings("unchecked")
    private void modelAnswers(String text) throws Exception {
        doAnswer(inv -> {
            ((Consumer<String>) inv.getArgument(2)).accept(text);
            return null;
        }).when(gemini).chatStream(anyString(), anyList(), any());
    }

    private static AiChatRequest request(String message) {
        return new AiChatRequest(FacialSection.SLUG, message, List.of());
    }

    private static FacialOption option(Long id, String name, String price) {
        return new FacialOption(id, name, null, "Marca", "Cutis", new BigDecimal(price), "https://t.example/" + id);
    }
}
