package com.comprasco.bakeprofit.subscription.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.hibernate.annotations.Generated;

import com.comprasco.bakeprofit.auth.entity.User;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_payment_transactions_users"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id",
        foreignKey = @ForeignKey(name = "fk_payment_transactions_subscriptions"))
    private Subscription subscription;

    /** Corresponde a data.id de Mercado Pago y puede ser nulo al iniciar el pago. */
    @Column(name = "external_payment_id", unique = true, length = 50)
    private String externalPaymentId;

    /** Referencia generada por la aplicación al iniciar el pago. */
    @Column(name = "external_reference", unique = true, nullable = false, length = 64)
    private String externalReference;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "status_detail", length = 100)
    private String statusDetail;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Generated
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Generated
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
