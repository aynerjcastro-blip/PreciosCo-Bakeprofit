package com.comprasco.bakeprofit.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.ai.exception.AiQuotaExceededException;
import com.comprasco.bakeprofit.auth.entity.Plan;
import com.comprasco.bakeprofit.auth.entity.User;
import com.comprasco.bakeprofit.auth.repository.UserRepository;

/**
 * Gestiona la cuota de consultas IA por usuario.
 * <p>
 * Los usuarios FREE tienen un límite mensual configurable.
 * Los usuarios PREMIUM no tienen límite.
 * </p>
 */
@Service
public class AiQuotaService {

    private final UserRepository userRepository;
    private final int freeMonthlyLimit;

    public AiQuotaService(
            UserRepository userRepository,
            @Value("${app.ai-quota.free-monthly-limit:20}") int freeMonthlyLimit) {
        this.userRepository = userRepository;
        this.freeMonthlyLimit = freeMonthlyLimit;
    }

    /**
     * Verifica que el usuario tenga cuota disponible.
     *
     * @throws AiQuotaExceededException si el usuario FREE agotó su cuota
     */
    public void assertQuotaAvailable(User user) {
        if (user.getPlan() == Plan.PREMIUM) {
            return; // sin límite
        }
        if (user.getAiQueriesUsed() >= freeMonthlyLimit) {
            throw new AiQuotaExceededException(user.getAiQueriesUsed(), freeMonthlyLimit);
        }
    }

    /**
     * Incrementa el contador de consultas usadas en 1.
     * Se llama DESPUÉS de obtener la respuesta exitosa de la IA.
     */
    @Transactional
    public void incrementUsage(User user) {
        userRepository.incrementAiQueriesUsed(user.getId());
    }

    /** Límite mensual para usuarios FREE. */
    public int getFreeMonthlyLimit() {
        return freeMonthlyLimit;
    }
}