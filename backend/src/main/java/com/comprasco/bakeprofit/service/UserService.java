package com.comprasco.bakeprofit.service;

import com.comprasco.bakeprofit.config.JwtSecurity;
import com.comprasco.bakeprofit.dto.AuthResponse;
import com.comprasco.bakeprofit.dto.GoogleLoginRequest;
import com.comprasco.bakeprofit.dto.LoginRequest;
import com.comprasco.bakeprofit.dto.RegisterRequest;
import com.comprasco.bakeprofit.entity.Role;
import com.comprasco.bakeprofit.entity.User;
import com.comprasco.bakeprofit.exception.EmailAlreadyExistsException;
import com.comprasco.bakeprofit.exception.InactiveUserException;
import com.comprasco.bakeprofit.exception.InvalidCredentialsException;
import com.comprasco.bakeprofit.exception.UserNotFoundException;
import com.comprasco.bakeprofit.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
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
private static final Logger log = LoggerFactory.getLogger(UserService.class);
    @Value("${google.client-id}")
    private String googleClientId;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JwtSecurity jwtSecurity) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtSecurity = jwtSecurity;

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
            return userRepository.save(newUser);
        });

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new InactiveUserException("La cuenta se encuentra desactivada. Contacte al administrador.");
        }

        String token = jwtSecurity.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole());
    }
}