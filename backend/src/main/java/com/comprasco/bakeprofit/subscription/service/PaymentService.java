package com.comprasco.bakeprofit.subscription.service;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.comprasco.bakeprofit.auth.entity.User;
import com.comprasco.bakeprofit.auth.exception.UserNotFoundException;
import com.comprasco.bakeprofit.auth.repository.UserRepository;
import com.comprasco.bakeprofit.subscription.config.PremiumCheckoutProperties;
import com.comprasco.bakeprofit.subscription.dto.CheckoutIntentResponse;
import com.comprasco.bakeprofit.subscription.entity.PaymentTransaction;
import com.comprasco.bakeprofit.subscription.exception.CheckoutGatewayException;
import com.comprasco.bakeprofit.subscription.repository.PaymentTransactionRepository;
import com.mercadopago.client.order.OrderClient;
import com.mercadopago.client.order.OrderConfigRequest;
import com.mercadopago.client.order.OrderCreateRequest;
import com.mercadopago.client.order.OrderItemRequest;
import com.mercadopago.client.order.OrderOnlineConfig;
import com.mercadopago.client.order.OrderPayerRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.order.Order;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final UserRepository userRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final OrderClient orderClient;
    private final PremiumCheckoutProperties checkout;
    private final String accessToken;

    public PaymentService(UserRepository userRepository, PaymentTransactionRepository paymentRepository,
                          OrderClient orderClient, PremiumCheckoutProperties checkout,
                          @Value("${app.mercadopago.access-token:}") String accessToken) {
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.orderClient = orderClient;
        this.checkout = checkout;
        this.accessToken = accessToken;
    }

    // La transacción del repositorio confirma el intento antes de llamar a Mercado Pago.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CheckoutIntentResponse createCheckoutIntent(Long userId) {
        validateConfiguration();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
        String externalReference = UUID.randomUUID().toString();

        PaymentTransaction payment = new PaymentTransaction();
        payment.setUser(user);
        payment.setExternalReference(externalReference);
        payment.setStatus("CREATED");
        payment.setAmount(checkout.price());
        payment.setCurrency(checkout.currency());
        PaymentTransaction savedPayment = paymentRepository.save(payment);

        Order order = createOrder(user, externalReference);
        if (order == null || !StringUtils.hasText(order.getId()) || !StringUtils.hasText(order.getCheckoutUrl())
                || !checkout.currency().equals(order.getCurrency())) {
            log.warn("Mercado Pago devolvió una Order incompleta o con moneda distinta para {}", externalReference);
            throw new CheckoutGatewayException();
        }

        if (paymentRepository.linkExternalOrder(savedPayment.getId(), order.getId()) != 1) {
            throw new IllegalStateException("No se pudo vincular la Order al intento " + externalReference);
        }
        return new CheckoutIntentResponse(externalReference, order.getCheckoutUrl());
    }

    private Order createOrder(User user, String externalReference) {
        BigDecimal price = checkout.price().stripTrailingZeros();
        String amount = price.scale() <= 0 ? price.toPlainString() : price.setScale(2).toPlainString();
        OrderItemRequest item = OrderItemRequest.builder()
            .title("PreciosCo Premium")
            .quantity(1)
            .unitPrice(amount)
            .build();
        OrderOnlineConfig online = OrderOnlineConfig.builder()
            .successUrl(checkout.successUrl().toString())
            .failureUrl(checkout.failureUrl().toString())
            .pendingUrl(checkout.pendingUrl().toString())
            .autoReturn("all")
            .build();
        OrderCreateRequest request = OrderCreateRequest.builder()
            .type("online")
            .processingMode("manual")
            .totalAmount(amount)
            .externalReference(externalReference)
            .payer(OrderPayerRequest.builder().email(user.getEmail()).build())
            .items(List.of(item))
            .config(OrderConfigRequest.builder().online(online).build())
            .build();
        MPRequestOptions options = MPRequestOptions.builder()
            .accessToken(accessToken)
            .build();

        try {
            return orderClient.create(request, options);
        } catch (MPApiException | MPException ex) {
            String reason = ex instanceof MPApiException apiError ? "HTTP " + apiError.getStatusCode()
                : ex.getClass().getSimpleName();
            log.warn("No se pudo crear la Order {} en Mercado Pago ({})", externalReference, reason);
            throw new CheckoutGatewayException();
        }
    }

    private void validateConfiguration() {
        BigDecimal price = checkout.price();
        if (price == null || price.signum() <= 0 || price.stripTrailingZeros().scale() > 2
                || price.precision() - price.scale() > 10) {
            throw new IllegalStateException("Configura app.premium.price con un importe positivo válido para NUMERIC(12,2)");
        }
        if (checkout.currency() == null || !checkout.currency().matches("[A-Z]{3}")) {
            throw new IllegalStateException("Configura app.premium.currency con el código de moneda de la cuenta");
        }
        if (!isWebUrl(checkout.successUrl()) || !isWebUrl(checkout.failureUrl()) || !isWebUrl(checkout.pendingUrl())) {
            throw new IllegalStateException("Configura las URLs de retorno de Premium con URLs absolutas HTTP o HTTPS");
        }
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalStateException("Configura app.mercadopago.access-token para habilitar Checkout Pro");
        }
    }

    private boolean isWebUrl(URI url) {
        return url != null && url.getHost() != null
            && ("https".equalsIgnoreCase(url.getScheme()) || "http".equalsIgnoreCase(url.getScheme()));
    }
}
