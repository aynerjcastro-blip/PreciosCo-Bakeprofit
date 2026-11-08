package com.comprasco.bakeprofit.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.comprasco.bakeprofit.dto.PasswordResetToken;


public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken,Long> {
    //*Buscamos el token al momento de que el usuaria clickea el boton de recuperacion */
    Optional<PasswordResetToken> findByToken (String token);
    //*Para limpiar tokens viejos de un usuario si solicita uno nuevo */
    void deleteByUserId(Long userId);
}

