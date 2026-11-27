package com.comprasco.bakeprofit.ai.recipe;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.catalog.entity.Product;
import com.comprasco.bakeprofit.catalog.repository.CategoryRepository;
import com.comprasco.bakeprofit.catalog.repository.ProductSpecifications;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Implementación JPA de {@link CatalogProductSearch}. Reutiliza los filtros de
 * la búsqueda del catálogo ({@link ProductSpecifications}: nombre sin tildes,
 * activo, con precio en la sección), pero selecciona solo el ID para no cargar
 * entidades {@code Product} ni sus relaciones EAGER.
 */
@Component
class JpaCatalogProductSearch implements CatalogProductSearch {

    private static final String SECTION_ROOT_NAME = "Supermercado";

    private final EntityManager entityManager;
    private final CategoryRepository categoryRepository;
    private final AtomicReference<Long> sectionId = new AtomicReference<>();

    JpaCatalogProductSearch(EntityManager entityManager, CategoryRepository categoryRepository) {
        this.entityManager = entityManager;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Long> searchIds(List<String> words, int limit) {
        Long section = supermarketSectionId();
        if (section == null || words.isEmpty()) {
            return List.of();
        }

        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.isActive(),
                ProductSpecifications.nameContainsAll(words),
                ProductSpecifications.inSection(section));

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Product> product = query.from(Product.class);
        query.select(product.<Long>get("id"))
                .where(spec.toPredicate(product, query, cb))
                .orderBy(cb.asc(cb.length(product.<String>get("name"))), cb.asc(product.get("id")));

        return entityManager.createQuery(query).setMaxResults(limit).getResultList();
    }

    /** ID de la categoría raíz de supermercados (no cambia: se guarda tras la primera consulta). */
    private Long supermarketSectionId() {
        Long id = sectionId.get();
        if (id == null) {
            id = categoryRepository.findRootIdByName(SECTION_ROOT_NAME).orElse(null);
            sectionId.set(id);
        }
        return id;
    }
}
