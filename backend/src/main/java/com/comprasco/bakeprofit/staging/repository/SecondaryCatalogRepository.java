package com.comprasco.bakeprofit.staging.repository;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import com.comprasco.bakeprofit.catalog.entity.Product;

/** La comparación de marca usa la misma expresión que el índice de BrandRepository. */
public interface SecondaryCatalogRepository extends Repository<Product, Long> {
    @Query(value = """
            SELECT product.* FROM products product
            JOIN brands brand ON brand.id = product.brand_id
            WHERE product.active AND product.ean IS NOT NULL
              AND product.category_id = :categoryId
              AND product.net_content = :content AND product.unit_base = :unit
              AND lower(f_unaccent(brand.name)) = lower(f_unaccent(:brandName))
            ORDER BY product.id
            """, nativeQuery = true)
    List<Product> findCandidates(@Param("categoryId") Long categoryId,
            @Param("brandName") String brandName, @Param("content") BigDecimal content,
            @Param("unit") String unit, Pageable page);
}
