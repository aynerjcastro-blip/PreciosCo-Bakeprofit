package com.comprasco.bakeprofit.security;

import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import com.comprasco.bakeprofit.config.JwtSecurity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro que autentica una petición a partir de un token JWT.
 *
 * <p>
 * Si el header {@code Authorization} contiene un token válido, establece
 * la identidad del usuario en {@link SecurityContextHolder}. No autoriza
 * rutas: eso lo hace la configuración de Spring Security.
 * </p>
 *
 * <p>
 * Un token ausente o inválido no corta la cadena; la petición continúa
 * sin autenticación y las reglas de autorización deciden el resultado.
 * </p>
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtSecurity jwt;

    public JwtAuthFilter(JwtSecurity jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());

            if (jwt.isTokenValid(token)) {
                var authentication = new UsernamePasswordAuthenticationToken(
                        jwt.extractEmail(token),
                        null,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_" + jwt.extractRole(token))));

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}