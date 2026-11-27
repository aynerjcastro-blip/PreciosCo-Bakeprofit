package com.comprasco.bakeprofit.subscription.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.comprasco.bakeprofit.auth.entity.Plan;
import com.comprasco.bakeprofit.subscription.entity.Subscription;
import com.comprasco.bakeprofit.subscription.entity.SubscriptionStatus;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    Optional<Subscription> findFirstByUserIdAndPlanAndStatusOrderByIdAsc(Long userId, Plan plan, SubscriptionStatus status);
}
