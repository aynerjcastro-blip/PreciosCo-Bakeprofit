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
    public User register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyExistsException(request.email()); // ← 409 CONFLICT
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);

        return userRepository.save(user);
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