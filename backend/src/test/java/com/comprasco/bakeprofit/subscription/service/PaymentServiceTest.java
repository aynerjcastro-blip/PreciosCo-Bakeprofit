package com.comprasco.bakeprofit.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comprasco.bakeprofit.auth.entity.Plan;
import com.comprasco.bakeprofit.auth.entity.User;
import com.comprasco.bakeprofit.auth.repository.UserRepository;
import com.comprasco.bakeprofit.subscription.config.PremiumCheckoutProperties;
import com.comprasco.bakeprofit.subscription.entity.PaymentTransaction;
import com.comprasco.bakeprofit.subscription.exception.CheckoutGatewayException;
import com.comprasco.bakeprofit.subscription.exception.PremiumAlreadyActiveException;
import com.comprasco.bakeprofit.subscription.repository.PaymentTransactionRepository;
import com.google.gson.JsonObject;
import com.mercadopago.client.order.OrderClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.net.HttpMethod;
import com.mercadopago.net.MPHttpClient;
import com.mercadopago.net.MPResponse;

class PaymentServiceTest {

    private static final long USER_ID = 21L;
    private static final long PAYMENT_ID = 83L;
    private static final String ORDER_ID = "ORDTST01KS5AJ6HTK2HRQ3XJ3C2JCKP9";
    private static final String CHECKOUT_URL = "https://www.mercadopago.com.co/checkout/v1/redirect?order_id=" + ORDER_ID;
    private static final BigDecimal PRICE = new BigDecimal("27900.00");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PaymentTransactionRepository paymentRepository = mock(PaymentTransactionRepository.class);
    private PaymentTransaction savedPayment;

    @BeforeEach
    void setUp() {
        var user = new User();
        user.setId(USER_ID);
        user.setEmail("buyer@example.test");
        user.setPlan(Plan.FREE);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(paymentRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            savedPayment = invocation.getArgument(0);
            savedPayment.setId(PAYMENT_ID);
            return savedPayment;
        });
    }

    @Test
    void rejectsCheckoutForPremiumWithoutCreatingAnIntentOrOrder() {
        userRepository.findById(USER_ID).orElseThrow().setPlan(Plan.PREMIUM);
        MPHttpClient transport = mock(MPHttpClient.class);

        assertThatThrownBy(() -> service(transport).createCheckoutIntent(USER_ID))
            .isInstanceOf(PremiumAlreadyActiveException.class);
        verifyNoInteractions(paymentRepository, transport);
    }

    @Test
    void savesTheIntentFirstAndSendsTheDocumentedOrdersRequest() {
        when(paymentRepository.linkExternalOrder(PAYMENT_ID, ORDER_ID)).thenReturn(1);
        PaymentService service = service(request -> {
            assertThat(savedPayment).isNotNull();
            assertThat(savedPayment.getUser().getId()).isEqualTo(USER_ID);
            assertThat(savedPayment.getStatus()).isEqualTo("CREATED");
            assertThat(savedPayment.getExternalOrderId()).isNull();
            assertThat(savedPayment.getSubscription()).isNull();
            assertThat(savedPayment.getAmount()).isEqualByComparingTo(PRICE);
            assertThat(savedPayment.getCurrency()).isEqualTo("COP");
            String reference = savedPayment.getExternalReference();
            assertThat(UUID.fromString(reference).toString()).isEqualTo(reference);

            JsonObject body = request.getPayload();
            assertThat(request.getUri()).endsWith("/v1/orders");
            assertThat(request.getMethod()).isEqualTo(HttpMethod.POST);
            assertThat(request.getHeaders().entrySet().stream()
                .filter(header -> header.getKey().equalsIgnoreCase("X-Idempotency-Key"))
                .map(Map.Entry::getValue).toList())
                .singleElement().satisfies(key -> assertThat(UUID.fromString(key).toString()).isEqualTo(key));
            assertThat(body.get("external_reference").getAsString()).isEqualTo(reference);
            assertThat(body.get("type").getAsString()).isEqualTo("online");
            assertThat(body.get("processing_mode").getAsString()).isEqualTo("manual");
            assertThat(body.get("total_amount").getAsString()).isEqualTo("27900");
            assertThat(body.getAsJsonObject("payer").get("email").getAsString()).isEqualTo("buyer@example.test");
            var item = body.getAsJsonArray("items").get(0).getAsJsonObject();
            assertThat(item.get("title").getAsString()).isEqualTo("PreciosCo Premium");
            assertThat(item.get("quantity").getAsInt()).isEqualTo(1);
            assertThat(item.get("unit_price").getAsString()).isEqualTo("27900");
            var online = body.getAsJsonObject("config").getAsJsonObject("online");
            assertThat(online.get("success_url").getAsString()).isEqualTo("https://frontend.example.test/success");
            assertThat(online.get("failure_url").getAsString()).isEqualTo("https://frontend.example.test/failure");
            assertThat(online.get("pending_url").getAsString()).isEqualTo("https://frontend.example.test/pending");
            assertThat(online.get("auto_return").getAsString()).isEqualTo("all");
            verify(paymentRepository, never()).linkExternalOrder(anyLong(), anyString());
            return orderResponse("COP");
        });

        var response = service.createCheckoutIntent(USER_ID);

        assertThat(response.externalReference()).isEqualTo(savedPayment.getExternalReference());
        assertThat(response.checkoutUrl()).isEqualTo(CHECKOUT_URL);
        verify(paymentRepository).linkExternalOrder(PAYMENT_ID, ORDER_ID);
    }

    @Test
    void keepsTheCreatedAttemptWhenMercadoPagoRejectsTheOrder() {
        PaymentService service = service(request -> {
            throw new MPApiException("Order rejected", new MPResponse(503, Map.of(), "{}"));
        });

        assertThatThrownBy(() -> service.createCheckoutIntent(USER_ID))
            .isInstanceOf(CheckoutGatewayException.class)
            .hasMessageContaining("No se pudo iniciar el pago con Mercado Pago");
        assertThat(savedPayment.getStatus()).isEqualTo("CREATED");
        assertThat(savedPayment.getExternalOrderId()).isNull();
        verify(paymentRepository, never()).linkExternalOrder(anyLong(), anyString());
    }

    @Test
    void doesNotReturnACheckoutForADifferentCurrency() {
        PaymentService service = service(request -> orderResponse("ARS"));

        assertThatThrownBy(() -> service.createCheckoutIntent(USER_ID)).isInstanceOf(CheckoutGatewayException.class);
        verify(paymentRepository, never()).linkExternalOrder(anyLong(), anyString());
    }

    private PaymentService service(MPHttpClient transport) {
        var settings = new PremiumCheckoutProperties(PRICE, "COP",
            URI.create("https://frontend.example.test/success"), URI.create("https://frontend.example.test/failure"),
            URI.create("https://frontend.example.test/pending"));
        return new PaymentService(userRepository, paymentRepository, new OrderClient(transport), settings, "test-token");
    }

    private MPResponse orderResponse(String currency) {
        return new MPResponse(201, Map.of(), """
            {"id":"%s","currency":"%s","checkout_url":"%s"}
            """.formatted(ORDER_ID, currency, CHECKOUT_URL));
    }
}
