package com.comprasco.bakeprofit.catalog.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;

import com.comprasco.bakeprofit.catalog.dto.ProductSearchCriteria;
import com.comprasco.bakeprofit.catalog.entity.Product;
import com.comprasco.bakeprofit.core.utils.SearchTerms;
import com.comprasco.bakeprofit.price.entity.Price;
import com.comprasco.bakeprofit.price.util.PriceRange;
import com.comprasco.bakeprofit.store.entity.Store;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Condiciones de la búsqueda de productos (HU-06a, HU-06b), una por filtro.
 *
 * <p>
 * Todos los filtros se resuelven en SQL para que la paginación cuente bien. Un filtro nuevo
 * (por ejemplo, la marca) es un método más, sin tocar los demás.
 * </p>
 */
public final class ProductSpecifications {

    // Envoltorio IMMUTABLE de unaccent (scripts/2026-11-scrum140-busqueda-sin-tildes.sql)
    private static final String UNACCENT_FUNCTION = "f_unaccent";

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String ACTIVE = "active";
    private static final String CATEGORY = "category";
    private static final String PRODUCT = "product";
    private static final String STORE = "store";
    private static final String VALUE = "value";
    private static final String REGISTRATION_DATE = "registrationDate";

    private ProductSpecifications() {
    }

    /** Une todos los filtros de la búsqueda. */
    public static Specification<Product> matching(ProductSearchCriteria criteria) {
        List<Specification<Product>> conditions = new ArrayList<>();
        conditions.add(isActive());
        if (!criteria.words().isEmpty()) {
            conditions.add(nameContainsAll(criteria.words()));
        }
        if (criteria.categoryId() != null) {
            conditions.add(inCategory(criteria.categoryId()));
        }
        if (criteria.filtersByPrice()) {
            conditions.add(hasCurrentPriceWithin(criteria.storeIds(), criteria.priceRange()));
        }
        return Specification.allOf(conditions);
    }

    public static Specification<Product> isActive() {
        return (product, query, cb) -> cb.isTrue(product.get(ACTIVE));
    }

    public static Specification<Product> inCategory(Long categoryId) {
        return (product, query, cb) -> cb.equal(product.get(CATEGORY).get(ID), categoryId);
    }

    /**
     * El nombre contiene todas las palabras, en cualquier orden, sin distinguir tildes ni
     * mayúsculas: "cafe" encuentra "Café" y "leche entera" encuentra "Entera Leche".
     */
    public static Specification<Product> nameContainsAll(List<String> words) {
        return (product, query, cb) -> {
            Expression<String> comparableName = comparable(cb, product.get(NAME));
            Predicate[] eachWord = words.stream()
                    .map(word -> cb.like(
                            comparableName,
                            comparable(cb, cb.literal(SearchTerms.containsPattern(word))),
                            SearchTerms.LIKE_ESCAPE))
                    .toArray(Predicate[]::new);
            return cb.and(eachWord);
        };
    }

    /**
     * El producto tiene un precio vigente en una tienda activa (de las seleccionadas, si las
     * hay) y ese mismo precio está dentro del rango.
     *
     * <p>
     * El precio vigente de un par producto-tienda es su periodo más reciente: la fila sin otra
     * más nueva del mismo par (por registrationDate y, si empatan, por id). El rango se evalúa
     * sobre esa fila y nunca sobre una fila histórica: si el arroz costó 3.000 y hoy cuesta
     * 5.000, no entra en un máximo de 4.000. Con varias tiendas basta con que una cumpla.
     * </p>
     */
    public static Specification<Product> hasCurrentPriceWithin(Set<Long> storeIds, PriceRange range) {
        return (product, query, cb) -> {
            Subquery<Long> currentPrice = query.subquery(Long.class);
            Root<Price> price = currentPrice.from(Price.class);
            Join<Price, Store> store = price.join(STORE);

            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.equal(price.get(PRODUCT), product));
            conditions.add(cb.isTrue(store.get(ACTIVE)));
            conditions.add(cb.not(cb.exists(newerPeriodOf(price, currentPrice, cb))));
            if (!storeIds.isEmpty()) {
                conditions.add(store.get(ID).in(storeIds));
            }
            Path<BigDecimal> value = price.get(VALUE);
            if (range.min() != null) {
                conditions.add(cb.greaterThanOrEqualTo(value, range.min()));
            }
            if (range.max() != null) {
                conditions.add(cb.lessThanOrEqualTo(value, range.max()));
            }

            currentPrice.select(price.get(ID)).where(conditions.toArray(Predicate[]::new));
            return cb.exists(currentPrice);
        };
    }

    // Un periodo posterior del mismo producto en la misma tienda; si existe, el precio ya no es vigente
    private static Subquery<Long> newerPeriodOf(Root<Price> price, Subquery<Long> outer, CriteriaBuilder cb) {
        Subquery<Long> newer = outer.subquery(Long.class);
        Root<Price> later = newer.from(Price.class);

        Path<LocalDateTime> laterStart = later.get(REGISTRATION_DATE);
        Path<LocalDateTime> priceStart = price.get(REGISTRATION_DATE);
        Path<Long> laterId = later.get(ID);
        Path<Long> priceId = price.get(ID);

        newer.select(laterId).where(
                cb.equal(later.get(PRODUCT), price.get(PRODUCT)),
                cb.equal(later.get(STORE), price.get(STORE)),
                cb.or(
                        cb.greaterThan(laterStart, priceStart),
                        cb.and(cb.equal(laterStart, priceStart), cb.greaterThan(laterId, priceId))));
        return newer;
    }

    // Misma transformación a los dos lados del LIKE: primero sin tildes, luego en minúsculas
    private static Expression<String> comparable(CriteriaBuilder cb, Expression<String> text) {
        return cb.lower(cb.function(UNACCENT_FUNCTION, String.class, text));
    }
}
