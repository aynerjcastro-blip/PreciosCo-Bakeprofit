package com.comprasco.bakeprofit.subscription.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comprasco.bakeprofit.subscription.entity.Subscription;
import com.comprasco.bakeprofit.subscription.entity.SubscriptionStatus;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);
}
