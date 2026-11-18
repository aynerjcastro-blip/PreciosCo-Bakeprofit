package com.comprasco.bakeprofit.staging.repository;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comprasco.bakeprofit.staging.entity.OfferStatus;
import com.comprasco.bakeprofit.staging.entity.ScrapedOffer;

import jakarta.persistence.LockModeType;

public interface ScrapedOfferRepository extends JpaRepository<ScrapedOffer, Long> {

    List<ScrapedOffer> findByStatus(OfferStatus status);

    /** Primera página de claves con ese estado, en orden de llegada. */
    @Query("""
                SELECT new com.comprasco.bakeprofit.staging.repository.PendingOfferKey(o.id, o.scrapedAt)
                FROM ScrapedOffer o
                WHERE o.status = :status
                  AND (o.runId IS NULL OR EXISTS (SELECT r.id FROM ScrapeRun r WHERE r.id = o.runId AND r.completedAt IS NOT NULL))
                ORDER BY o.scrapedAt, o.id
            """)
    List<PendingOfferKey> findKeysByStatus(@Param("status") OfferStatus status, Pageable page);

    /**
     * Página siguiente a {@code (afterScrapedAt, afterId)}.
     *
     * <p>
     * Se pagina por clave y no por número de página: cada oferta prfindKeysByStatusAfterocesada sale de
     * PENDING, así que la página 1 de la segunda consulta ya no es la de la primera
     * y
     * se saltarían filas. Además, una oferta que no se pudo marcar no vuelve a
     * leerse
     * en la misma corrida.
     * </p>
     */
    @Query("""
                SELECT new com.comprasco.bakeprofit.staging.repository.PendingOfferKey(o.id, o.scrapedAt)
                FROM ScrapedOffer o
                WHERE o.status = :status
                  AND (o.runId IS NULL OR EXISTS (SELECT r.id FROM ScrapeRun r WHERE r.id = o.runId AND r.completedAt IS NOT NULL))
                  AND (o.scrapedAt > :afterScrapedAt
                       OR (o.scrapedAt = :afterScrapedAt AND o.id > :afterId))
                ORDER BY o.scrapedAt, o.id
            """)
    List<PendingOfferKey> findKeysByStatusAfter(@Param("status") OfferStatus status,
            @Param("afterScrapedAt") LocalDateTime afterScrapedAt,
            @Param("afterId") Long afterId,
            Pageable page);

    /**
     * Carga la oferta con SELECT ... FOR UPDATE. Si dos instancias del backend
     * procesan
     * la misma oferta, la segunda espera, la encuentra ya marcada y no la repite.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM ScrapedOffer o WHERE o.id = :id")
    Optional<ScrapedOffer> lockById(@Param("id") Long id);

    // Un bloqueo transaccional antes de bloquear ofertas evita interbloqueos entre
    // dos workers que empiecen por miembros distintos de un EAN/corrida.
    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(724193810)", nativeQuery = true)
    Integer lockIngestion();

    @Query(value = "SELECT EXISTS (SELECT 1 FROM scrape_runs WHERE id = :runId AND completed_at IS NOT NULL)", nativeQuery = true)
    boolean isRunComplete(@Param("runId") UUID runId);

    @Query(value = """
            SELECT o.* FROM scraped_offers o
            WHERE o.status IN ('PENDING', 'UNCLASSIFIED') AND o.id <> :offerId
              AND o.has_ean IS DISTINCT FROM false
              AND o.ean ~ '^([0-9]{8}|[0-9]{12,14})$'
              AND lpad(o.ean, 14, '0') = :gtin
              AND (o.run_id IS NULL OR EXISTS (SELECT 1 FROM scrape_runs r WHERE r.id = o.run_id AND r.completed_at IS NOT NULL))
            ORDER BY o.id FOR UPDATE OF o
            """, nativeQuery = true)
    List<ScrapedOffer> findOtherSizeEvidenceOffers(@Param("gtin") String gtin, @Param("offerId") Long offerId);

    @Query(value = """
            SELECT o.* FROM scraped_offers o
            WHERE o.run_id = :runId AND o.store_id = :storeId AND o.status = 'PENDING'
              AND ((CAST(:gtin AS text) IS NOT NULL
                    AND o.has_ean IS DISTINCT FROM false
                    AND o.ean ~ '^([0-9]{8}|[0-9]{12,14})$' AND lpad(o.ean, 14, '0') = :gtin)
                   OR (CAST(:gtin AS text) IS NOT NULL AND (o.has_ean = false OR btrim(coalesce(o.ean, '')) = '' OR o.ean ~ '^[0-9]{1,7}$'))
                   OR (CAST(:productId AS bigint) IS NOT NULL AND EXISTS (
                       SELECT 1 FROM store_product_links l WHERE l.store_id = o.store_id
                         AND l.store_sku = o.store_sku AND l.product_id = :productId)))
            ORDER BY o.id FOR UPDATE OF o
            """, nativeQuery = true)
    List<ScrapedOffer> lockRunOffers(@Param("runId") UUID runId, @Param("storeId") Long storeId,
                                    @Param("gtin") String gtin, @Param("productId") Long productId);

    /**
     * Borra las ofertas de ese estado marcadas antes del corte. PENDING no tiene
     * processed_at.
     */
    @Modifying
    @Query("DELETE FROM ScrapedOffer o WHERE o.status = :status AND o.processedAt < :cutoff")
    int deleteByStatusMarkedBefore(@Param("status") OfferStatus status,
            @Param("cutoff") LocalDateTime cutoff);
}
