package com.comprasco.bakeprofit.repository;
import com.comprasco.bakeprofit.entity.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface PriceRepository extends JpaRepository<Price, Long> {

    
    @Query("""
        SELECT p FROM Price p
        WHERE p.product.id = :productId
        AND p.registrationDate = (
            SELECT MAX(p2.registrationDate)
            FROM Price p2
            WHERE p2.product.id = p.product.id
            AND p2.store.id = p.store.id
        )
        ORDER BY p.value ASC
    """)
    List<Price> findRecentPricesByProduct(@Param("productId") Long productId);

   
    @Query("""
        SELECT MIN(p.value) FROM Price p
        WHERE p.product.id = :productId
        AND p.registrationDate = (
            SELECT MAX(p2.registrationDate)
            FROM Price p2
            WHERE p2.product.id = p.product.id
            AND p2.store.id = p.store.id
        )
    """)
    java.math.BigDecimal findMinPriceByProduct(@Param("productId") Long productId);

    boolean existsByProductIdAndStoreId(Long productId, Long storeId);

        /** Los dos precios más recientes de un producto en una tienda (el último y el anterior). */
    List<Price> findTop2ByProductIdAndStoreIdOrderByRegistrationDateDescIdDesc(Long productId, Long storeId);

    /** Tiendas que tienen al menos un precio del producto. */
    @Query("SELECT DISTINCT p.store.id FROM Price p WHERE p.product.id = :productId")
    List<Long> findStoreIdsByProductId(@Param("productId") Long productId);
}