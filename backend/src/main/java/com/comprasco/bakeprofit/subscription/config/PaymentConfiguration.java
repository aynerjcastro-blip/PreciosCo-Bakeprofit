package com.comprasco.bakeprofit.subscription.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mercadopago.client.order.OrderClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PremiumCheckoutProperties.class)
public class PaymentConfiguration {

    @Bean
    public OrderClient mercadoPagoOrderClient() {
        return new OrderClient();
    }
}
