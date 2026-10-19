package com.comprasco.bakeprofit.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.comprasco.bakeprofit.security.JwtAuthFilter;

/**
 * Configuración central de seguridad: filtros HTTP, codificación de
 * contraseñas y política CORS.
 *
 * <p>
 * Las rutas públicas y los permisos por rol se declaran en
 * {@link #filterChain}. Cualquier endpoint nuevo queda protegido por
 * defecto.
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final JwtAuthFilter jwtAuthFilter;

        public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
                this.jwtAuthFilter = jwtAuthFilter;
        }

        /**
         * Define las reglas de autenticación y autorización de la API.
         *
         * <p>
         * Las reglas se evalúan en orden: primero las excepciones y al final
         * {@code anyRequest().authenticated()}.
         * </p>
         */
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                String[] catalog = {
                                "/api/categories/**",
                                "/api/stores/**",
                                "/api/products/**"
                };

                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setStatus(
                                                                        jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                                                        response.setContentType("application/json;charset=UTF-8");
                                                        response.getWriter()
                                                                        .write("""
                                                                                {
                                                                                "status": 401,
                                                                                "error": "Unauthorized",
                                                                                "message": "Acceso no autorizado o cuenta desactivada",
                                                                                "path": "%s"
                                                                                        }
                                                                                        """
                                                                                        .formatted(request
                                                                                                        .getRequestURI()));
                                                }))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, catalog).permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/prices/compare").permitAll()
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public BCryptPasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        /**
         * Orígenes permitidos para el frontend.
         *
         * <p>
         * En producción deben provenir de una propiedad de configuración,
         * no quedar escritos en el código.
         * </p>
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOrigins(List.of(
                                "http://localhost:5173",
                                "http://127.0.0.1:5173"));

                configuration.setAllowedMethods(List.of(
                                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

                configuration.setAllowedHeaders(List.of(
                                "Content-Type", "Authorization"));

                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}