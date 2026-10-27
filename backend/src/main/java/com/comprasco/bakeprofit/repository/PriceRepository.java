package com.comprasco.bakeprofit.repository;

import com.comprasco.bakeprofit.entity.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceRepository extends JpaRepository<Price, Long> {

    // Método añadido para validar si existe al menos un precio en la regla de opiniones (Opción B)
    boolean existsByProductIdAndStoreId(Long productId, Long storeId);

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
}