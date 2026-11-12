package com.comprasco.bakeprofit.auth.security;

import com.comprasco.bakeprofit.auth.entity.User;
import com.comprasco.bakeprofit.auth.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

/**
 * Filtro de seguridad que valida el token JWT y verifica que la cuenta este
 * activa en la base de datos por peticion.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtSecurity jwtSecurity;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    /**
     * Inyeccion de dependencias por constructor.
     */
    public JwtAuthFilter(JwtSecurity jwtSecurity, UserRepository userRepository) {
        this.jwtSecurity = jwtSecurity;
        this.userRepository = userRepository;
    }

    @Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    // Si algo no cuadra (sin token, token inválido, usuario inexistente o inactivo) no se
    // autentica y el entry point de SecurityConfig responde 401 en las rutas protegidas
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);

        if (jwtSecurity.isTokenValid(token)) {
            String email = jwtSecurity.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userRepository.findByEmail(email).orElse(null);

                if (user != null) {
                    if (Boolean.TRUE.equals(user.getActive())) {
                        List<SimpleGrantedAuthority> authorities = List.of(
                                new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                user, null, authorities);
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } else {
                        // Solo el id: el correo no debe quedar en los logs
                        log.debug("Acceso bloqueado: cuenta inactiva (id={})", user.getId());
                    }
                }
            }
        }
    }

    filterChain.doFilter(request, response);
}
}