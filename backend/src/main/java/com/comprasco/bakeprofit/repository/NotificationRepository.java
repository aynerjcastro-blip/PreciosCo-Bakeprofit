package com.comprasco.bakeprofit.repository;


import com.comprasco.bakeprofit.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comprasco.bakeprofit.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByUserIdAndPriceId(Long userId, Long priceId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndReadAtIsNull(Long userId);

    /** Los 50 avisos más recientes del usuario (el contador de no leídos va aparte). */
    List<Notification> findTop50ByUserIdOrderByCreatedAtDescIdDesc(Long userId);

}