/*
 * Este archivo SOLO se encarga de hablar con el backend.
 * No toca el DOM ni muestra nada en pantalla: de eso se encargan los
 * componentes de React (AuthForm, ForgotPassword, ResetPassword).
 */

const API_BASE = "http://localhost:8080/api/auth";

/**
 * Lee el cuerpo de la respuesta sin importar si es JSON o texto plano.
 * ¿Por qué? Los errores llegan como JSON (GlobalExceptionHandler), pero algunos
 * exitos, como el de reset-password, llegan como texto plano.
 * @param {Response} response
 * @returns {Promise<object>} el JSON, o { message: texto } si no era JSON
 */
async function readBody(response) {
    const text = await response.text();
    try {
        return JSON.parse(text);
    } catch {
        return { message: text };
    }
}

/**
 * Hace un POST con cuerpo JSON a un endpoint de /api/auth.
 * @param {string} path - ruta relativa a API_BASE, por ejemplo "/login"
 * @param {object} body - datos a enviar
 * @param {string} fallbackMessage - mensaje a mostrar si el backend no manda uno
 * @returns {Promise<object>} el cuerpo de la respuesta
 * @throws {Error} si no hay conexion o si el backend responde con un error
 */
async function postJson(path, body, fallbackMessage) {
    let response;
    try {
        response = await fetch(`${API_BASE}${path}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body),
        });
    } catch {
        // fetch solo rechaza por fallo de red, no por respuestas 4xx o 5xx
        throw new Error("No se pudo conectar con el servidor. Intenta de nuevo en unos minutos.");
    }

    const data = await readBody(response);

    if (!response.ok) {
        // El backend ya manda un mensaje claro gracias al GlobalExceptionHandler,
        // asi que lo reusamos en vez de inventar uno generico aqui.
        throw new Error(data.message || fallbackMessage);
    }

    return data;
}

/**
 * Envia las credenciales de login al backend.
 * @param {string} email
 * @param {string} password
 * @returns {Promise<object>} el AuthResponse del backend (token, name, email, role)
 * @throws {Error} si el backend responde con un error (credenciales invalidas, etc.)
 */
export function login(email, password) {
    return postJson("/login", { email, password }, "No se pudo iniciar sesion");
}

/**
 * Envia los datos de un usuario nuevo al backend para registrarlo.
 * @param {object} userData - { name, email, password }
 * @returns {Promise<object>} el AuthResponse del backend
 * @throws {Error} si el backend responde con un error (email duplicado, etc.)
 */
export function register(userData) {
    return postJson("/register", userData, "No se pudo completar el registro");
}

/**
 * Envia el ID token de Google al backend para iniciar sesion o registrarse.
 * @param {string} credential - el ID token JWT devuelto por Google
 * @returns {Promise<object>} el AuthResponse del backend con el JWT de la app
 * @throws {Error} si el token no es valido o la cuenta esta desactivada
 */
export function googleLogin(credential) {
    return postJson("/google", { credential }, "No se pudo iniciar sesión con Google");
}

/**
 * Solicita el correo con el enlace para restablecer la contrasena.
 * El backend debe responder igual exista o no el correo, para no revelar
 * quien esta registrado.
 * @param {string} email
 * @returns {Promise<object>} { message } con el texto de confirmacion
 * @throws {Error} si falla la conexion o el envio del correo
 */
export function forgotPassword(email) {
    return postJson("/forgot-password", { email }, "No se pudo enviar el enlace. Intenta de nuevo.");
}

/**
 * Cambia la contrasena usando el token que llego por correo.
 * @param {string} token - el valor de ?token= del enlace
 * @param {string} newPassword
 * @returns {Promise<object>} { message } con el texto de confirmacion
 * @throws {Error} si el token es invalido o vencio
 */
export function resetPassword(token, newPassword) {
    return postJson("/reset-password", { token, newPassword }, "No se pudo restablecer la contraseña.");
}