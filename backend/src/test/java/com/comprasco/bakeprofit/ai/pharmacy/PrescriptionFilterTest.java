package com.comprasco.bakeprofit.ai.pharmacy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.comprasco.bakeprofit.price.dto.CatalogOfferRow;

/** HU-39 · RF63 · RN-33: filtro de productos de fórmula médica. */
class PrescriptionFilterTest {

    /** Con la lista real del classpath y un término extra por configuración. */
    private final PrescriptionFilter filter = new PrescriptionFilter(
            new ClassPathResource("ai/rx-active-ingredients.txt"), List.of("Termino Extra"));

    @Test
    void excludesProductsWhoseNameContainsAPrescriptionActiveIngredient() {
        assertThat(filter.matchedTerm("Amoxicilina 500 mg x 50 cápsulas")).contains("amoxicilina");
        assertThat(filter.matchedTerm("LOSARTÁN POTÁSICO 50MG")).contains("losartan");
        assertThat(filter.matchedTerm("Ácido Valproico 250 mg")).contains("acido valproico");
        assertThat(filter.matchedTerm("Clonazepam-2mg gotas")).contains("clonazepam");
    }

    @Test
    void keepsOverTheCounterProducts() {
        for (String otc : List.of(
                "Acetaminofén 500 mg x 10 tabletas",
                "Loratadina 10 mg",
                "Diclofenaco gel 1% 50 g",
                "Sales de rehidratación oral",
                "Vitamina C 500 mg",
                "Alcohol antiséptico 350 ml",
                "Ibuprofeno 400 mg")) {
            assertThat(filter.matchedTerm(otc)).as(otc).isEmpty();
        }
    }

    @Test
    void matchesWholeWordsOnly() {
        // "litio" está en la lista; no debe disparar con palabras que solo lo contienen
        PrescriptionFilter onlyLitio = new PrescriptionFilter(List.of("litio"));
        assertThat(onlyLitio.matchedTerm("Carbonato de litio 300 mg")).contains("litio");
        assertThat(onlyLitio.matchedTerm("Crema Politionica")).isEmpty();
    }

    @Test
    void extraTermsFromConfigurationAreApplied() {
        assertThat(filter.matchedTerm("Producto con término extra")).contains("termino extra");
    }

    @Test
    void columnFlagExcludesEvenIfTheNameLooksOverTheCounter() {
        CatalogOfferRow flagged = row(1L, "Producto sin principio en el nombre", true);
        assertThat(filter.requiresPrescription(flagged)).isTrue();
    }

    @Test
    void excludeRemovesEveryOfferOfPrescriptionProducts_andCountsDistinctProducts() {
        List<CatalogOfferRow> rows = List.of(
                row(1L, "Acetaminofén 500 mg", false),
                row(2L, "Azitromicina 500 mg", false),
                row(2L, "Azitromicina 500 mg", false), // segunda tienda del mismo producto
                row(3L, "Gel antibacterial", true),     // marcado en la BD
                row(4L, "Loratadina 10 mg", null));     // columna sin valor = no marcado

        PrescriptionFilter.Result result = filter.exclude(rows);

        assertThat(result.allowed()).extracting(CatalogOfferRow::productId).containsExactly(1L, 4L);
        assertThat(result.excludedProducts()).isEqualTo(2);
    }

    @Test
    void normalizeRemovesAccentsCaseAndSeparators() {
        assertThat(PrescriptionFilter.normalize("  Ácido-VALPROICO/250mg ")).isEqualTo("acido valproico 250mg");
    }

    static CatalogOfferRow row(Long id, String name, Boolean flagged) {
        return new CatalogOfferRow(id, name, null, null, "Drogas La Rebaja", new BigDecimal("1000"),
                "https://tienda.example/" + id, flagged);
    }
}
