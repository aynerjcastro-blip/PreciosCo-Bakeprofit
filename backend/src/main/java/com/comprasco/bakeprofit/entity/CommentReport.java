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
@Table(name = "comment_reports",
    uniqueConstraints = @UniqueConstraint(
        name = "comment_reports_comment_id_user_id_key",
        columnNames = {"comment_id", "user_id"}))
public class CommentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_comment_reports_comments"))
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_comment_reports_users"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reason;

    @Column(length = 200)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    public static CommentReport of(Comment comment, User user, ReportReason reason, String details) {
        CommentReport r = new CommentReport();
        r.comment = Objects.requireNonNull(comment);
        r.user = Objects.requireNonNull(user);
        r.reason = Objects.requireNonNull(reason);
        r.details = details;
        r.status = ReportStatus.PENDIENTE;
        r.registrationDate = LocalDateTime.now();
        return r;
    }
}
