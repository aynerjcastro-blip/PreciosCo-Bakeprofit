package com.comprasco.bakeprofit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Clase de configuración global de Spring Framework.
 * Define beans y componentes transversales necesarios en todo el ciclo de vida de la aplicación.
 */
@Configuration
public class AppConfig {

    /**
     * Registra un bean de tipo RestTemplate en el contenedor de Spring.
     * Permite realizar peticiones HTTP de forma sencilla hacia servicios externos o APIs de staging.
     *
     * @return Instancia configurada de RestTemplate.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}