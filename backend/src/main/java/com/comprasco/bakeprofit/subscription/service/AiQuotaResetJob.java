package com.comprasco.bakeprofit.subscription.service;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.auth.repository.UserRepository;

@Component
public class AiQuotaResetJob {

    private static final Logger log = LoggerFactory.getLogger(AiQuotaResetJob.class);
    private final UserRepository userRepository;
    private final Clock clock;

    public AiQuotaResetJob(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /**
     * El lote avanza un mes desde el vencimiento registrado para conservar el calendario de cada
     * usuario aunque la ejecución se retrase. Si quedaron varios períodos vencidos, se recupera
     * uno por cada ejecución.
     */
    @Scheduled(
        initialDelayString = "${app.ai-quota.reset-check.initial-delay}",
        fixedDelayString = "${app.ai-quota.reset-check.fixed-delay}")
    public void run() {
        int updated = userRepository.resetDueAiQuotas(OffsetDateTime.now(clock));
        log.info("Reinicio de cuota de IA: {} usuario(s) actualizado(s)", updated);
    }
}
