package com.comprasco.bakeprofit.catalog.dto;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.comprasco.bakeprofit.core.utils.SearchTerms;
import com.comprasco.bakeprofit.price.util.PriceRange;

/**
 * Filtros de la búsqueda de productos (HU-06a y HU-06b), ya validados.
 *
 * @param words      palabras del nombre; el producto debe contenerlas todas, en cualquier orden
 * @param categoryId categoría hoja, o null
 * @param storeIds   tiendas seleccionadas, sin repetir; vacío significa todas las activas
 * @param priceRange rango del precio vigente; {@link PriceRange#UNBOUNDED} si no hay
 */
public record ProductSearchCriteria(
        List<String> words,
        Long categoryId,
        Set<Long> storeIds,
        PriceRange priceRange
) {

    public ProductSearchCriteria {
        words = List.copyOf(words);
        storeIds = Set.copyOf(storeIds);
        Objects.requireNonNull(priceRange);
    }

    /**
     * Crea los filtros desde los parámetros de la petición. Un rango inválido lanza
     * {@code InvalidSearchFilterException} (400). Los ids de tienda nulos (un "storeId="
     * vacío) se ignoran y los repetidos cuentan una vez.
     */
    public static ProductSearchCriteria of(String name, Long categoryId, Collection<Long> storeIds,
                                           BigDecimal minPrice, BigDecimal maxPrice) {
        Set<Long> distinctStoreIds = new TreeSet<>();
        if (storeIds != null) {
            storeIds.stream().filter(Objects::nonNull).forEach(distinctStoreIds::add);
        }
        return new ProductSearchCriteria(
                SearchTerms.words(name),
                categoryId,
                distinctStoreIds,
                new PriceRange(minPrice, maxPrice));
    }

    /**
     * Sin tienda ni rango no se exige ningún precio: la búsqueda de HU-06a también muestra
     * productos que todavía no tienen precio.
     */
    public boolean filtersByPrice() {
        return !storeIds.isEmpty() || priceRange.isBounded();
    }
}
