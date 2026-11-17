
package com.comprasco.bakeprofit.core.filter;

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

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Un bucket por IP, expira si no se usa en 10 min
    @Value("${app.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    private final LoadingCache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(this::newBucket);

    private Bucket newBucket(String key) {
        int limit = requestsPerMinute > 0 ? requestsPerMinute : 60;
        return Bucket.builder()
                .addLimit(
                        Bandwidth.builder()
                                .capacity(limit)
                                .refillGreedy(limit, Duration.ofMinutes(1))
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

                // LOG TEMPORAL — eliminalo después de debuggear
                logger.debug(String.format("IP=%s remaining=%d consumed=%s capacity=%d",
                        clientIp, probe.getRemainingTokens(), probe.isConsumed(), requestsPerMinute));

                response.setHeader("X-Rate-Limit-Remaining",
                        String.valueOf(probe.getRemainingTokens()));

        if (!probe.isConsumed()) {
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.setHeader("Retry-After", String.valueOf(waitSeconds));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {
                      "status": 429,
                      "error": "Too Many Requests",
                      "message": "Límite de solicitudes excedido. Intenta en %d segundos."
                    }
                    """.formatted(waitSeconds));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        // X-Forwarded-For lo pone el proxy/balanceador (Railway, Render, etc.)
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}