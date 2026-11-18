package com.comprasco.bakeprofit.catalog.exception;

import com.comprasco.bakeprofit.catalog.entity.Category;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Solo lecturas agregadas (conteos). Extiende Repository, no JpaRepository,
 * para no exponer
 * métodos de escritura que aquí no se necesitan.
 * SUPUESTOS A AJUSTAR: tablas categories, stores (category_id, active), prices
 * (store_id,
 * product_id, last_seen_at), products (active).
 */
public interface LandingStatsRepository extends Repository<Category, Long> {

    /**
     * Fila de resultado: Spring la llena por el nombre del alias (getStoreCount <-
     * "storeCount").
     */
    interface SectionRow {
        Long getId();

        String getName();

        Long getProductCount();

        Long getStoreCount();
    }

    // Los alias van entre comillas: sin ellas PostgreSQL los pasa a minúsculas
    // ("storecount").
    @Query(nativeQuery = true, value = """
            SELECT r.id AS "id",
                   r.name AS "name",
                   (SELECT count(*) FROM stores s
                     WHERE s.category_id = r.id AND s.active) AS "storeCount",
                   (SELECT count(DISTINCT pr.product_id)
                      FROM prices pr
                      JOIN stores s ON s.id = pr.store_id AND s.active AND s.category_id = r.id
                      JOIN products p ON p.id = pr.product_id AND p.active) AS "productCount"
            FROM categories r
            WHERE r.parent_id IS NULL
            ORDER BY r.name
            """)
    List<SectionRow> findSectionStats();

    // DISTINCT: un producto con precios en dos tiendas se cuenta una sola vez.
    @Query(nativeQuery = true, value = """
            SELECT count(DISTINCT pr.product_id)
            FROM prices pr
            JOIN stores s ON s.id = pr.store_id AND s.active
            JOIN products p ON p.id = pr.product_id AND p.active
            """)
    long countProductsWithPrice();

    @Query(nativeQuery = true, value = "SELECT count(*) FROM stores WHERE active")
    long countActiveStores();

    @Query(nativeQuery = true, value = """
            SELECT max(pr.last_seen_at)
            FROM prices pr JOIN stores s ON s.id = pr.store_id AND s.active
            """)
    Optional<LocalDateTime> findLastUpdate(); // si Hibernate se queja del tipo, devolver java.sql.Timestamp y usar
                              // toInstant()
}