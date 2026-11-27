package com.comprasco.bakeprofit.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.comprasco.bakeprofit.auth.entity.Plan;
import com.comprasco.bakeprofit.auth.entity.Role;
import com.comprasco.bakeprofit.auth.entity.User;
import com.comprasco.bakeprofit.auth.repository.UserRepository;
import com.comprasco.bakeprofit.auth.security.JwtAuthFilter;
import com.comprasco.bakeprofit.auth.security.JwtSecurity;
import com.comprasco.bakeprofit.auth.security.SecurityConfig;
import com.comprasco.bakeprofit.core.config.OpenApiConfig;
import com.comprasco.bakeprofit.core.filter.RateLimitFilter;
import com.comprasco.bakeprofit.core.handler.GlobalExceptionHandler;
import com.comprasco.bakeprofit.core.response.ErrorResponseWriter;
import com.comprasco.bakeprofit.subscription.config.PremiumCheckoutProperties;
import com.comprasco.bakeprofit.subscription.controller.PaymentWebhookController;
import com.comprasco.bakeprofit.subscription.controller.SubscriptionController;
import com.comprasco.bakeprofit.subscription.entity.PaymentTransaction;
import com.comprasco.bakeprofit.subscription.entity.SubscriptionStatus;
import com.comprasco.bakeprofit.subscription.repository.PaymentTransactionRepository;
import com.comprasco.bakeprofit.subscription.repository.SubscriptionRepository;
import com.comprasco.bakeprofit.subscription.service.OrderWebhookService;
import com.comprasco.bakeprofit.subscription.service.PaymentService;
import com.mercadopago.client.order.OrderClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.net.HttpMethod;
import com.mercadopago.net.MPResponse;

import tools.jackson.databind.ObjectMapper;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = ".+")
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(classes = OrderWebhookIntegrationTest.Config.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "app.mercadopago.access-token=webhook-integration-test",
        "app.mercadopago.webhook-secret=" + OrderWebhookIntegrationTest.WEBHOOK_SECRET,
        "jwt.secret=local-only-integration-test-secret-for-webhook",
        "app.rate-limit.requests-per-minute=60",
        "spring.datasource.hikari.maximum-pool-size=2"
    })
class OrderWebhookIntegrationTest {

    static final String WEBHOOK_SECRET = "webhook-integration-signing-secret";
    private static final String WEBHOOK_PATH = "/api/payments/webhook/mercadopago";
    // Cuerpos de la sección de validación y del simulador publicados en la guía de Webhooks de Checkout Pro Orders.
    private static final ClassPathResource ORDER_NOTIFICATION = new ClassPathResource(
        "mercadopago/checkout-pro-orders-notification.json");
    private static final ClassPathResource SIMULATED_NOTIFICATION = new ClassPathResource(
        "mercadopago/checkout-pro-orders-simulated-notification.json");

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableConfigurationProperties(PremiumCheckoutProperties.class)
    @EntityScan(basePackageClasses = {User.class, PaymentTransaction.class})
    @EnableJpaRepositories(basePackageClasses = {UserRepository.class, PaymentTransactionRepository.class})
    @Import({OrderWebhookService.class, PaymentService.class, PaymentWebhookController.class,
        SubscriptionController.class, SecurityConfig.class, OpenApiConfig.class,
        JwtAuthFilter.class, JwtSecurity.class, RateLimitFilter.class, GlobalExceptionHandler.class,
        ErrorResponseWriter.class})
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
                if (testOrders.unavailable.contains(orderId)) {
                    throw new MPApiException("Order unavailable", new MPResponse(503, Map.of(), "{}"));
                }
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
        private final Set<String> unavailable = ConcurrentHashMap.newKeySet();
        private final AtomicInteger calls = new AtomicInteger();
        private volatile CountDownLatch concurrentReads;
    }

    @Autowired private OrderWebhookService service;
    @Autowired private UserRepository users;
    @Autowired private PaymentTransactionRepository payments;
    @Autowired private SubscriptionRepository subscriptions;
    @Autowired private JwtSecurity jwtSecurity;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestOrders orders;
    @Autowired private WebApplicationContext webContext;

    private final List<Long> testUsers = new ArrayList<>();
    private MockMvc mvc;

    @BeforeEach
    void configureMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(webContext).apply(springSecurity()).build();
    }

    @AfterEach
    void cleanTestRows() {
        orders.concurrentReads = null;
        orders.responses.clear();
        orders.unavailable.clear();
        for (Long userId : testUsers) {
            jdbc.update("DELETE FROM payment_transactions WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM subscriptions WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM users WHERE id = ?", userId);
        }
    }

    @Test
    void usesTheSameErrorResponseFieldsAcrossMvcAndTheSecurityFilters() throws Exception {
        String checkout = "/api/subscriptions/checkout";
        Fixture payment = newPayment();
        User buyer = users.findById(payment.userId()).orElseThrow();
        String token = jwtSecurity.generateToken(buyer.getEmail(), "USER");

        assertError(mvc.perform(webhook(payment.orderId(), "{", true)), 400, WEBHOOK_PATH);
        assertError(mvc.perform(webhook(payment.orderId(), orderBody(payment.orderId()), false)), 401, WEBHOOK_PATH);
        assertError(mvc.perform(post(checkout)), 401, checkout);
        String missing = "/api/recurso-inexistente";
        assertError(mvc.perform(get(missing).header("Authorization", "Bearer " + token)), 404, missing);

        respond(payment, "processed", "accredited", "10000", "10000", "COP", "processed", "accredited");
        mvc.perform(signedWebhook(payment.orderId())).andExpect(status().isOk());
        assertError(mvc.perform(post(checkout).header("Authorization", "Bearer " + token)), 409, checkout);
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", payment.userId());
        assertError(mvc.perform(post(checkout).header("Authorization", "Bearer " + token)), 403, checkout);

        orders.unavailable.add(payment.orderId());
        assertError(mvc.perform(signedWebhook(payment.orderId())), 502, WEBHOOK_PATH);

        String rateLimitIp = "192.0.2.24";
        for (int attempt = 0; attempt < 60; attempt++) {
            mvc.perform(post(checkout).header("X-Forwarded-For", rateLimitIp)).andExpect(status().isUnauthorized());
        }
        assertError(mvc.perform(post(checkout).header("X-Forwarded-For", rateLimitIp))
            .andExpect(result -> assertThat(result.getResponse().getHeader("Retry-After")).isNotBlank()), 429, checkout);
    }

    private void assertError(ResultActions result, int code, String path) throws Exception {
        var response = result.andExpect(status().is(code))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(code))
            .andExpect(jsonPath("$.path").value(path))
            .andReturn().getResponse();
        var fields = objectMapper.readTree(response.getContentAsByteArray()).propertyNames();
        assertThat(fields).containsExactlyInAnyOrder("timestamp", "status", "error", "message", "path");
    }

    @Test
    void rejectsAnInvalidSignatureWithoutCallingMercadoPagoOrChangingAnyRows() throws Exception {
        Fixture payment = newPayment();
        var originalPayment = jdbc.queryForMap("SELECT * FROM payment_transactions WHERE id = ?", payment.paymentId());
        var originalUser = jdbc.queryForMap("SELECT * FROM users WHERE id = ?", payment.userId());
        int calls = orders.calls.get();

        mvc.perform(webhook(payment.orderId(), orderBody(payment.orderId()), false))
            .andExpect(status().isUnauthorized());

        assertThat(orders.calls.get()).isEqualTo(calls);
        assertThat(jdbc.queryForMap("SELECT * FROM payment_transactions WHERE id = ?", payment.paymentId()))
            .isEqualTo(originalPayment);
        assertThat(jdbc.queryForMap("SELECT * FROM users WHERE id = ?", payment.userId())).isEqualTo(originalUser);
        assertNotActivated(payment);
    }

    @Test
    void activatesPremiumForAnAccreditedOrderWithTheRecordedReferenceAndAmount() throws Exception {
        Fixture payment = newPayment();
        respond(payment, "processed", "accredited", "10000", "10000", "COP", "processed", "accredited");

        mvc.perform(signedWebhook(payment.orderId())).andExpect(status().isOk());

        assertPayment(payment, "processed", "accredited");
        assertActivated(payment);
    }

    @Test
    void handlesTheSameSignedWebhookTwiceWithoutCreatingASecondSubscription() throws Exception {
        Fixture payment = newPayment();
        respond(payment, "processed", "accredited", "10000", "10000", "COP", "processed", "accredited");
        var request = signedWebhook(payment.orderId());

        mvc.perform(request).andExpect(status().isOk());
        Long originalSubscription = assertActivated(payment);
        mvc.perform(request).andExpect(status().isOk());

        assertThat(assertActivated(payment)).isEqualTo(originalSubscription);
        assertPayment(payment, "processed", "accredited");
    }

    @Test
    void recordsAPendingOrderWithoutActivatingPremium() throws Exception {
        Fixture payment = newPayment();
        respond(payment, "processing", "in_process", "10000", null, "COP", "processing", "in_process");
        mvc.perform(signedWebhook(payment.orderId())).andExpect(status().isOk());
        assertPayment(payment, "processing", "in_process");
        assertNotActivated(payment);
    }

    @Test
    void acknowledgesARejectedOrderAndKeepsTheUserOnFree() throws Exception {
        Fixture payment = newPayment();
        respond(payment, "action_required", "waiting_retry", "10000", null, "COP", "failed", "rejected_by_issuer");
        mvc.perform(signedWebhook(payment.orderId()))
            .andExpect(status().isOk())
            .andExpect(content().string(""));
        assertPayment(payment, "action_required", "rejected_by_issuer");
        assertThat(payments.findById(payment.paymentId()).orElseThrow().getSubscription()).isNull();
        assertNotActivated(payment);
    }

    @Test
    void exposesBearerOnlyForCheckoutAndDescribesTheWebhookSignatureInOpenApi() throws Exception {
        String checkout = "$.paths['/api/subscriptions/checkout'].post";
        String webhook = "$.paths['" + WEBHOOK_PATH + "'].post";

        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath(checkout + ".security[0].bearerAuth").isArray())
            .andExpect(jsonPath(checkout + ".responses['201']").exists())
            .andExpect(jsonPath(checkout + ".responses['200']").doesNotExist())
            .andExpect(jsonPath(checkout + ".responses['403'].description")
                .value("Rol no permitido: solo USER puede iniciar un pago"))
            .andExpect(jsonPath(checkout + ".responses['409'].content['application/json'].schema['$ref']")
                .value("#/components/schemas/ErrorResponse"))
            .andExpect(jsonPath(checkout + ".responses['429']").exists())
            .andExpect(jsonPath(webhook + ".security").isArray())
            .andExpect(jsonPath(webhook + ".security").isEmpty())
            .andExpect(jsonPath(webhook + ".responses['401'].description").value(
                "Firma ausente o inválida, o ID firmado inconsistente"))
            .andExpect(jsonPath(webhook + ".responses['403'].description")
                .value("Origen rechazado por CORS. Este rechazo es independiente del JWT"))
            .andExpect(jsonPath(webhook + ".responses['429']").exists());
    }

    @Test
    void accreditsOnlyOnceUnderConcurrentDeliveryAndSkipsARefundedOrderBeforeCallingMercadoPago() throws Exception {
        Fixture payment = newPayment();
        respond(payment, "processed", "accredited", "10000", "10000", "COP", "processed", "accredited");
        processTogether(payment, payment);

        assertPayment(payment, "processed", "accredited");
        assertActivated(payment);

        respond(payment, "refunded", "refunded", "10000", "10000", "COP", "refunded", "refunded");
        service.process(payment.orderId());
        assertPayment(payment, "refunded", "refunded");
        int callsAfterRefund = orders.calls.get();
        service.process(payment.orderId());
        assertThat(orders.calls.get()).isEqualTo(callsAfterRefund);
    }

    @Test
    void recordsDistinctConcurrentOrdersForOneUserUnderTheSameSubscription() throws Exception {
        Fixture first = newPayment();
        Fixture second = newPaymentFor(users.findById(first.userId()).orElseThrow());
        respond(first, "processed", "accredited", "10000", "10000", "COP", "processed", "accredited");
        respond(second, "processed", "accredited", "10000", "10000", "COP", "processed", "accredited");

        processTogether(first, second);

        assertPayment(first, "processed", "accredited");
        assertPayment(second, "processed", "accredited");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM subscriptions WHERE user_id = ?",
            Long.class, first.userId())).isEqualTo(1L);
        var active = subscriptions.findByUserIdAndStatus(first.userId(), SubscriptionStatus.ACTIVE);
        assertThat(active).singleElement().satisfies(subscription ->
            assertThat(subscription.getPlan()).isEqualTo(Plan.PREMIUM));
        Long subscriptionId = active.getFirst().getId();
        assertThat(jdbc.queryForList("SELECT subscription_id FROM payment_transactions WHERE user_id = ?",
            Long.class, first.userId())).containsExactly(subscriptionId, subscriptionId);
        assertThat(users.findById(first.userId()).orElseThrow().getPlan()).isEqualTo(Plan.PREMIUM);
    }

    @Test
    void rejectsANewCheckoutAfterPremiumIsActivated() throws Exception {
        Fixture payment = newPayment();
        respond(payment, "processed", "accredited", "10000", "10000", "COP", "processed", "accredited");
        mvc.perform(signedWebhook(payment.orderId())).andExpect(status().isOk());
        Long subscriptionId = assertActivated(payment);
        int calls = orders.calls.get();
        User user = users.findById(payment.userId()).orElseThrow();
        String token = jwtSecurity.generateToken(user.getEmail(), user.getRole().name());

        mvc.perform(post("/api/subscriptions/checkout").header("Authorization", "Bearer " + token))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Tu cuenta ya tiene Premium activo."));

        assertThat(orders.calls.get()).isEqualTo(calls);
        assertThat(assertActivated(payment)).isEqualTo(subscriptionId);
    }

    @Test
    void logsOrdersWithADifferentCurrencyAmountOrAccreditedTotal(CapturedOutput output) throws Exception {
        Fixture payment = newPayment();
        respond(payment, "processed", "accredited", "10000", "10000", "USD", "processed", "accredited");
        mvc.perform(signedWebhook(payment.orderId())).andExpect(status().isOk());
        respond(payment, "processed", "accredited", "9000", "9000", "COP", "processed", "accredited");
        mvc.perform(signedWebhook(payment.orderId())).andExpect(status().isOk());
        respond(payment, "processed", "accredited", "10000", "9000", "COP", "processed", "accredited");
        mvc.perform(signedWebhook(payment.orderId())).andExpect(status().isOk());

        assertThat(payments.findById(payment.paymentId()).orElseThrow().getStatus()).isEqualTo("CREATED");
        assertNotActivated(payment);
        String expectedMessage = "El importe o la moneda de la Order " + payment.orderId()
            + " no coincide con el intento local";
        assertThat(output.getAll().lines().filter(line -> line.contains(expectedMessage)).count()).isEqualTo(3L);
    }

    private void processTogether(Fixture first, Fixture second) throws Exception {
        orders.concurrentReads = new CountDownLatch(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstResult = executor.submit(() -> { service.process(first.orderId()); return null; });
            var secondResult = executor.submit(() -> { service.process(second.orderId()); return null; });
            firstResult.get(20, TimeUnit.SECONDS);
            secondResult.get(20, TimeUnit.SECONDS);
        } finally {
            orders.concurrentReads = null;
        }
    }

    @Test
    void acknowledgesAnOrderWithNoMatchingCheckoutIntent(CapturedOutput output) throws Exception {
        Fixture payment = newPayment();
        String unknownReference = UUID.randomUUID().toString();
        respond(payment, "processed", "accredited", "10000", "10000", "COP", "processed", "accredited");
        orders.responses.computeIfPresent(payment.orderId(), (id, response) ->
            response.replace(payment.reference(), unknownReference));
        String body = SIMULATED_NOTIFICATION.getContentAsString(StandardCharsets.UTF_8)
            .replace("ORDTST01M4V4VSXCTN8ZN1FJ02KKMFGE", payment.orderId())
            .replace("ext_ref_manual_full_test", unknownReference);

        mvc.perform(webhook(payment.orderId(), body, true)).andExpect(status().isOk());

        assertThat(payments.findByExternalReference(unknownReference)).isEmpty();
        assertThat(payments.findByExternalOrderId(payment.orderId())).isEmpty();
        assertThat(payments.findById(payment.paymentId()).orElseThrow().getStatus()).isEqualTo("CREATED");
        assertNotActivated(payment);
        assertThat(output.getAll()).contains("No existe un intento local para la Order " + payment.orderId());
    }

    private Long assertActivated(Fixture payment) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM payment_transactions WHERE user_id = ?",
            Long.class, payment.userId())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM subscriptions WHERE user_id = ?",
            Long.class, payment.userId())).isEqualTo(1L);
        var active = subscriptions.findByUserIdAndStatus(payment.userId(), SubscriptionStatus.ACTIVE);
        assertThat(active).singleElement().satisfies(subscription -> assertThat(subscription.getPlan()).isEqualTo(Plan.PREMIUM));
        Long subscriptionId = active.getFirst().getId();
        assertThat(jdbc.queryForObject("SELECT subscription_id FROM payment_transactions WHERE id = ?",
            Long.class, payment.paymentId())).isEqualTo(subscriptionId);
        assertThat(users.findById(payment.userId()).orElseThrow().getPlan()).isEqualTo(Plan.PREMIUM);
        return subscriptionId;
    }

    private void assertNotActivated(Fixture payment) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM subscriptions WHERE user_id = ?",
            Long.class, payment.userId())).isZero();
        assertThat(users.findById(payment.userId()).orElseThrow().getPlan()).isEqualTo(Plan.FREE);
    }

    private MockHttpServletRequestBuilder signedWebhook(String orderId) throws Exception {
        return webhook(orderId, orderBody(orderId), true);
    }

    private String orderBody(String orderId) throws Exception {
        return ORDER_NOTIFICATION.getContentAsString(StandardCharsets.UTF_8)
            .replace("ORD01JYH1Z1YJN4HZ8J3Q0RB3YP6D", orderId);
    }

    private MockHttpServletRequestBuilder webhook(String orderId, String body, boolean validSignature) throws Exception {
        String requestId = UUID.randomUUID().toString();
        long timestamp = Instant.now().getEpochSecond();
        String manifest = "id:%s;request-id:%s;ts:%s;".formatted(orderId.toLowerCase(Locale.ROOT), requestId, timestamp);
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String digest = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
        if (!validSignature) {
            digest = (digest.startsWith("0") ? "1" : "0") + digest.substring(1);
        }
        return post(WEBHOOK_PATH).queryParam("data.id", orderId).queryParam("type", "order")
            .header("x-request-id", requestId).header("x-signature", "ts=%s,v1=%s".formatted(timestamp, digest))
            .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private Fixture newPayment() {
        var user = new User();
        user.setName("Prueba webhook");
        user.setEmail(UUID.randomUUID() + "@example.test");
        user.setRole(Role.USER);
        user.setPassword("fixture");
        users.saveAndFlush(user);
        testUsers.add(user.getId());
        return newPaymentFor(user);
    }

    private Fixture newPaymentFor(User user) {
        String reference = UUID.randomUUID().toString();
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
