package com.comprasco.bakeprofit.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Servicio para generar y validar tokens JWT.
 *
 * <p>
 * El token guarda el email como {@code subject} y el rol en el claim
 * personalizado {@code role}, sin el prefijo {@code ROLE_}.
 * </p>
 */
@Component
public class JwtSecurity {

    private final SecretKey key;
    private final long expiration;

    /**
     * @param secret     clave de firma definida en {@code jwt.secret}
     * @param expiration vigencia del token en milisegundos
     * @throws WeakKeyException si la clave tiene menos de 256 bits
     */
    public JwtSecurity(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * Genera un token JWT firmado para un usuario.
     *
     * @param email email del usuario
     * @param role  rol sin el prefijo {@code ROLE_}
     * @return token JWT compacto
     */
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    /**
     * Valida la firma, el formato y la vigencia del token.
     *
     * @param token JWT sin el prefijo {@code Bearer }
     * @return {@code true} si el token es válido
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}