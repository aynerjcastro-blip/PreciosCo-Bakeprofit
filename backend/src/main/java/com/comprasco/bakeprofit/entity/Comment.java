package com.comprasco.bakeprofit.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "comments",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "comments_rating_id_key",
            columnNames = {"rating_id"}
        )
    }
)

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Comentario de una opinión. Pertenece a una única {@link Rating} (relación 1 a 1 obligatoria):
 * el usuario, el producto y la tienda se leen de ella, así no se duplican ni pueden contradecirse.
 * Eliminar el comentario es un borrado lógico (ELIMINADO): la fila se conserva, por eso no se puede volver a comentar.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "comments",
    uniqueConstraints = @UniqueConstraint(
        name = "comments_rating_id_key",
        columnNames = "rating_id"))

public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rating_id", nullable = false)
    private Rating rating;

    @Size(min = 10, max = 500, message = "El comentario debe tener entre 10 y 500 caracteres")

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rating_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_comments_ratings"))
    private Rating rating;

    // Se conserva aunque el comentario se elimine (borrado lógico)

    @Column(nullable = false, length = 500)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)

    private CommentStatus status = CommentStatus.PUBLICADO;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;

    public enum CommentStatus {
        PUBLICADO, REPORTADO, CONSERVADO, OCULTO, ELIMINADO
    }

    @PrePersist
    public void prePersist() {
        this.registrationDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Rating getRating() { return rating; }
    public void setRating(Rating rating) { this.rating = rating; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public CommentStatus getStatus() { return status; }
    public void setStatus(CommentStatus status) { this.status = status; }
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }

    private CommentStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    public static Comment of(Rating rating, String text) {
        Comment c = new Comment();
        c.rating = Objects.requireNonNull(rating);
        c.text = Objects.requireNonNull(text);
        c.status = CommentStatus.PUBLICADO;
        c.registrationDate = rating.getRegistrationDate();
        return c;
    }

    public void updateText(String text) {
        this.text = Objects.requireNonNull(text);
    }

    /** Borrado lógico por su autor. Un comentario OCULTO por moderación conserva ese estado. */
    public void delete() {
        if (status != CommentStatus.OCULTO) {
            status = CommentStatus.ELIMINADO;
        }
    }

    /** Un reporte nuevo reabre un comentario conservado. REPORTADO, OCULTO y ELIMINADO no cambian. */
    public void markAsReported() {
        if (status == CommentStatus.PUBLICADO || status == CommentStatus.CONSERVADO) {
            status = CommentStatus.REPORTADO;
        }
    }
}