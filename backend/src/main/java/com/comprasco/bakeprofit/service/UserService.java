package com.comprasco.bakeprofit.service;

import com.comprasco.bakeprofit.config.JwtSecurity;
import com.comprasco.bakeprofit.dto.AuthResponse;
import com.comprasco.bakeprofit.dto.ForgotPasswordRequest;
import com.comprasco.bakeprofit.dto.GoogleLoginRequest;
import com.comprasco.bakeprofit.dto.LoginRequest;
import com.comprasco.bakeprofit.dto.PasswordResetToken;
import com.comprasco.bakeprofit.dto.ProfileResponse;
import com.comprasco.bakeprofit.dto.RegisterRequest;
import com.comprasco.bakeprofit.dto.ResetPasswordRequest;
import com.comprasco.bakeprofit.dto.UpdateDisplayNameRequest;
import com.comprasco.bakeprofit.dto.UpdateProfileRequest;
import com.comprasco.bakeprofit.entity.AuthProvider;
import com.comprasco.bakeprofit.entity.Role;
import com.comprasco.bakeprofit.entity.User;
import com.comprasco.bakeprofit.exception.EmailAlreadyExistsException;
import com.comprasco.bakeprofit.exception.ForbiddenActionException;
import com.comprasco.bakeprofit.exception.InactiveUserException;
import com.comprasco.bakeprofit.exception.InvalidCredentialsException;
import com.comprasco.bakeprofit.exception.UserNotFoundException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.comprasco.bakeprofit.repository.PasswordResetTokenRepository;
import com.comprasco.bakeprofit.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio encargado de la gestion de usuarios, registro y autenticacion.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtSecurity jwtSecurity;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    @Value("${google.client-id}")
    private String googleClientId;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JwtSecurity jwtSecurity,
            PasswordResetTokenRepository tokenRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtSecurity = jwtSecurity;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    /* CONSULTAS */

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con id: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con email: " + email));
    }

    public List<User> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

    /* REGISTRO / AUTH */

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);

        String token = jwtSecurity.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        return new AuthResponse(token, savedUser.getName(), savedUser.getEmail(), savedUser.getRole());
    }

    /**
     * Verifica credenciales de usuario y valida estado activo.
     *
     * @param email       correo del usuario
     * @param rawPassword contrasena en texto plano
     * @return entidad User si credenciales y estado son validos
     */
    public User verifyCredentials(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        // RN-10: Validar cuenta activa
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new InactiveUserException("La cuenta se encuentra desactivada. Contacte al administrador.");
        }

        return user;
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = findById(id);
        user.setActive(false);
    }

    @Transactional
    public void activateUser(Long id) {
        User user = findById(id);
        user.setActive(true);
    }

    /**
     * Inicia sesion y genera un token JWT para usuarios activos.
     *
     * @param request DTO con email y password
     * @return AuthResponse con el token generado
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        // RN-10: Rechazar login de cuentas inactivas
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new InactiveUserException("La cuenta se encuentra desactivada. Contacte al administrador.");
        }

        String token = jwtSecurity.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole());
    }

    /**
     * Autentica o registra un usuario mediante el token de Google OAuth.
     */
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        GoogleIdToken idToken;
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(List.of(googleClientId))
                    .build();

            idToken = verifier.verify(request.credential());
        } catch (GeneralSecurityException | IOException e) {
            log.error("No se pudo verificar el token de Google", e);
            throw new InvalidCredentialsException();
        }

        if (idToken == null) {
            log.warn("Token de Google inválido o con audience distinto al configurado");
            throw new InvalidCredentialsException();
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        // Sin esto, un correo no verificado podría entrar a una cuenta existente
        // (DP-22)
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new InvalidCredentialsException();
        }

        String email = payload.getEmail();
        String name = (String) payload.get("name");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setName(name != null ? name : email.split("@")[0]);
            newUser.setEmail(email);
            // La columna password es not-null; esta contraseña nunca se usa
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            newUser.setRole(Role.USER);
            newUser.setActive(true);
            newUser.setAuthProvider(AuthProvider.GOOGLE);
            return userRepository.save(newUser);
        });

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new InactiveUserException("La cuenta se encuentra desactivada. Contacte al administrador.");
        }

        String token = jwtSecurity.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole());
    }

    /**
     * Solicita el restablecimiento de contraseña.
     */
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.email());

        // Si el usuario no existe, salimos silenciosamente sin lanzar error.
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();

        // 1. Limpiamos tokens anteriores para evitar acumulación en BD
        tokenRepository.deleteByUserId(user.getId());

        // 2. Generamos y guardamos el nuevo token
        PasswordResetToken resetToken = new PasswordResetToken(user);
        tokenRepository.save(resetToken);

        // 3. Enviamos el correo (Reemplaza esto con tu método real de SCRUM-138)
        String resetUrl = "http://localhost:5173/reset-password?token=" + resetToken.getToken();

        try {
            emailService.sendEmail(user.getEmail(), "Recuperar contraseña", "Tu enlace: " + resetUrl);
        } catch (MailException e) {
            // Responder igual exista o no el correo; el fallo queda solo en el log
            log.error("No se pudo enviar el correo de recuperación", e);
        }

    }

    /**
     * Valida el token y cambia la contraseña.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Enlace inválido o ya utilizado."));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken); // Limpieza proactiva
            throw new IllegalArgumentException("El enlace ha expirado a los 30 minutos. Solicita uno nuevo.");
        }

        // Actualizamos la contraseña cifrada
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Eliminamos el token para que no se pueda volver a usar
        tokenRepository.delete(resetToken);
    }

    public ProfileResponse getProfile(Long userId) {
        User user = findById(userId);
        return new ProfileResponse(user.getName(), user.getEmail(), user.getRole(),
                user.getAuthProvider() == AuthProvider.LOCAL);
    }

    @Transactional
    public AuthResponse updateProfile(Long userId, UpdateProfileRequest request) {
        // El usuario del SecurityContext llega desconectado de la sesión; se recarga
        // para poder modificarlo
        User user = findById(userId);

        String newName = request.name().trim();
        String newEmail = request.email().trim();
        boolean emailChanged = !newEmail.equals(user.getEmail());

        if (emailChanged) {
            // Google identifica la cuenta por correo; cambiarlo rompería su inicio de
            // sesión (DP-23)
            if (user.getAuthProvider() == AuthProvider.GOOGLE) {
                throw new ForbiddenActionException(
                        "Las cuentas creadas con Google no pueden cambiar su correo desde el perfil.");
            }

            boolean takenByAnother = userRepository.findByEmail(newEmail)
                    .filter(other -> !other.getId().equals(user.getId()))
                    .isPresent();
            if (takenByAnother) {
                throw new EmailAlreadyExistsException(newEmail);
            }
        }

        user.setName(newName);
        user.setEmail(newEmail);

        // El JWT guarda el correo como subject: solo si cambió hace falta uno nuevo.
        // Emitirlo siempre alargaría la sesión 24 h cada vez que alguien edita su
        // nombre.
        String token = emailChanged
                ? jwtSecurity.generateToken(user.getEmail(), user.getRole().name())
                : null;

        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole());
    }

    /**
     * Cambia el nombre a mostrar de un usuario y devuelve su perfil actualizado.
     *
     * @param userId  id del usuario autenticado
     * @param request datos validados con el nuevo nombre
     * @return el perfil con el nombre ya actualizado
     * @throws RuntimeException si no existe un usuario con ese id
     */
    @Transactional
    public ProfileResponse updateDisplayName(Long userId, UpdateDisplayNameRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setName(request.name());
        userRepository.save(user);
        return getProfile(userId);
    }
}