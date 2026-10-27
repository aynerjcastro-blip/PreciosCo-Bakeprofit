package com.comprasco.bakeprofit.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.comprasco.bakeprofit.entity.Follow;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Optional<Follow> findByIdAndUserId(Long id, Long userId);

    Optional<Follow> findByUserIdAndProductId(Long userId, Long productId);

    List<Follow> findByUserIdOrderByRegistrationDateDesc(Long userId);

    /** Ids de los seguimientos con el aviso activo: lo que revisa el detector de bajas. */
    @Query("SELECT f.id FROM Follow f WHERE f.notificationActive = true")
    List<Long> findActiveIds();
}