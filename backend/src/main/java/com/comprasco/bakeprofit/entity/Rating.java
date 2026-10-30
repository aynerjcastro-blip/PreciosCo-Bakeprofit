package com.comprasco.bakeprofit.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.CheckConstraint;
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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ratings",
    uniqueConstraints = @UniqueConstraint(
        name = "ratings_product_id_store_id_user_id_key",
        columnNames = {"product_id", "store_id", "user_id"}),
    indexes = @Index(name = "idx_ratings_store_id", columnList = "store_id"))
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_ratings_users"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_ratings_products"))
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_ratings_stores"))
    private Store store;

    @Column(nullable = false,
        check = @CheckConstraint(
            name = "chk_ratings_score",
            constraint = "score >= 1 AND score <= 5"))
    private Integer score;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    public static Rating of(User user, Product product, Store store, int score) {
        Rating r = new Rating();
        r.user = Objects.requireNonNull(user);
        r.product = Objects.requireNonNull(product);
        r.store = Objects.requireNonNull(store);
        r.score = score;
        r.registrationDate = LocalDateTime.now();
        return r;
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    public void updateScore(int score) {
        this.score = score;
    }
}