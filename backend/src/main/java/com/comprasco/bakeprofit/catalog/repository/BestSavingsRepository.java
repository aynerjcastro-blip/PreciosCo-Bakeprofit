package com.comprasco.bakeprofit.catalog.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.comprasco.bakeprofit.catalog.entity.Product;

public interface BestSavingsRepository extends Repository<Product, Long> {

    interface Row {
        Long getId(); String getName();
        Long getCategoryId(); String getCategoryName();
        Long getSectionId(); String getSectionName();
        BigDecimal getSavingsPct(); BigDecimal getMinPrice(); BigDecimal getMaxPrice();
        Integer getStoreCount();
    }

    @Query(value = """
        SELECT * FROM (
          SELECT p.id AS id, p.name AS name,
                 c.id AS categoryId, c.name AS categoryName,
                 r.id AS sectionId, r.name AS sectionName,
                 s.savings_pct AS savingsPct, s.min_price AS minPrice,
                 s.max_price AS maxPrice, s.store_count AS storeCount,
                 ROW_NUMBER() OVER (PARTITION BY c.id
                                    ORDER BY s.savings_pct DESC, p.name, p.id) AS rn
          FROM products p
          JOIN categories c ON c.id = p.category_id
          JOIN categories r ON r.id = c.parent_id
          JOIN product_savings s ON s.product_id = p.id
          WHERE p.active AND s.store_count > 1
            AND (CAST(:sectionId AS bigint) IS NULL OR r.id = :sectionId)
        ) t WHERE t.rn <= :limit
        ORDER BY t.sectionName, t.categoryName, t.rn
        """, nativeQuery = true)
    List<Row> findTopBySubcategory(@Param("sectionId") Long sectionId, @Param("limit") int limit);
}