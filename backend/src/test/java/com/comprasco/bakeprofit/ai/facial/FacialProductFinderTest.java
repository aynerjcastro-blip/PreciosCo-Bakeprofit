package com.comprasco.bakeprofit.ai.facial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Limit;

import com.comprasco.bakeprofit.ai.facial.FacialProductFinder.FacialGroup;
import com.comprasco.bakeprofit.ai.facial.FacialProductFinder.FacialOption;
import com.comprasco.bakeprofit.catalog.repository.CategoryRepository;
import com.comprasco.bakeprofit.catalog.repository.ProductRepository;
import com.comprasco.bakeprofit.price.dto.CatalogOfferRow;
import com.comprasco.bakeprofit.price.repository.PriceRepository;

/** HU-40 · RF32: opciones dentro del presupuesto, ordenadas por tipo de piel y necesidad. */
class FacialProductFinderTest {

    private static final SkinProfile OILY_ACNE_100K = profile("grasa", List.of("acne"), "100000");

    @Test
    void budgetIsInclusive_andProductsOverBudgetAreLeftOut() {
        List<FacialOption> options = List.of(
                option(1L, "Limpiador Suave", "99999"),
                option(2L, "Limpiador Neutro", "100000"),     // igual al presupuesto: entra
                option(3L, "Limpiador Premium", "100001"));   // un peso más: no entra

        FacialGroup g = FacialProductFinder.group(FacialProductType.LIMPIADOR, options, OILY_ACNE_100K);

        assertThat(g.withinBudget()).extracting(FacialOption::productId).containsExactlyInAnyOrder(1L, 2L);
        assertThat(g.cheapestOverBudget()).isNull();
    }

    @Test
    void matchesForNeedAndSkinTypeGoFirst_thenCheapest() {
        List<FacialOption> options = List.of(
                option(1L, "Gel limpiador básico", "40000"),
                option(2L, "Cetaphil Oil Control Limpiador Facial", "90000"),        // piel grasa (+1)
                option(3L, "Effaclar Gel Limpiador Anti Imperfecciones", "95000"),   // acné (+2) y grasa (+1)
                option(4L, "Limpiador hidratante", "30000"));

        FacialGroup g = FacialProductFinder.group(FacialProductType.LIMPIADOR, options, OILY_ACNE_100K);

        assertThat(g.withinBudget()).extracting(FacialOption::productId).containsExactly(3L, 2L, 4L);
    }

    @Test
    void kidsProductsGoLastUnlessTheyAreTheOnlyOption() {
        List<FacialOption> options = List.of(
                option(1L, "Protector Solar Kids - Sophieskin", "43760"),
                option(2L, "Protector Solar Fluido Invisible", "54742"));

        FacialGroup g = FacialProductFinder.group(FacialProductType.PROTECTOR_SOLAR, options, OILY_ACNE_100K);

        assertThat(g.withinBudget()).extracting(FacialOption::productId).containsExactly(2L, 1L);
    }

    @Test
    void unitIsDroppedWhenTheNameAlreadyHasTheSize() {
        assertThat(FacialProductFinder.unitIfMissing("Cetaphil Limpiador 236ml", "236 ml")).isNull();
        assertThat(FacialProductFinder.unitIfMissing("Sérum Facial 30 Ml", "30 ml")).isNull();
        assertThat(FacialProductFinder.unitIfMissing("Espuma Limpiadora - Avène", "150 ml")).isEqualTo("150 ml");
        assertThat(FacialProductFinder.unitIfMissing("Crema", null)).isNull();
    }

    @Test
    void atMostThreeOptions() {
        List<FacialOption> options = List.of(option(1L, "A", "10000"), option(2L, "B", "20000"),
                option(3L, "C", "30000"), option(4L, "D", "40000"));

        assertThat(FacialProductFinder.group(FacialProductType.SERUM, options, OILY_ACNE_100K).withinBudget())
                .hasSize(FacialProductFinder.MAX_OPTIONS);
    }

    @Test
    void impossibleBudget_reportsNoneAndTheCheapestOverBudget() {
        SkinProfile tiny = profile("seca", List.of(), "5000");
        List<FacialOption> options = List.of(option(1L, "Crema A", "63960"), option(2L, "Crema B", "49960"));

        FacialGroup g = FacialProductFinder.group(FacialProductType.HIDRATANTE, options, tiny);

        assertThat(g.withinBudget()).isEmpty();
        assertThat(g.cheapestOverBudget().productId()).isEqualTo(2L);
        assertThat(g.availableInCatalog()).isTrue();
    }

    @Test
    void withoutBudgetEverythingCounts() {
        SkinProfile noBudget = profile("normal", List.of(), null);
        FacialGroup g = FacialProductFinder.group(FacialProductType.SERUM,
                List.of(option(1L, "Sérum lujo", "439000")), noBudget);

        assertThat(g.withinBudget()).hasSize(1);
    }

    @Test
    void scoreUsesWholeWordsWithoutAccents() {
        SkinProfile stains = profile("sensible", List.of("manchas"), null);
        assertThat(FacialProductFinder.score(option(1L, "Sérum Anti Manchas Piel Sensible", "1"), stains))
                .isEqualTo(3);
        // "mat" no debe coincidir dentro de "matte" ni de otras palabras
        assertThat(FacialProductFinder.score(option(2L, "Automatico", "1"), OILY_ACNE_100K)).isZero();
    }

    @Test
    void findQueriesEachSubcategoryAndPricesOnce() {
        CategoryRepository categories = mock(CategoryRepository.class);
        ProductRepository products = mock(ProductRepository.class);
        PriceRepository prices = mock(PriceRepository.class);
        when(categories.findActiveChildIdByRootAndName("Cuidado facial", "Limpiadores faciales"))
                .thenReturn(Optional.of(26L));
        when(categories.findActiveChildIdByRootAndName("Cuidado facial", "Contorno de ojos"))
                .thenReturn(Optional.empty());
        when(products.findPricedActiveIdsByCategoryIds(eq(List.of(26L)), any(Limit.class)))
                .thenReturn(List.of(1L, 2L));
        when(prices.findCatalogOffersByProductIds(anyCollection())).thenReturn(List.of(
                row(1L, "Cetaphil Oil Control Limpiador", "Farmatodo", "123800"),
                row(1L, "Cetaphil Oil Control Limpiador", "Cutis", "90000"),   // más barato en otra tienda
                row(2L, "Espuma Limpiadora Sisley", "Medipiel", "679000")));

        SkinProfile p = new SkinProfile(true, "grasa", List.of(), new BigDecimal("100000"),
                List.of(FacialProductType.LIMPIADOR, FacialProductType.CONTORNO_OJOS));
        List<FacialGroup> groups = new FacialProductFinder(categories, products, prices).find(p);

        assertThat(groups).hasSize(2);
        FacialGroup cleanser = groups.get(0);
        assertThat(cleanser.withinBudget()).singleElement().satisfies(o -> {
            assertThat(o.store()).isEqualTo("Cutis");
            assertThat(o.price()).isEqualByComparingTo("90000");
        });
        assertThat(groups.get(1).availableInCatalog()).isFalse();
    }

    private static SkinProfile profile(String skin, List<String> needs, String budget) {
        return new SkinProfile(true, skin, needs, budget == null ? null : new BigDecimal(budget),
                List.of(FacialProductType.LIMPIADOR));
    }

    private static FacialOption option(Long id, String name, String price) {
        return new FacialOption(id, name, null, null, "Tienda", new BigDecimal(price), "https://t.example/" + id);
    }

    private static CatalogOfferRow row(Long id, String name, String store, String value) {
        return new CatalogOfferRow(id, name, null, "Marca", store, new BigDecimal(value),
                "https://t.example/" + id, false);
    }
}
