package com.comprasco.bakeprofit.core.filter;

import com.comprasco.bakeprofit.core.response.ErrorResponseWriter;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Filtro de rate limiting por IP usando Token Bucket (Bucket4j + Caffeine).
 *
 * Se usa {@code intervally} en vez de {@code greedy} para que los tokens
 * se recarguen de golpe al cumplirse el minuto, no de forma continua.
 * Con greedy un flujo constante de requests nunca agota el bucket
 * porque recarga ~1 token/segundo; con intervally, la request 61
 * dentro del mismo minuto recibe 429 sin importar el ritmo.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ErrorResponseWriter errors;

    @Value("${app.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    // Un bucket por IP, se descarta si la IP no hace requests en 10 min
    private final LoadingCache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(this::newBucket);

    public RateLimitFilter(ErrorResponseWriter errors) {
        this.errors = errors;
    }

    private Bucket newBucket(String key) {
        int limit = requestsPerMinute > 0 ? requestsPerMinute : 60;
        return Bucket.builder()
                .addLimit(
                        Bandwidth.builder()
                                .capacity(limit)
                                .refillIntervally(limit, Duration.ofMinutes(1))
                                .build())
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        Bucket bucket = buckets.get(clientIp);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        response.setHeader("X-Rate-Limit-Remaining",
                String.valueOf(probe.getRemainingTokens()));

        if (!probe.isConsumed()) {
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.setHeader("Retry-After", String.valueOf(waitSeconds));
            errors.write(request, response, HttpStatus.TOO_MANY_REQUESTS,
                    "Límite de solicitudes excedido. Intenta en %d segundos.".formatted(waitSeconds));
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resuelve la IP real del cliente.
     * Detrás de un proxy/balanceador (Railway, Render, etc.)
     * la IP viene en X-Forwarded-For; sin proxy, usa remoteAddr.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
