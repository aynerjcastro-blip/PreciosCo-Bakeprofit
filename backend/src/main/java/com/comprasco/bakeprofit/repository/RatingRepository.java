package com.comprasco.bakeprofit.repository;

import com.comprasco.bakeprofit.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    boolean existsByUserIdAndProductIdAndStoreId(Long userId, Long productId, Long storeId);

    Optional<Rating> findByUserIdAndProductIdAndStoreId(Long userId, Long productId, Long storeId);

    // Consulta con regla de visibilidad: Solo PUBLICADO o CONSERVADO
    @Query("SELECT AVG(r.score), COUNT(r) FROM Rating r JOIN Comment c ON c.rating.id = r.id " +
           "WHERE r.product.id = :productId AND r.store.id = :storeId " +
           "AND c.status IN ('PUBLICADO', 'CONSERVADO')")
    Object[] getSummaryByProductAndStore(@Param("productId") Long productId, @Param("storeId") Long storeId);
}