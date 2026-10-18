package com.comprasco.bakeprofit.security;

import com.comprasco.bakeprofit.config.JwtSecurity;
import com.comprasco.bakeprofit.entity.User;
import com.comprasco.bakeprofit.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro de seguridad que valida el token JWT y verifica que la cuenta este activa en la base de datos por peticion.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtSecurity jwtSecurity;
    private final UserRepository userRepository;

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

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtSecurity.isTokenValid(token)) {
                String email = jwtSecurity.extractEmail(token);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // RN-10: Carga del usuario por peticion para revalidar el estado activo en la BD
                    User user = userRepository.findByEmail(email).orElse(null);

                    // Verificacion del campo active (si la propiedad en User es private Boolean active)
                    if (user != null && Boolean.TRUE.equals(user.getActive())) {

                        // Mapeo explicito del rol de la entidad a SimpleGrantedAuthority de Spring Security
                        List<SimpleGrantedAuthority> authorities = List.of(
                                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                        );

                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                authorities
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}