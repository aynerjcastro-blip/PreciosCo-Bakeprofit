package com.comprasco.bakeprofit.ai.pharmacy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.ai.text.SearchText;
import com.comprasco.bakeprofit.price.dto.CatalogOfferRow;

/**
 * Filtro de productos de venta bajo fórmula médica para el asistente de droguería
 * (HU-39 · RF63 · RN-33).
 *
 * <p>
 * Se aplica en el servicio, ANTES de enviar datos al modelo, y es independiente
 * del prompt y del scraper. Un producto se excluye si cualquiera de las dos
 * fuentes lo marca (DP-56):
 * </p>
 * <ol>
 * <li>La columna {@code products.requires_prescription}.</li>
 * <li>La lista de principios activos de fórmula
 * ({@code classpath:ai/rx-active-ingredients.txt} más
 * {@code preciosco.ai.rx.extra-terms}): el nombre del producto contiene el
 * término como palabra completa, sin distinguir tildes ni mayúsculas.</li>
 * </ol>
 */
@Component
public class PrescriptionFilter {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionFilter.class);

    /** Términos ya normalizados, rodeados de espacios para comparar palabras completas. */
    private final List<String> terms;

    @Autowired // hay otro constructor (para pruebas): Spring debe usar este
    public PrescriptionFilter(
            @Value("classpath:ai/rx-active-ingredients.txt") Resource list,
            @Value("${preciosco.ai.rx.extra-terms:}") List<String> extraTerms) {
        Set<String> all = new LinkedHashSet<>(readTerms(list));
        if (extraTerms != null) {
            extraTerms.stream().map(PrescriptionFilter::normalize).filter(t -> !t.isBlank()).forEach(all::add);
        }
        this.terms = all.stream().map(t -> " " + t + " ").toList();
        log.info("Filtro de fórmula médica (RN-33): {} principios activos cargados", terms.size());
    }

    /** Para pruebas: lista explícita de términos. */
    PrescriptionFilter(Collection<String> rawTerms) {
        this.terms = rawTerms.stream().map(PrescriptionFilter::normalize).filter(t -> !t.isBlank())
                .distinct().map(t -> " " + t + " ").toList();
    }

    /**
     * Principio activo de fórmula que contiene el nombre, si hay alguno.
     */
    public Optional<String> matchedTerm(String productName) {
        String name = " " + normalize(productName) + " ";
        return terms.stream().filter(name::contains).map(String::strip).findFirst();
    }

    /** ¿El producto requiere fórmula según la columna o la lista? */
    public boolean requiresPrescription(CatalogOfferRow row) {
        return row.flaggedAsPrescription() || matchedTerm(row.productName()).isPresent();
    }

    /**
     * Quita las filas de productos de fórmula.
     *
     * @return las filas permitidas y cuántos productos distintos se excluyeron
     */
    public Result exclude(List<CatalogOfferRow> rows) {
        List<CatalogOfferRow> allowed = new ArrayList<>(rows.size());
        Set<Long> excluded = new LinkedHashSet<>();
        for (CatalogOfferRow row : rows) {
            if (requiresPrescription(row)) {
                excluded.add(row.productId());
            } else {
                allowed.add(row);
            }
        }
        return new Result(allowed, excluded.size());
    }

    /** Filas que sí pueden ir al modelo y número de productos excluidos. */
    public record Result(List<CatalogOfferRow> allowed, int excludedProducts) {
    }

    /** Minúsculas, sin tildes y con cualquier separador convertido en un espacio. */
    static String normalize(String text) {
        return SearchText.normalize(text);
    }

    private static List<String> readTerms(Resource list) {
        List<String> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(list.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int comment = line.indexOf('#');
                String term = normalize(comment >= 0 ? line.substring(0, comment) : line);
                if (!term.isBlank()) {
                    result.add(term);
                }
            }
        } catch (IOException e) {
            // Sin la lista no se arranca: el filtro es obligatorio (RN-33)
            throw new UncheckedIOException("No se pudo leer la lista de principios activos de fórmula", e);
        }
        return result;
    }
}
