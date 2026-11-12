package com.comprasco.bakeprofit.catalog.repository;

import com.comprasco.bakeprofit.catalog.entity.Brand;
import com.comprasco.bakeprofit.catalog.entity.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.active = :active WHERE p.category.id = :categoryId")
    int updateActiveByCategoryId(@Param("categoryId") Long categoryId, @Param("active") boolean active);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.active = :active WHERE p.category.parent.id = :parentId")
    int updateActiveByCategoryParentId(@Param("parentId") Long parentId, @Param("active") boolean active);
}