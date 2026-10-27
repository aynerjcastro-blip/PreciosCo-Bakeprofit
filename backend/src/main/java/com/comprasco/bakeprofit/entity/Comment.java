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
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rating_id", nullable = false)
    private Rating rating;

    @Size(min = 10, max = 500, message = "El comentario debe tener entre 10 y 500 caracteres")
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
}