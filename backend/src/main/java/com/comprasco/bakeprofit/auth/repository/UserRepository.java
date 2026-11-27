package com.comprasco.bakeprofit.auth.repository;

import java.time.OffsetDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.auth.entity.Plan;
import com.comprasco.bakeprofit.auth.entity.Role;
import com.comprasco.bakeprofit.auth.entity.User;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;


public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndActiveTrue(String email);

    List<User> findByActiveTrue();

    List<User> findByActiveFalse();

    List<User> findByRole(Role role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        UPDATE users
        SET ai_queries_used = 0,
            ai_queries_reset_at = ai_queries_reset_at + INTERVAL '1 month'
        WHERE ai_queries_reset_at <= :now
        """, nativeQuery = true)
    int resetDueAiQuotas(@Param("now") OffsetDateTime now);

    @Modifying
    @Query("UPDATE User u SET u.plan = :plan WHERE u.id = :userId")
    int updatePlan(@Param("userId") Long userId, @Param("plan") Plan plan);

}
