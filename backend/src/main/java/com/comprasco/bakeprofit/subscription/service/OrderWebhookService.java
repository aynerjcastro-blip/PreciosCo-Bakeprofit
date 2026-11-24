package com.comprasco.bakeprofit.subscription.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import com.comprasco.bakeprofit.auth.entity.Plan;
import com.comprasco.bakeprofit.auth.repository.UserRepository;
import com.comprasco.bakeprofit.subscription.entity.PaymentTransaction;
import com.comprasco.bakeprofit.subscription.entity.Subscription;
import com.comprasco.bakeprofit.subscription.entity.SubscriptionStatus;
import com.comprasco.bakeprofit.subscription.repository.PaymentTransactionRepository;
import com.comprasco.bakeprofit.subscription.repository.SubscriptionRepository;
import com.mercadopago.client.order.OrderClient;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.order.Order;
import com.mercadopago.resources.order.OrderPayment;

@Service
public class OrderWebhookService {

    private static final Logger log = LoggerFactory.getLogger(OrderWebhookService.class);
    private static final Set<String> TERMINAL_STATUSES = Set.of("canceled", "expired", "refunded");
    private static final Set<String> PRE_ACCREDITATION_STATUSES = Set.of("created", "processing", "action_required", "failed");

    private final PaymentTransactionRepository payments;
    private final SubscriptionRepository subscriptions;
    private final UserRepository users;
    private final OrderClient orders;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final String accessToken;

    public OrderWebhookService(PaymentTransactionRepository payments, SubscriptionRepository subscriptions,
                               UserRepository users, OrderClient orders, PlatformTransactionManager transactionManager,
                               Clock clock, @Value("${app.mercadopago.access-token:}") String accessToken) {
        this.payments = payments;
        this.subscriptions = subscriptions;
        this.users = users;
        this.orders = orders;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
        this.accessToken = accessToken;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void process(String orderId) throws MPApiException, MPException {
        // Esta transacción corta libera la conexión antes del GET y del bloqueo posterior.
        boolean terminal = Boolean.TRUE.equals(transaction.execute(status ->
            payments.existsByExternalOrderIdAndStatusIn(orderId, TERMINAL_STATUSES)));
        if (terminal) {
            return;
        }
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalStateException("Falta app.mercadopago.access-token para consultar la Order");
        }
        Order order = orders.get(orderId, MPRequestOptions.builder().accessToken(accessToken).build());
        if (order == null || !orderId.equals(order.getId()) || !"online".equals(order.getType())
                || !StringUtils.hasText(order.getExternalReference()) || !StringUtils.hasText(order.getStatus())) {
            log.error("Mercado Pago devolvió datos incompletos o incompatibles para la Order {}", orderId);
            return;
        }

        transaction.executeWithoutResult(result -> applyOrder(order));
    }

    private void applyOrder(Order order) {
        PaymentTransaction payment = payments.findByExternalReferenceForUpdate(order.getExternalReference()).orElse(null);
        if (payment == null) {
            log.error("No existe un intento local para la Order {}", order.getId());
            return;
        }
        if (payment.getExternalOrderId() != null && !order.getId().equals(payment.getExternalOrderId())) {
            log.error("El intento local de la Order {} está vinculado a otra orden", order.getId());
            return;
        }
        if (TERMINAL_STATUSES.contains(payment.getStatus()) || isOlderThanAccreditation(payment, order)) {
            return;
        }

        boolean accredited = "processed".equals(order.getStatus()) && "accredited".equals(order.getStatusDetail());
        if (!payment.getCurrency().equals(order.getCurrency()) || !matchesAmount(payment.getAmount(), order.getTotalAmount())
                || (accredited && !matchesAmount(payment.getAmount(), order.getTotalPaidAmount()))) {
            log.error("El importe o la moneda de la Order {} no coincide con el intento local", order.getId());
            return;
        }

        OffsetDateTime updatedAt = OffsetDateTime.now(clock);
        payment.setExternalOrderId(order.getId());
        payment.setStatus(order.getStatus());
        payment.setStatusDetail(statusDetail(order));
        payment.setUpdatedAt(updatedAt);

        if (accredited && payment.getSubscription() == null) {
            Subscription subscription = new Subscription();
            subscription.setUser(payment.getUser());
            subscription.setPlan(Plan.PREMIUM);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setStartedAt(updatedAt);
            payment.setSubscription(subscriptions.save(subscription));
            users.updatePlan(payment.getUser().getId(), Plan.PREMIUM);
        }
    }

    private boolean isOlderThanAccreditation(PaymentTransaction payment, Order order) {
        if (payment.getSubscription() != null && PRE_ACCREDITATION_STATUSES.contains(order.getStatus())) {
            return true;
        }
        return "partially_refunded".equals(payment.getStatusDetail()) && "accredited".equals(order.getStatusDetail());
    }

    private String statusDetail(Order order) {
        String detail = order.getStatusDetail();
        // En sandbox el rechazo reintentable llega en el último pago fallido, dentro de transactions.payments.
        boolean reportsFailure = "failed".equals(order.getStatus())
            || ("action_required".equals(order.getStatus()) && "waiting_retry".equals(detail));
        if (!reportsFailure || order.getTransactions() == null || order.getTransactions().getPayments() == null) {
            return detail;
        }
        return order.getTransactions().getPayments().stream()
            .filter(Objects::nonNull)
            .filter(payment -> "failed".equals(payment.getStatus()) && StringUtils.hasText(payment.getStatusDetail()))
            .max(Comparator.comparing(OrderPayment::getAttemptNumber, Comparator.nullsFirst(Comparator.naturalOrder())))
            .map(OrderPayment::getStatusDetail)
            .orElse(detail);
    }

    private boolean matchesAmount(BigDecimal expected, String received) {
        if (!StringUtils.hasText(received)) {
            return false;
        }
        try {
            return expected.compareTo(new BigDecimal(received)) == 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
