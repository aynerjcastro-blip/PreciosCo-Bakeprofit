package com.comprasco.bakeprofit.repository;

import com.comprasco.bakeprofit.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByRatingId(Long ratingId);
}