package com.comprasco.bakeprofit.repository;


import com.comprasco.bakeprofit.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByRatingId(Long ratingId);

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.comprasco.bakeprofit.entity.Comment;
import com.comprasco.bakeprofit.entity.CommentStatus;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Optional<Comment> findByRatingId(Long ratingId);

    // Reportar: solo comentarios visibles
    Optional<Comment> findByRatingIdAndStatusIn(Long ratingId, Collection<CommentStatus> statuses);

    // Listados: el EntityGraph trae calificación, usuario, producto y tienda en una sola consulta (evita N+1)
    @EntityGraph(attributePaths = {"rating", "rating.user", "rating.product", "rating.store"})
    Page<Comment> findByRatingProductIdAndStatusIn(Long productId, 
                                                   Collection<CommentStatus> statuses, 
                                                   Pageable pageable);

    @EntityGraph(attributePaths = {"rating", "rating.user", "rating.product", "rating.store"})
    Page<Comment> findByRatingStoreIdAndStatusIn(Long storeId, 
                                                 Collection<CommentStatus> statuses, 
                                                 Pageable pageable);

    @EntityGraph(attributePaths = {"rating", "rating.user", "rating.product", "rating.store"})
    Page<Comment> findByRatingProductIdAndRatingStoreIdAndStatusIn(Long productId, Long storeId,
                                                                   Collection<CommentStatus> statuses, 
                                                                   Pageable pageable);

}