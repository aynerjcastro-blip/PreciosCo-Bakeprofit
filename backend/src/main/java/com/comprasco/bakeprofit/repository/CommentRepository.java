package com.comprasco.bakeprofit.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.comprasco.bakeprofit.entity.Comment;
import com.comprasco.bakeprofit.entity.CommentStatus;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // El EntityGraph trae usuario, producto y tienda en la misma consulta (evita N+1 al armar el DTO)
    @EntityGraph(attributePaths = {"user", "product", "store"})
    Page<Comment> findByProductIdAndStatusNot(Long productId, CommentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "product", "store"})
    Page<Comment> findByStoreIdAndStatusNot(Long storeId, CommentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "product", "store"})
    Page<Comment> findByProductIdAndStoreIdAndStatusNot(Long productId, Long storeId,
                                                        CommentStatus status, Pageable pageable);

    // Para reportar: un comentario OCULTO se trata como inexistente
    Optional<Comment> findByIdAndStatusNot(Long id, CommentStatus status);
}
