package com.comprasco.bakeprofit.subscription.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.subscription.entity.PaymentTransaction;

import jakarta.persistence.LockModeType;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /** Busca un pago por el ID del webhook. Un ID nulo no identifica un pago. */
    @Query("SELECT p FROM PaymentTransaction p WHERE p.externalOrderId = :externalOrderId")
    Optional<PaymentTransaction> findByExternalOrderId(@Param("externalOrderId") String externalOrderId);

    boolean existsByExternalOrderIdAndStatusIn(String externalOrderId, Collection<String> statuses);

    Optional<PaymentTransaction> findByExternalReference(String externalReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentTransaction p WHERE p.externalReference = :externalReference")
    Optional<PaymentTransaction> findByExternalReferenceForUpdate(@Param("externalReference") String externalReference);

    @Transactional
    @Modifying
    @Query("""
        UPDATE PaymentTransaction p SET p.externalOrderId = :orderId
        WHERE p.id = :paymentId AND (p.externalOrderId IS NULL OR p.externalOrderId = :orderId)
        """)
    int linkExternalOrder(@Param("paymentId") Long paymentId, @Param("orderId") String orderId);
}
