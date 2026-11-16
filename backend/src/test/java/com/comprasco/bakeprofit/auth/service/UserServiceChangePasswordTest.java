package com.comprasco.bakeprofit.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.comprasco.bakeprofit.auth.dto.ChangePasswordRequest;
import com.comprasco.bakeprofit.auth.entity.AuthProvider;
import com.comprasco.bakeprofit.auth.entity.User;
import com.comprasco.bakeprofit.auth.exception.ForbiddenActionException;
import com.comprasco.bakeprofit.auth.exception.InvalidPasswordChangeException;
import com.comprasco.bakeprofit.auth.exception.UserNotFoundException;
import com.comprasco.bakeprofit.auth.repository.PasswordResetTokenRepository;
import com.comprasco.bakeprofit.auth.repository.UserRepository;
import com.comprasco.bakeprofit.auth.security.JwtSecurity;
import com.comprasco.bakeprofit.notification.service.EmailService;

/** Cambio de contraseña con sesión iniciada: pide la actual y rechaza las cuentas de Google. */
@ExtendWith(MockitoExtension.class)
class UserServiceChangePasswordTest {

    private static final String ACTUAL = "Actual1234";
    private static final String NUEVA = "Nueva56789";

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtSecurity jwtSecurity;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private EmailService emailService;

    // Codificador real y con coste bajo: así se prueba el hash de verdad sin que el test tarde
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, encoder, jwtSecurity, tokenRepository, emailService);
    }

    private User user(AuthProvider provider) {
        User user = new User();
        user.setPassword(encoder.encode(ACTUAL));
        user.setAuthProvider(provider);
        return user;
    }

    @Test
    void conLaActualCorrectaGuardaLaNuevaCodificada() {
        User user = user(AuthProvider.LOCAL);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.changePassword(1L, new ChangePasswordRequest(ACTUAL, NUEVA));

        assertThat(encoder.matches(NUEVA, user.getPassword())).isTrue();
        assertThat(user.getPassword()).isNotEqualTo(NUEVA);
        verify(userRepository).save(user);
    }

    @Test
    void conLaActualIncorrectaRechazaYNoCambiaNada() {
        User user = user(AuthProvider.LOCAL);
        String hashAntes = user.getPassword();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.changePassword(1L, new ChangePasswordRequest("otra-clave", NUEVA)))
                .isInstanceOf(InvalidPasswordChangeException.class)
                .hasMessage("La contraseña actual es incorrecta");

        assertThat(user.getPassword()).isEqualTo(hashAntes);
        verify(userRepository, never()).save(any());
    }

    @Test
    void siLaNuevaEsIgualALaActualRechaza() {
        User user = user(AuthProvider.LOCAL);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.changePassword(1L, new ChangePasswordRequest(ACTUAL, ACTUAL)))
                .isInstanceOf(InvalidPasswordChangeException.class)
                .hasMessage("La nueva contraseña debe ser distinta de la actual");

        verify(userRepository, never()).save(any());
    }

    @Test
    void lasCuentasDeGoogleNoPuedenCambiarContrasena() {
        User user = user(AuthProvider.GOOGLE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.changePassword(1L, new ChangePasswordRequest(ACTUAL, NUEVA)))
                .isInstanceOf(ForbiddenActionException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void conUnUsuarioInexistenteResponde404() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword(99L, new ChangePasswordRequest(ACTUAL, NUEVA)))
                .isInstanceOf(UserNotFoundException.class);
    }
}