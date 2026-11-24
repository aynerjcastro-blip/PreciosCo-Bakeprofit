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

    @Scheduled(
        initialDelayString = "${app.ai-quota.reset-check.initial-delay:PT1M}",
        fixedDelayString = "${app.ai-quota.reset-check.fixed-delay:PT1H}")
    public void run() {
        int updated = userRepository.resetDueAiQuotas(OffsetDateTime.now(clock));
        log.info("Reinicio de cuota de IA: {} usuario(s) actualizado(s)", updated);
    }
}
