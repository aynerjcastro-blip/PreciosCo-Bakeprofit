package com.comprasco.bakeprofit.entity;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Seguimiento de un producto por un usuario (RF23, RF24, RF25).
 *
 * <p>
 * Un usuario sigue un producto una sola vez. {@code targetPrice} es opcional: sin
 * precio objetivo, la alerta salta ante cualquier reducción (RN-16).
 * {@code alertActiveSince} marca desde cuándo cuenta la alerta: el detector solo
 * considera precios registrados después de esa fecha, así seguir un producto o
 * reactivar el aviso no genera avisos de bajas anteriores.
 * </p>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "follows",
    uniqueConstraints = @UniqueConstraint(
        name = "follows_user_id_product_id_key",
        columnNames = { "user_id", "product_id" }))
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_follows_users"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_follows_products"))
    private Product product;

    @Column(nullable = false)
    private Boolean notificationActive;

    // null = avisar ante cualquier reducción
    @Column(precision = 12, scale = 2)
    private BigDecimal targetPrice;

    @Column(nullable = false)
    private LocalDateTime alertActiveSince;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    public static Follow of(User user, Product product, BigDecimal targetPrice) {
        Follow f = new Follow();
        f.user = Objects.requireNonNull(user);
        f.product = Objects.requireNonNull(product);
        f.targetPrice = targetPrice;
        f.notificationActive = true;
        f.registrationDate = LocalDateTime.now();
        f.alertActiveSince = f.registrationDate;
        return f;
    }

    /**
     * Cambia el precio objetivo y/o activa o desactiva el aviso.
     * Al reactivar el aviso se reinicia {@code alertActiveSince}: las bajas ocurridas
     * mientras estuvo desactivado no generan avisos.
     */
    public void updateAlert(boolean active, BigDecimal targetPrice) {
        if (active && !Boolean.TRUE.equals(this.notificationActive)) {
            this.alertActiveSince = LocalDateTime.now();
        }
        this.notificationActive = active;
        this.targetPrice = targetPrice;
    }
}