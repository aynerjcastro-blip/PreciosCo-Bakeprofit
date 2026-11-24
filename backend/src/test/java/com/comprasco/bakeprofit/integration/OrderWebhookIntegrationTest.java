package com.comprasco.bakeprofit.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

import com.comprasco.bakeprofit.auth.entity.Plan;
import com.comprasco.bakeprofit.auth.entity.Role;
import com.comprasco.bakeprofit.auth.entity.User;
import com.comprasco.bakeprofit.auth.repository.UserRepository;
import com.comprasco.bakeprofit.subscription.entity.PaymentTransaction;
import com.comprasco.bakeprofit.subscription.entity.SubscriptionStatus;
import com.comprasco.bakeprofit.subscription.repository.PaymentTransactionRepository;
import com.comprasco.bakeprofit.subscription.repository.SubscriptionRepository;
import com.comprasco.bakeprofit.subscription.service.OrderWebhookService;
import com.mercadopago.client.order.OrderClient;
import com.mercadopago.net.HttpMethod;
import com.mercadopago.net.MPResponse;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = ".+")
@SpringBootTest(classes = OrderWebhookIntegrationTest.Config.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "app.mercadopago.access-token=webhook-integration-test",
        "spring.datasource.hikari.maximum-pool-size=2"
    })
class OrderWebhookIntegrationTest {

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {User.class, PaymentTransaction.class})
    @EnableJpaRepositories(basePackageClasses = {UserRepository.class, PaymentTransactionRepository.class})
    @Import(OrderWebhookService.class)
    static class Config {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }

        @Bean
        TestOrders testOrders() {
            return new TestOrders();
        }

        @Bean
        OrderClient orderClient(TestOrders testOrders) {
            return new OrderClient(request -> {
                assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
                assertThat(request.getUri()).contains("/v1/orders/");
                assertThat(request.getHeaders()).containsEntry("Authorization", "Bearer webhook-integration-test");
                String orderId = request.getUri().substring(request.getUri().lastIndexOf('/') + 1);
                testOrders.calls.incrementAndGet();
                CountDownLatch concurrentReads = testOrders.concurrentReads;
                if (concurrentReads != null) {
                    concurrentReads.countDown();
                    try {
                        assertThat(concurrentReads.await(10, TimeUnit.SECONDS)).isTrue();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(ex);
                    }
                }
                String response = testOrders.responses.get(orderId);
                assertThat(response).as("respuesta para la Order %s", orderId).isNotNull();
                return new MPResponse(200, Map.of(), response);
            });
        }
    }

    static class TestOrders {
        private final Map<String, String> responses = new ConcurrentHashMap<>();
        private final AtomicInteger calls = new AtomicInteger();
        private volatile CountDownLatch concurrentReads;
    }

    @Autowired private OrderWebhookService service;
    @Autowired private UserRepository users;
    @Autowired private PaymentTransactionRepository payments;
    @Autowired private SubscriptionRepository subscriptions;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TestOrders orders;

    private final List<Long> testUsers = new ArrayList<>();

    @AfterEach
    void cleanTestRows() {
        orders.concurrentReads = null;
        orders.responses.clear();
        for (Long userId : testUsers) {
            jdbc.update("DELETE FROM payment_transactions WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM subscriptions WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM users WHERE id = ?", userId);
        }
    }

    @Test
    void recordsTheObservedPendingAndRejectedOrdersWithoutActivatingPremium() throws Exception {
        Fixture payment = newPayment();
        respond(payment, "processing", "in_process", "10000", null, "COP", "processing", "in_process");
        service.process(payment.orderId());
        assertPayment(payment, "processing", "in_process");

        respond(payment, "action_required", "waiting_retry", "10000", null, "COP", "failed", "rejected_by_issuer");
        service.process(payment.orderId());
        assertPayment(payment, "action_required", "rejected_by_issuer");
        assertThat(subscriptions.findByUserIdAndStatus(payment.userId(), SubscriptionStatus.ACTIVE)).isEmpty();
        assertThat(users.findById(payment.userId()).orElseThrow().getPlan()).isEqualTo(Plan.FREE);
    }

    @Test
    void accreditsOnlyOnceUnderConcurrentDeliveryAndSkipsARefundedOrderBeforeCallingMercadoPago() throws Exception {
        Fixture payment = newPayment();
        respond(payment, "processed", "accredited", "10000", "10000", "COP", "processed", "accredited");
        orders.concurrentReads = new CountDownLatch(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { service.process(payment.orderId()); return null; });
            var second = executor.submit(() -> { service.process(payment.orderId()); return null; });
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        } finally {
            orders.concurrentReads = null;
        }

        assertPayment(payment, "processed", "accredited");
        var active = subscriptions.findByUserIdAndStatus(payment.userId(), SubscriptionStatus.ACTIVE);
        assertThat(active).singleElement().satisfies(subscription -> {
            assertThat(subscription.getPlan()).isEqualTo(Plan.PREMIUM);
            assertThat(jdbc.queryForObject("SELECT subscription_id FROM payment_transactions WHERE id = ?",
                Long.class, payment.paymentId())).isEqualTo(subscription.getId());
        });
        assertThat(users.findById(payment.userId()).orElseThrow().getPlan()).isEqualTo(Plan.PREMIUM);

        respond(payment, "refunded", "refunded", "10000", "10000", "COP", "refunded", "refunded");
        service.process(payment.orderId());
        assertPayment(payment, "refunded", "refunded");
        int callsAfterRefund = orders.calls.get();
        service.process(payment.orderId());
        assertThat(orders.calls.get()).isEqualTo(callsAfterRefund);
    }

    @Test
    void ignoresOrdersWithADifferentCurrencyAmountOrAccreditedTotal() throws Exception {
        Fixture payment = newPayment();
        respond(payment, "processed", "accredited", "10000", "10000", "USD", "processed", "accredited");
        service.process(payment.orderId());
        respond(payment, "processed", "accredited", "9000", "9000", "COP", "processed", "accredited");
        service.process(payment.orderId());
        respond(payment, "processed", "accredited", "10000", "9000", "COP", "processed", "accredited");
        service.process(payment.orderId());

        assertThat(payments.findById(payment.paymentId()).orElseThrow().getStatus()).isEqualTo("CREATED");
        assertThat(subscriptions.findByUserIdAndStatus(payment.userId(), SubscriptionStatus.ACTIVE)).isEmpty();
    }

    private Fixture newPayment() {
        String reference = UUID.randomUUID().toString();
        var user = new User();
        user.setName("Prueba webhook");
        user.setEmail(reference + "@example.test");
        user.setRole(Role.USER);
        user.setPassword("fixture");
        users.saveAndFlush(user);
        testUsers.add(user.getId());

        var payment = new PaymentTransaction();
        payment.setUser(user);
        payment.setExternalReference(reference);
        payment.setStatus("CREATED");
        payment.setAmount(new BigDecimal("10000.00"));
        payment.setCurrency("COP");
        payments.saveAndFlush(payment);

        String orderId = "ORDTST" + reference.replace("-", "").substring(0, 26).toUpperCase(Locale.ROOT);
        return new Fixture(user.getId(), payment.getId(), reference, orderId);
    }

    private void respond(Fixture payment, String status, String detail, String total, String paidTotal,
                         String currency, String paymentStatus, String paymentDetail) {
        String paid = paidTotal == null ? "null" : '"' + paidTotal + '"';
        orders.responses.put(payment.orderId(), """
            {"id":"%s","type":"online","external_reference":"%s","currency":"%s",
             "status":"%s","status_detail":"%s","total_amount":"%s","total_paid_amount":%s,
             "transactions":{"payments":[{"status":"%s","status_detail":"%s","attempt_number":1}]}}
            """.formatted(payment.orderId(), payment.reference(), currency, status, detail, total, paid,
                paymentStatus, paymentDetail));
    }

    private void assertPayment(Fixture fixture, String status, String detail) {
        assertThat(payments.findById(fixture.paymentId())).hasValueSatisfying(payment -> {
            assertThat(payment.getExternalOrderId()).isEqualTo(fixture.orderId());
            assertThat(payment.getStatus()).isEqualTo(status);
            assertThat(payment.getStatusDetail()).isEqualTo(detail);
        });
    }

    record Fixture(Long userId, Long paymentId, String reference, String orderId) {
    }
}
