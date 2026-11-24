package com.comprasco.bakeprofit.subscription.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comprasco.bakeprofit.subscription.entity.PaymentTransaction;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /** Busca un pago por el ID del webhook. Un ID nulo no identifica un pago. */
    @Query("SELECT p FROM PaymentTransaction p WHERE p.externalPaymentId = :externalPaymentId")
    Optional<PaymentTransaction> findByExternalPaymentId(@Param("externalPaymentId") String externalPaymentId);

    Optional<PaymentTransaction> findByExternalReference(String externalReference);
}
