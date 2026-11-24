package com.comprasco.bakeprofit.subscription.controller;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comprasco.bakeprofit.auth.entity.User;
import com.comprasco.bakeprofit.auth.security.CurrentUser;
import com.comprasco.bakeprofit.core.response.ErrorResponse;
import com.comprasco.bakeprofit.subscription.dto.CheckoutIntentResponse;
import com.comprasco.bakeprofit.subscription.exception.CheckoutGatewayException;
import com.comprasco.bakeprofit.subscription.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final PaymentService paymentService;

    public SubscriptionController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CheckoutIntentResponse> checkout(@CurrentUser User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createCheckoutIntent(user.getId()));
    }

    @ExceptionHandler(CheckoutGatewayException.class)
    public ResponseEntity<ErrorResponse> handleCheckoutGatewayError(CheckoutGatewayException ex,
                                                                    HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .build());
    }
}
