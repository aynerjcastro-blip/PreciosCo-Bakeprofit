package com.comprasco.bakeprofit.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comprasco.bakeprofit.entity.Rating;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    // Pre-comprobación de duplicado (la única en BD es la red de seguridad)
    boolean existsByUserIdAndProductIdAndStoreId(Long userId, Long productId, Long storeId);

    // Para que el frontend sepa si el usuario ya calificó ese producto en esa tienda
    Optional<Rating> findByUserIdAndProductIdAndStoreId(Long userId, Long productId, Long storeId);

    @Query("""
        SELECT AVG(r.score) AS average, COUNT(r) AS total
        FROM Rating r
        WHERE r.product.id = :productId
    """)
    RatingStats statsByProduct(@Param("productId") Long productId);

    @Query("""
        SELECT AVG(r.score) AS average, COUNT(r) AS total
        FROM Rating r
        WHERE r.product.id = :productId
        AND r.store.id = :storeId
    """)
    RatingStats statsByProductAndStore(@Param("productId") Long productId,
                                       @Param("storeId") Long storeId);

    @Query("""
        SELECT AVG(r.score) AS average, COUNT(r) AS total
        FROM Rating r
        WHERE r.store.id = :storeId
    """)
    RatingStats statsByStore(@Param("storeId") Long storeId);
}
