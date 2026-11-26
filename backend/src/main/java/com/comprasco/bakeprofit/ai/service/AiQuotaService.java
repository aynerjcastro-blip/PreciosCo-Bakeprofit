package com.comprasco.bakeprofit.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
 * <p>
 * La consulta se <b>reserva</b> antes de llamar a la IA con un UPDATE
 * condicional (atómico), y se <b>devuelve</b> si la llamada falla. Así varias
 * peticiones en paralelo no pueden superar el límite.
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
     * Reserva una consulta para el usuario.
     *
     * @return consultas usadas este mes después de reservar
     * @throws AiQuotaExceededException si el usuario FREE agotó su cuota
     */
    public int consume(User user) {
        if (user.getPlan() == Plan.PREMIUM) {
            userRepository.incrementAiQueriesUsed(user.getId());
        } else if (userRepository.tryConsumeAiQuery(user.getId(), freeMonthlyLimit) == 0) {
            throw new AiQuotaExceededException(freeMonthlyLimit, freeMonthlyLimit);
        }
        return userRepository.findAiQueriesUsedById(user.getId());
    }

    /** Devuelve una consulta reservada cuando la IA no pudo responder. */
    public void refund(User user) {
        userRepository.refundAiQuery(user.getId());
    }

    /** Límite mensual del usuario (0 = ilimitado / PREMIUM). */
    public int limitFor(User user) {
        return user.getPlan() == Plan.PREMIUM ? 0 : freeMonthlyLimit;
    }

    /** Límite mensual para usuarios FREE. */
    public int getFreeMonthlyLimit() {
        return freeMonthlyLimit;
    }
}
