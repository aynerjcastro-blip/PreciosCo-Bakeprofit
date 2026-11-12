package com.comprasco.bakeprofit.price.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comprasco.bakeprofit.price.entity.Price;

public interface PriceRepository extends JpaRepository<Price, Long> {

    /**
     * Precio vigente del producto en cada tienda activa, de menor a mayor (comparador, RF07).
     * Una tienda inactiva no se compara: el visitante no la ve en la búsqueda ni en el selector.
     */
    @Query("""
        SELECT p FROM Price p
        WHERE p.product.id = :productId
        AND p.store.active = true
        AND p.registrationDate = (
            SELECT MAX(p2.registrationDate)
            FROM Price p2
            WHERE p2.product.id = p.product.id
            AND p2.store.id = p.store.id
        )
        ORDER BY p.value ASC
    """)
    List<Price> findRecentPricesByProduct(@Param("productId") Long productId);

    /** El precio más bajo entre las tiendas activas: el primero del comparador (Seguidos, RF27). */
    @Query("""
        SELECT MIN(p.value) FROM Price p
        WHERE p.product.id = :productId
        AND p.store.active = true
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

    /** El periodo de precio más reciente del producto en la tienda. */
    Optional<Price> findFirstByProductIdAndStoreIdOrderByRegistrationDateDescIdDesc(Long productId, Long storeId);

    /** Tiendas que tienen al menos un precio del producto. */
    @Query("SELECT DISTINCT p.store.id FROM Price p WHERE p.product.id = :productId")
    List<Long> findStoreIdsByProductId(@Param("productId") Long productId);
}