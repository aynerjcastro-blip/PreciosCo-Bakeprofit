package com.comprasco.bakeprofit.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.comprasco.bakeprofit.util.Gtin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import lombok.Setter;

/**
 * Producto exacto del catálogo (ADR-01).
 *
 * <p>
 * Los productos comunes se identifican por su EAN; los repuestos, por marca +
 * referencia del fabricante normalizada. Los CHECK de identidad, los índices únicos
 * parciales y la columna generada viven en el script SQL: JPA no puede expresarlos
 * y ddl-auto=validate no los revisa.
 * </p>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "products",
    indexes = @Index(name = "idx_products_category", columnList = "category_id"))
public class Product {

    public static final int MANUFACTURER_REF_MAX_LENGTH = 60;
    public static final int NET_CONTENT_INTEGER_DIGITS = 8;
    public static final int NET_CONTENT_FRACTION_DIGITS = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String unit;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private LocalDateTime registrationDate = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_products_categories"))
    private Category category;

    @ManyToOne
    @JoinColumn(name = "brand_id", foreignKey = @ForeignKey(name = "fk_products_brands"))
    private Brand brand;

    // GTIN-14: siempre 14 dígitos, rellenado con ceros a la izquierda (ver Gtin.normalize)
    @Column(length = Gtin.LENGTH)
    private String ean;

    // Contenido neto en la unidad base: 1000 con G es 1 kg
    @Column(precision = NET_CONTENT_INTEGER_DIGITS + NET_CONTENT_FRACTION_DIGITS,
            scale = NET_CONTENT_FRACTION_DIGITS)
    private BigDecimal netContent;

    @Enumerated(EnumType.STRING)
    @Column(length = UnitBase.COLUMN_LENGTH)
    private UnitBase unitBase;

    @Column(length = MANUFACTURER_REF_MAX_LENGTH)
    private String manufacturerRef;

    // La calcula PostgreSQL; INSERT y UPDATE porque cambia cada vez que cambia manufacturerRef.
    // El columnDefinition es idéntico al del script y solo documenta: ddl-auto=validate no lo usa.
    @Setter(AccessLevel.NONE)
    @Generated(event = { EventType.INSERT, EventType.UPDATE })
    @Column(insertable = false, updatable = false, length = MANUFACTURER_REF_MAX_LENGTH,
            columnDefinition = "VARCHAR(60) GENERATED ALWAYS AS "
                    + "(upper(regexp_replace(manufacturer_ref, '[^A-Za-z0-9]', '', 'g'))) STORED")
    private String manufacturerRefNorm;

    public boolean hasManufacturerReference() {
        return manufacturerRef != null;
    }

    public boolean hasNetContent() {
        return netContent != null;
    }
}