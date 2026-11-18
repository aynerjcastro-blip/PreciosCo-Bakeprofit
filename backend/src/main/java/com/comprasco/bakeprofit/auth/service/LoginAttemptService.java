package com.comprasco.bakeprofit.auth.service;

import com.comprasco.bakeprofit.auth.entity.User;
import com.comprasco.bakeprofit.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class LoginAttemptService {

    private final UserRepository userRepository;

    @Value("${app.security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    public LoginAttemptService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // REQUIRES_NEW = abre su propia transacción, independiente de la del caller
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailedAttempt(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        int newCount = (user.getFailedAttempts() != null ? user.getFailedAttempts() : 0) + 1;
        user.setFailedAttempts(newCount);

        if (newCount >= maxFailedAttempts) {
            user.setLockTime(LocalDateTime.now());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetAttempts(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getFailedAttempts() != null && user.getFailedAttempts() > 0) {
            user.setFailedAttempts(0);
            user.setLockTime(null);
        }
    }
}