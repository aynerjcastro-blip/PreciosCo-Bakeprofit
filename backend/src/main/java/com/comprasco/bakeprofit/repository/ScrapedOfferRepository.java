package com.comprasco.bakeprofit.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comprasco.bakeprofit.entity.OfferStatus;
import com.comprasco.bakeprofit.entity.ScrapedOffer;

import jakarta.persistence.LockModeType;

public interface ScrapedOfferRepository extends JpaRepository<ScrapedOffer, Long> {

    List<ScrapedOffer> findByStatus(OfferStatus status);

    /** Primera página de claves con ese estado, en orden de llegada. */
    @Query("""
        SELECT new com.comprasco.bakeprofit.repository.PendingOfferKey(o.id, o.scrapedAt)
        FROM ScrapedOffer o
        WHERE o.status = :status
        ORDER BY o.scrapedAt, o.id
    """)
    List<PendingOfferKey> findKeysByStatus(@Param("status") OfferStatus status, Pageable page);

    /**
     * Página siguiente a {@code (afterScrapedAt, afterId)}.
     *
     * <p>
     * Se pagina por clave y no por número de página: cada oferta procesada sale de
     * PENDING, así que la página 1 de la segunda consulta ya no es la de la primera y
     * se saltarían filas. Además, una oferta que no se pudo marcar no vuelve a leerse
     * en la misma corrida.
     * </p>
     */
    @Query("""
        SELECT new com.comprasco.bakeprofit.repository.PendingOfferKey(o.id, o.scrapedAt)
        FROM ScrapedOffer o
        WHERE o.status = :status
          AND (o.scrapedAt > :afterScrapedAt
               OR (o.scrapedAt = :afterScrapedAt AND o.id > :afterId))
        ORDER BY o.scrapedAt, o.id
    """)
    List<PendingOfferKey> findKeysByStatusAfter(@Param("status") OfferStatus status,
                                                @Param("afterScrapedAt") LocalDateTime afterScrapedAt,
                                                @Param("afterId") Long afterId,
                                                Pageable page);

    /**
     * Carga la oferta con SELECT ... FOR UPDATE. Si dos instancias del backend procesan
     * la misma oferta, la segunda espera, la encuentra ya marcada y no la repite.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM ScrapedOffer o WHERE o.id = :id")
    Optional<ScrapedOffer> lockById(@Param("id") Long id);

    /** Borra las ofertas de ese estado marcadas antes del corte. PENDING no tiene processed_at. */
    @Modifying
    @Query("DELETE FROM ScrapedOffer o WHERE o.status = :status AND o.processedAt < :cutoff")
    int deleteByStatusMarkedBefore(@Param("status") OfferStatus status,
                                   @Param("cutoff") LocalDateTime cutoff);
}