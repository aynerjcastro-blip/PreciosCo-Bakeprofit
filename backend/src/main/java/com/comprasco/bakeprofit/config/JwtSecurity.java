package com.comprasco.bakeprofit.config;

import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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
        return Jwts.builder() // Inicia la construccion del token
                .subject(email). // Quien es el token en JWT en este caso el email
                claim("role", role). // Agrega un claim personalizado. para el payload del JWT
                issuedAt(new Date())// Fecha y hora de la creacion del token
                .expiration(new Date(System.currentTimeMillis() + expiration)). // Fecha y hora de vencimiento suma de
                                                                                // jwt.expiration en config local
                signWith(key).// Firma el token con la jwt.secret, garantiza que si alguien modifica el
                              // payload la firma no coincide y el back lo rechaza
                compact();// Compacta el string con formato header.payload.signature.
    }

    /**
     ** Genera la clave criptográfica secreta a partir del secreto configurado en la
     ** aplicación.
     ** <p>
     ** El flujo que sigue este método es:
     ** </p>
     ** <ul>
     ** <li>La variable {@code secret} es un String definido en los atributos de la
     ** clase, cuyo valor se inyecta desde el archivo de propiedades.</li>
     ** <li>Este método toma esa cadena de texto y la convierte en un arreglo de
     ** bytes utilizando la codificación estándar UTF-8.</li>
     ** <li>Dado que los algoritmos de encriptación no procesan texto plano
     ** directamente, es obligatorio realizar esta conversión a bytes puros.</li>
     ** <li>{@link Keys#hmacShaKeyFor} procesa estos bytes para generar una clave
     ** segura bajo el algoritmo HMAC-SHA.</li>
     ** </ul>
     * 
     * @return Una instancia de {@link SecretKey} utilizada para la firma (fase de
     *         creación) y verificación (fase de parsing) de los tokens JWT.
     * @throws io.jsonwebtoken.security.WeakKeyException
     **                                                   Si el tamaño de la cadena
     *                                                   {@code secret} es inferior
     *                                                   a 32 caracteres (256 bits).
     */
    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     ** Extrae, valida y decodifica el cuerpo (Claims) de un JSON Web Token (JWT).
     ** 
     ** <p>
     ** Este método realiza el proceso completo de seguridad sobre un token recibido:
     ** </p>
     ** <ul>
     ** <li><b>Inicialización:</b> Activa el constructor analizador (parser) de la
     ** librería JJWT.</li>
     ** <li><b>Configuración de seguridad:</b> Proporciona la clave criptográfica
     ** {@code key()}. El parser la usará para garantizar que el token no haya sido
     ** alterado ni falsificado.</li>
     ** <li><b>Construcción:</b> Instancia el objeto analizador con la configuración
     ** de la clave ya aplicada.</li>
     ** <li><b>Procesamiento y Validación:</b> Toma la cadena del token enviada por
     ** el cliente, verifica criptográficamente su firma y decodifica el contenido en
     ** Base64. Si el token expiró o es inválido, el flujo se interrumpe lanzando una
     ** excepción.</li>
     ** <li><b>Extracción:</b> Si la firma es legítima, obtiene el payload (cuerpo)
     ** del token con los datos del usuario.</li>
     ** </ul>
     * 
     * @param token Cadena de texto comprimida en formato JWT enviada normalmente en
     *              el encabezado de autorización.
     * @return Un objeto {@link Claims} que actúa como un mapa con la información
     *         del usuario (sujeto, roles, expiración, etc.).
     * @throws io.jsonwebtoken.JwtException Si el token es inválido, la firma no
     *                                      coincide o ha expirado.
     */
    private Claims parseClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     ** Verifica de forma segura si un JSON Web Token (JWT) es
     ** auténtico,estructuralmente correcto y vigente.
     * 
     ** <p>
     ** Este método intenta delegar la decodificación del token a
     * {@link #parseClaims(String)}.
     ** Si el proceso es exitoso, se asume que el token es totalmente seguro y está
     ** listo para usarse.
     * </p>
     * 
     * @param token Cadena de texto que representa el JWT enviado por el cliente.
     * @return {@code true} si el token posee una firma legítima, no ha expirado y
     *         su formato es válido;
     *         {@code false} en caso de estar alterado, vencido, mal estructurado o
     *         si es nulo/vacío.
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     ** Extrae el correo electrónico (identificador principal del usuario) almacenado
     ** en el token.
     ** <p>
     ** Por convención en arquitecturas de autenticación, el identificador único del
     ** usuario
     ** (en este caso, su email) se almacena dentro del claim estándar conocido como
     ** <b>Subject</b> ({@code sub}).
     ** </p>
     * 
     * @param token Cadena de texto que representa el JWT del cual se extraerá la
     *              información.
     * @return El correo electrónico del usuario codificado en el token.
     * @throws io.jsonwebtoken.JwtException Si el token es inválido o no se puede
     *                                      procesar adecuadamente.
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     ** Extrae el rol o nivel de acceso personalizado asignado al usuario dentro del
     ** token.
     * 
     ** <p>
     ** A diferencia del Subject, el rol es un <b>Claim privado/personalizado</b>.
     ** Este método
     ** busca la clave {@code "role"} dentro del payload y realiza un casteo seguro
     ** hacia el tipo {@link String}.
     ** </p>
     * 
     * @param token Cadena de texto que representa el JWT del cual se extraerá el
     *              rol.
     * @return Una cadena de texto con el nombre del rol asignado (por ejemplo,
     *         {@code "ROLE_ADMIN"} o {@code "ROLE_USER"}).
     * @throws io.jsonwebtoken.JwtException Si el token es inválido o la clave
     *                                      especificada no se encuentra o no
     *                                      coincide con el tipo esperado.
     */
    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

}
