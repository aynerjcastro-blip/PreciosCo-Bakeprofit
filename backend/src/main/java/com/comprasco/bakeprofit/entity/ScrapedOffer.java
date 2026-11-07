package com.comprasco.bakeprofit.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Oferta cruda escrita por el scraper (Python) en la tabla de staging.
 *
 * <p>
 * Java no crea ofertas: solo las lee y cambia su estado. Los datos llegan sin
 * validar, por eso {@code ean} y {@code unitBase} son texto libre y no tienen
 * CHECK: decidir si sirven es trabajo del procesador. El estado solo cambia con
 * {@link #markProcessed()} y {@link #markUnclassified(String)}, y únicamente
 * desde PENDING: procesar dos veces la misma oferta insertaría un precio repetido
 * en {@code prices}, que es histórico.
 * </p>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "scraped_offers",
    indexes = @Index(name = "idx_scraped_offers_status", columnList = "status"))
public class ScrapedOffer {

    private static final String NOT_PENDING_MESSAGE =
            "La oferta %d ya fue procesada (estado %s); solo se procesan ofertas PENDING.";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_scraped_offers_stores"))
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scrape_target_id",
        foreignKey = @ForeignKey(name = "fk_scraped_offers_scrape_targets"))
    private ScrapeTarget scrapeTarget;

    @Column(length = 20)
    private String ean;

    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Column(length = Brand.NAME_MAX_LENGTH)
    private String brandName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(precision = 12, scale = 2)
    private BigDecimal listPrice;

    @Column(columnDefinition = "text")
    private String productUrl;

    @Column(columnDefinition = "text")
    private String imageUrl;

    @Column(precision = 10, scale = 2)
    private BigDecimal netContent;

    // Texto crudo, no UnitBase: un valor desconocido no debe impedir leer la fila
    @Column(length = UnitBase.COLUMN_LENGTH)
    private String unitBase;

    @Column(nullable = false, updatable = false)
    private LocalDateTime scrapedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = OfferStatus.COLUMN_LENGTH)
    private OfferStatus status;

    @Column(columnDefinition = "text")
    private String reason;

    @Column
    private LocalDateTime processedAt;

    public boolean isPending() {
        return status == OfferStatus.PENDING;
    }

    /** La oferta se convirtió en precio del catálogo. */
    public void markProcessed() {
        requirePending();
        this.status = OfferStatus.PROCESSED;
        this.reason = null;
        this.processedAt = LocalDateTime.now();
    }

    /** La oferta no pasó los filtros; {@code reason} queda para revisarla después. */
    public void markUnclassified(String reason) {
        requirePending();
        this.status = OfferStatus.UNCLASSIFIED;
        this.reason = Objects.requireNonNull(reason);
        this.processedAt = LocalDateTime.now();
    }

    private void requirePending() {
        if (!isPending()) {
            throw new IllegalStateException(NOT_PENDING_MESSAGE.formatted(id, status));
        }
    }
}