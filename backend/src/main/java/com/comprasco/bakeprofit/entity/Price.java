package com.comprasco.bakeprofit.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor

@Table(name = "prices")
public class Price {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal value;

    @Column(nullable = false)
    private LocalDateTime registrationDate = LocalDateTime.now();

    @Column 
    private String source;

    // Enlace directo al producto en la tienda
    @Column(name = "product_url", columnDefinition = "text")
    private String productUrl;

    // Imagen del producto en la tienda
    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_prices_products"))
    private Product product;

    @ManyToOne
    @JoinColumn(name = "store_id", nullable = false, foreignKey = @ForeignKey(name = "fk_prices_stores" ))
    private Store store;

}