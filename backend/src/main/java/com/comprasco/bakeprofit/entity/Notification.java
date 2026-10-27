package com.comprasco.bakeprofit.entity;


import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Aviso dentro de la plataforma por una baja de precio (RF27).
 *
 * <p>
 * Guarda el precio nuevo al que corresponde ({@code price}) y los dos valores
 * (anterior y nuevo) para mostrarlos sin depender de la tabla {@code prices}.
 * La única (usuario, precio) es la que vuelve idempotente al detector: el mismo
 * precio nunca avisa dos veces al mismo usuario. Esta tabla solo <em>lee</em>
 * {@code prices}; no la modifica.
 * </p>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notifications",
    uniqueConstraints = @UniqueConstraint(
        name = "notifications_user_id_price_id_key",
        columnNames = { "user_id", "price_id" }),
    indexes = @Index(name = "idx_notifications_user_id", columnList = "user_id"))

public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 255)
    private String message;

    @Column(nullable = false)
    private Boolean read = false;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @PrePersist
    public void prePersist() {
        this.createdDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_notifications_users"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_notifications_products"))
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_notifications_stores"))
    private Store store;

    // El precio (nuevo) que originó el aviso
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "price_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_notifications_prices"))
    private Price price;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal previousValue;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal newValue;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // null = no leído
    @Column
    private LocalDateTime readAt;

    public static Notification of(User user, Product product, Store store, Price price,
                                  BigDecimal previousValue, BigDecimal newValue) {
        Notification n = new Notification();
        n.user = Objects.requireNonNull(user);
        n.product = Objects.requireNonNull(product);
        n.store = Objects.requireNonNull(store);
        n.price = Objects.requireNonNull(price);
        n.previousValue = Objects.requireNonNull(previousValue);
        n.newValue = Objects.requireNonNull(newValue);
        n.createdAt = LocalDateTime.now();
        return n;
    }

    public boolean isRead() {
        return readAt != null;
    }

    /** Marca el aviso como leído. Repetirlo no cambia la fecha original. */
    public void markRead() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }

}