package com.comprasco.bakeprofit.catalog.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import org.hibernate.annotations.Immutable;

/**
 * Vista product_savings (solo lectura, scripts/2026-11-ht13-ahorro.sql). Ahorro = (máx - mín) / máx
 * de los precios vigentes en tiendas activas; 0 con un solo precio o sin precio (DP-39).
 */
@Entity
@Immutable
@Table(name = "product_savings")
public class ProductSavings {
    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "store_count")
    private int storeCount;

    @Column(name = "min_price")
    private BigDecimal minPrice;

    @Column(name = "max_price")
    private BigDecimal maxPrice;

    @Column(name = "savings_pct")
    private BigDecimal savingsPct;

    public Long getProductId() { return productId; }
    public int getStoreCount() { return storeCount; }
    public BigDecimal getMinPrice() { return minPrice; }
    public BigDecimal getMaxPrice() { return maxPrice; }
    public BigDecimal getSavingsPct() { return savingsPct; }
}
