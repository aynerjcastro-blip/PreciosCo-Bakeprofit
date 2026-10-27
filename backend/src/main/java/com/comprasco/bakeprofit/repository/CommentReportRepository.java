package com.comprasco.bakeprofit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comprasco.bakeprofit.entity.CommentReport;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
}
