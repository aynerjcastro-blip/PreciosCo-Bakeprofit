package com.comprasco.bakeprofit.service;

import com.comprasco.bakeprofit.config.JwtSecurity;
import com.comprasco.bakeprofit.dto.AuthResponse;
import com.comprasco.bakeprofit.dto.LoginRequest;
import com.comprasco.bakeprofit.dto.RegisterRequest;
import com.comprasco.bakeprofit.entity.Role;
import com.comprasco.bakeprofit.entity.User;
import com.comprasco.bakeprofit.exception.EmailAlreadyExistsException;
import com.comprasco.bakeprofit.exception.InvalidCredentialsException;
import com.comprasco.bakeprofit.exception.UserNotFoundException;
import com.comprasco.bakeprofit.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtSecurity jwtSecurity;

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

        // Verifica que el email no este ya en uso antes de intentar guardar.
        // Se hace aqui, no se deja que la base de datos falle sola, para
        // poder devolver un mensaje de error claro (409) en vez de una
        // excepcion generica de base de datos.
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyExistsException(request.email()); // -> 409 CONFLICT
        }

        // Construye la entidad User a partir del DTO. No se copia "role"
        // del request: el rol siempre se fija aqui mismo como USER, nunca
        // lo decide el cliente, para que nadie pueda auto-asignarse ADMIN
        // enviando ese campo en el JSON.
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password())); // nunca texto plano
        user.setRole(Role.USER);

        // Guarda el usuario en la base de datos. savedUser trae el id
        // generado por la base de datos, aunque aqui no se use directamente.
        User savedUser = userRepository.save(user);

        // Genera el JWT para el usuario recien creado, usando email y rol
        // como los dos datos que el token necesita llevar en su payload.
        String token = jwtSecurity.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        // Devuelve el DTO de salida (no la entidad User completa), para
        // no exponer el password hasheado ni otros campos internos.
        return new AuthResponse(token, savedUser.getName(), savedUser.getEmail(), savedUser.getRole());
    }

    /**
     * Login seguro: no filtra si falló email o password.
     * Siempre lanza la misma excepción para no dar pistas.
     */
    public User verifyCredentials(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return user; // Devuelve el usuario si todo OK
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

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException());
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtSecurity.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole());
    }
}