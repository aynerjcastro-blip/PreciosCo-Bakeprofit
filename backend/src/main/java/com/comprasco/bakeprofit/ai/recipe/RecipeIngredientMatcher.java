package com.comprasco.bakeprofit.ai.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.ai.recipe.MatchedIngredient.ProductMatch;
import com.comprasco.bakeprofit.core.utils.SearchTerms;
import com.comprasco.bakeprofit.price.dto.CatalogOfferRow;
import com.comprasco.bakeprofit.price.repository.PriceRepository;

/**
 * Vincula los ingredientes de una receta con productos reales del catálogo de
 * supermercados (SCRUM-325, RF30).
 *
 * <p>
 * La disponibilidad la decide la base de datos, no la IA: cada ingrediente se
 * busca en TODO el catálogo con sus términos (del más específico al más
 * general) y está disponible solo si algún producto con precio vigente lo
 * cubre.
 * </p>
 *
 * <p>
 * Rendimiento: las búsquedas de los ingredientes corren en paralelo (una por
 * ingrediente, en hilos virtuales) y luego los precios de todos los candidatos
 * se traen en una sola consulta.
 * </p>
 */
@Component
public class RecipeIngredientMatcher {

    /** Productos candidatos que se buscan por término. */
    static final int SEARCH_LIMIT = 8;
    /** Productos que se ofrecen por ingrediente. */
    static final int MAX_PRODUCTS_PER_INGREDIENT = 3;
    private static final int MAX_TERMS = 3;
    private static final long SEARCH_TIMEOUT_SECONDS = 15;

    /** Palabras que no ayudan a buscar ("pechuga de pollo" → "pechuga", "pollo"). */
    private static final Set<String> STOPWORDS = Set.of(
            "de", "del", "la", "las", "el", "los", "y", "e", "o", "u", "con", "sin",
            "en", "al", "a", "para", "por", "un", "una", "unos", "unas");

    private final CatalogProductSearch search;
    private final PriceRepository priceRepository;

    public RecipeIngredientMatcher(CatalogProductSearch search, PriceRepository priceRepository) {
        this.search = search;
        this.priceRepository = priceRepository;
    }

    /** Cruza cada ingrediente con el catálogo, conservando el orden de la receta. */
    public List<MatchedIngredient> match(List<RecipeIngredient> ingredients) {
        if (ingredients.isEmpty()) {
            return List.of();
        }

        // 1. Candidatos de cada ingrediente (en paralelo)
        List<List<Long>> idsPerIngredient = inParallel(ingredients, this::findCandidateIds);

        // 2. Precios vigentes de todos los candidatos en una sola consulta
        Set<Long> allIds = new LinkedHashSet<>();
        idsPerIngredient.forEach(allIds::addAll);
        Map<Long, List<CatalogOfferRow>> offersByProduct = allIds.isEmpty()
                ? Map.of()
                : groupByProduct(priceRepository.findCatalogOffersByProductIds(allIds));

        // 3. Hasta 3 productos con precio por ingrediente, en orden de relevancia
        List<MatchedIngredient> result = new ArrayList<>(ingredients.size());
        for (int i = 0; i < ingredients.size(); i++) {
            List<ProductMatch> products = idsPerIngredient.get(i).stream()
                    .filter(offersByProduct::containsKey)
                    .limit(MAX_PRODUCTS_PER_INGREDIENT)
                    .map(id -> toMatch(offersByProduct.get(id)))
                    .toList();
            result.add(new MatchedIngredient(ingredients.get(i), products));
        }
        return result;
    }

    /** Prueba los términos en orden y se queda con el primero que encuentra productos. */
    private List<Long> findCandidateIds(RecipeIngredient ingredient) {
        for (String term : ingredient.searchTerms().stream().limit(MAX_TERMS).toList()) {
            List<String> words = significantWords(term);
            if (words.isEmpty()) {
                continue;
            }
            List<Long> ids = search.searchIds(words, SEARCH_LIMIT);
            if (!ids.isEmpty()) {
                return ids;
            }
        }
        return List.of();
    }

    /** Palabras del término sin artículos ni preposiciones. */
    static List<String> significantWords(String term) {
        return SearchTerms.words(term).stream()
                .filter(w -> !STOPWORDS.contains(w.toLowerCase(Locale.ROOT)))
                .toList();
    }

    private static ProductMatch toMatch(List<CatalogOfferRow> offers) {
        CatalogOfferRow first = offers.getFirst();
        List<CatalogOfferRow> byPrice = offers.stream()
                .sorted((a, b) -> a.value().compareTo(b.value()))
                .toList();
        return new ProductMatch(first.productId(), first.productName(), first.unit(), first.brandName(), byPrice);
    }

    private static Map<Long, List<CatalogOfferRow>> groupByProduct(Collection<CatalogOfferRow> rows) {
        Map<Long, List<CatalogOfferRow>> map = new LinkedHashMap<>();
        for (CatalogOfferRow row : rows) {
            map.computeIfAbsent(row.productId(), id -> new ArrayList<>()).add(row);
        }
        return map;
    }

    private static <T, R> List<R> inParallel(List<T> items, Function<T, R> task) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<R>> futures = items.stream()
                    .map(item -> executor.submit(() -> task.apply(item)))
                    .toList();
            List<R> results = new ArrayList<>(futures.size());
            for (Future<R> future : futures) {
                results.add(future.get(SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Búsqueda de ingredientes interrumpida", e);
        } catch (ExecutionException e) {
            throw e.getCause() instanceof RuntimeException re
                    ? re
                    : new IllegalStateException("Falló la búsqueda de ingredientes", e.getCause());
        } catch (TimeoutException e) {
            throw new IllegalStateException("La búsqueda de ingredientes tardó demasiado", e);
        }
    }
}
