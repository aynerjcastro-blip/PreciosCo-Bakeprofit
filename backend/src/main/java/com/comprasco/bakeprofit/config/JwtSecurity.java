package com.comprasco.bakeprofit.config;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;

@Component
public class JwtSecurity {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * *Esta clase convierte el string secreto en un array de bytes usando UTF-8
     * *Y luego en una SecretKey compatible con HMAC-SHA (algoritmo de la firma)
     * 
     * @param email
     * @param role
     * @return Jwts.builder la construccion del token JWT
     */
    public String generateToken(String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder() //Inicia la construccion del token
        .subject(email).    //Quien es el token en JWT en este caso el email 
        claim("role", role). //Agrega un claim personalizado. para el payload del JWT
        issuedAt(new Date())//Fecha y hora de la creacion del token
        .expiration(new Date(System.currentTimeMillis() + expiration)).  //Fecha y hora de vencimiento suma de jwt.expiration en config local
        signWith(key).//Firma el token con la jwt.secret, garantiza que si alguien modifica el payload la firma no coincide y el back lo rechaza
        compact();// Compacta el string con formato header.payload.signature.
    }
}
