package com.comprasco.bakeprofit.ai.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.comprasco.bakeprofit.price.dto.CatalogOfferRow;
import com.comprasco.bakeprofit.price.repository.PriceRepository;

@ExtendWith(MockitoExtension.class)
class RecipeIngredientMatcherTest {

    @Mock
    private PriceRepository priceRepository;

    /** Búsqueda falsa: palabras (unidas por espacio) → IDs. Registra las búsquedas hechas. */
    private final Map<String, List<Long>> catalog = new ConcurrentHashMap<>();
    private final List<String> searched = new java.util.concurrent.CopyOnWriteArrayList<>();

    private RecipeIngredientMatcher matcher;

    @BeforeEach
    void setUp() {
        CatalogProductSearch search = (words, limit) -> {
            String key = String.join(" ", words);
            searched.add(key);
            return catalog.getOrDefault(key, List.of());
        };
        matcher = new RecipeIngredientMatcher(search, priceRepository);
    }

    @Test
    void ingredientWithProductAndPriceIsAvailable_withOffersSortedByPrice() {
        catalog.put("arroz", List.of(1L));
        when(priceRepository.findCatalogOffersByProductIds(anyCollection())).thenReturn(List.of(
                row(1L, "Arroz Diana", "Éxito", "5200"),
                row(1L, "Arroz Diana", "D1", "4500")));

        List<MatchedIngredient> result = matcher.match(List.of(ingredient("arroz", "arroz")));

        assertThat(result).hasSize(1);
        MatchedIngredient arroz = result.getFirst();
        assertThat(arroz.available()).isTrue();
        assertThat(arroz.best().name()).isEqualTo("Arroz Diana");
        assertThat(arroz.best().cheapest().storeName()).isEqualTo("D1");
    }

    @Test
    void ingredientNotInCatalogIsUnavailable_andSkipsPriceQueryWhenNothingFound() {
        List<MatchedIngredient> result = matcher.match(List.of(ingredient("guascas", "guascas")));

        assertThat(result.getFirst().available()).isFalse();
        assertThat(result.getFirst().best()).isNull();
        verify(priceRepository, never()).findCatalogOffersByProductIds(anyCollection());
    }

    @Test
    void triesTermsFromSpecificToGeneral_andDropsStopwords() {
        catalog.put("pollo", List.of(5L));
        when(priceRepository.findCatalogOffersByProductIds(anyCollection()))
                .thenReturn(List.of(row(5L, "Pollo entero", "D1", "15000")));

        List<MatchedIngredient> result = matcher.match(
                List.of(ingredient("pechuga de pollo", "pechuga de pollo", "pollo")));

        assertThat(result.getFirst().available()).isTrue();
        // "de" no se busca; primero el término específico, luego el general
        assertThat(searched).containsExactly("pechuga pollo", "pollo");
    }

    @Test
    void productFoundButWithoutCurrentPriceIsNotAvailable() {
        catalog.put("leche", List.of(9L));
        when(priceRepository.findCatalogOffersByProductIds(anyCollection())).thenReturn(List.of());

        assertThat(matcher.match(List.of(ingredient("leche", "leche"))).getFirst().available()).isFalse();
    }

    @Test
    void keepsRecipeOrder_andAtMostThreeProductsPerIngredient() {
        catalog.put("papa", List.of(1L, 2L, 3L, 4L));
        catalog.put("sal", List.of(7L));
        when(priceRepository.findCatalogOffersByProductIds(anyCollection())).thenReturn(List.of(
                row(1L, "Papa", "D1", "1000"), row(2L, "Papa criolla", "D1", "2000"),
                row(3L, "Papa pastusa", "D1", "3000"), row(4L, "Papa sabanera", "D1", "4000"),
                row(7L, "Sal refinada", "Éxito", "1500")));

        List<MatchedIngredient> result = matcher.match(List.of(
                ingredient("papa", "papa"), ingredient("cilantro", "cilantro"), ingredient("sal", "sal")));

        assertThat(result).extracting(m -> m.ingredient().name()).containsExactly("papa", "cilantro", "sal");
        assertThat(result.get(0).products()).hasSize(RecipeIngredientMatcher.MAX_PRODUCTS_PER_INGREDIENT);
        assertThat(result.get(1).available()).isFalse();
        assertThat(result.get(2).best().name()).isEqualTo("Sal refinada");
    }

    @Test
    void significantWordsRemovesArticlesAndPrepositions() {
        assertThat(RecipeIngredientMatcher.significantWords("Crema de leche"))
                .containsExactly("Crema", "leche");
        assertThat(RecipeIngredientMatcher.significantWords("de la")).isEmpty();
    }

    private static RecipeIngredient ingredient(String name, String... terms) {
        return new RecipeIngredient(name, "1 unidad", List.of(terms));
    }

    private static CatalogOfferRow row(Long productId, String name, String store, String value) {
        return new CatalogOfferRow(productId, name, null, null, store, new BigDecimal(value),
                "https://tienda.example/" + productId);
    }
}
