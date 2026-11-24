package com.comprasco.bakeprofit.subscription.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.subscription.entity.PaymentTransaction;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /** Busca un pago por el ID del webhook. Un ID nulo no identifica un pago. */
    @Query("SELECT p FROM PaymentTransaction p WHERE p.externalOrderId = :externalOrderId")
    Optional<PaymentTransaction> findByExternalOrderId(@Param("externalOrderId") String externalOrderId);

    Optional<PaymentTransaction> findByExternalReference(String externalReference);

    @Transactional
    @Modifying
    @Query("""
        UPDATE PaymentTransaction p SET p.externalOrderId = :orderId
        WHERE p.id = :paymentId AND p.externalOrderId IS NULL
        """)
    int linkExternalOrder(@Param("paymentId") Long paymentId, @Param("orderId") String orderId);
}
