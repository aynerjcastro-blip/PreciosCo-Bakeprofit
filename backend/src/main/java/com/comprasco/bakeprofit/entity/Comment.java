package com.comprasco.bakeprofit.entity;

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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "comments",
    indexes = {
        @Index(name = "idx_comments_product_id_store_id", columnList = "product_id, store_id"),
        @Index(name = "idx_comments_store_id", columnList = "store_id")
    })
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_comments_users"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_comments_products"))
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_comments_stores"))
    private Store store;

    @Column(nullable = false, length = 500)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommentStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    public static Comment of(User user, Product product, Store store, String text) {
        Comment c = new Comment();
        c.user = Objects.requireNonNull(user);
        c.product = Objects.requireNonNull(product);
        c.store = Objects.requireNonNull(store);
        c.text = Objects.requireNonNull(text);
        c.status = CommentStatus.PUBLICADO;
        c.registrationDate = LocalDateTime.now();
        return c;
    }

    /** Un reporte nuevo reabre un comentario conservado; uno oculto no cambia. */
    public void markAsReported() {
        if (status == CommentStatus.PUBLICADO || status == CommentStatus.CONSERVADO) {
            status = CommentStatus.REPORTADO;
        }
    }
}
