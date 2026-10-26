package com.comprasco.bakeprofit.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Entity
@Table(
    name = "comment_reports",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "comment_reports_user_id_comment_id_key",
            columnNames = {"user_id", "comment_id"}
        )
    }
)
public class CommentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReportReason reason;

    @Size(max = 200, message = "El detalle no puede superar los 200 caracteres")
    @Column(length = 200)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDIENTE;

    public enum ReportReason {
        SPAM, INAPROPIADO, OFENSIVO, OTRO
    }

    public enum ReportStatus {
        PENDIENTE, RESUELTO
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Comment getComment() { return comment; }
    public void setComment(Comment comment) { this.comment = comment; }
    public ReportReason getReason() { return reason; }
    public void setReason(ReportReason reason) { this.reason = reason; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
}