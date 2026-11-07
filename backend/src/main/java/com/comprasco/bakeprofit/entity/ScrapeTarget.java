package com.comprasco.bakeprofit.entity;

import java.math.BigDecimal;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Qué busca el scraper en una tienda y qué acepta como resultado válido
 * (reemplaza el diccionario PRODUCTOS del scraper).
 *
 * <p>
 * Un resultado entra al catálogo solo si trae la unidad esperada y su contenido
 * está dentro del rango; un límite nulo deja ese lado abierto. El CHECK de la
 * unidad y el del rango viven en el script.
 * </p>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "scrape_targets")
public class ScrapeTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_scrape_targets_stores"))
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_scrape_targets_categories"))
    private Category category;

    @Column(nullable = false, length = 100)
    private String searchTerm;

    // Filtro de categoría propio de la tienda (parámetro fq de VTEX)
    @Column(length = 200)
    private String storeCategoryFq;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = UnitBase.COLUMN_LENGTH)
    private UnitBase unitBase;

    @Column(precision = 10, scale = 2)
    private BigDecimal minContent;

    @Column(precision = 10, scale = 2)
    private BigDecimal maxContent;

    @Column(nullable = false)
    private Boolean active;

    public static ScrapeTarget of(Store store, Category category, String searchTerm,
                                  UnitBase unitBase, BigDecimal minContent, BigDecimal maxContent) {
        ScrapeTarget target = new ScrapeTarget();
        target.store = Objects.requireNonNull(store);
        target.category = Objects.requireNonNull(category);
        target.searchTerm = Objects.requireNonNull(searchTerm);
        target.unitBase = Objects.requireNonNull(unitBase);
        target.minContent = minContent;
        target.maxContent = maxContent;
        target.active = true;
        return target;
    }

    /** Indica si un resultado con ese contenido y esa unidad cumple este objetivo. */
    public boolean accepts(BigDecimal content, UnitBase unit) {
        return content != null
                && unitBase == unit
                && isWithinRange(content);
    }

    private boolean isWithinRange(BigDecimal content) {
        boolean reachesMinimum = minContent == null || content.compareTo(minContent) >= 0;
        boolean withinMaximum = maxContent == null || content.compareTo(maxContent) <= 0;
        return reachesMinimum && withinMaximum;
    }
}