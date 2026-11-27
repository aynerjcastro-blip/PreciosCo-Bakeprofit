package com.comprasco.bakeprofit.catalog.repository;

import com.comprasco.bakeprofit.catalog.entity.Brand;
import com.comprasco.bakeprofit.catalog.entity.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Limit;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// JpaSpecificationExecutor: la búsqueda combina sus filtros con ProductSpecifications (HU-06a, HU-06b)
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    List<Product> findByActiveTrue ();

    List<Product> findByActiveFalse ();

    Optional<Product> findByEan(String ean);

    // Comparan contra la columna generada: el valor se normaliza en Java con ManufacturerReference
    boolean existsByBrandAndManufacturerRefNorm(Brand brand, String manufacturerRefNorm);

    boolean existsByBrandAndManufacturerRefNormAndIdNot(Brand brand, String manufacturerRefNorm, Long id);

    /**
     * IDs de productos activos de las categorías que tienen al menos un precio
     * en una tienda activa (contexto del asistente de IA). Solo IDs: no carga
     * entidades ni dispara las relaciones EAGER (marca, categoría).
     */
    @Query("""
            SELECT p.id FROM Product p
            WHERE p.category.id IN :categoryIds
              AND p.active = true
              AND EXISTS (SELECT 1 FROM Price pr WHERE pr.product = p AND pr.store.active = true)
            ORDER BY p.id
            """)
    List<Long> findPricedActiveIdsByCategoryIds(@Param("categoryIds") Collection<Long> categoryIds, Limit limit);

    /** Marca o desmarca un producto como de venta bajo fórmula médica (HU-39, RN-33). */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.requiresPrescription = :required WHERE p.id = :id")
    int updateRequiresPrescription(@Param("id") Long id, @Param("required") boolean required);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.active = :active WHERE p.category.id = :categoryId")
    int updateActiveByCategoryId(@Param("categoryId") Long categoryId, @Param("active") boolean active);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.active = :active WHERE p.category.parent.id = :parentId")
    int updateActiveByCategoryParentId(@Param("parentId") Long parentId, @Param("active") boolean active);
}