/*

 * Este archivo SOLO se encarga de hablar con el backend.
 * No toca el DOM, no muestra ni oculta nada en pantalla.
 * Esa responsabilidad vive en js/ui/login.js y js/ui/registro.js.
 *
 */

/**
 * Envia las credenciales de login al backend.
 * @param {string} email
 * @param {string} password
 * @returns {Promise<object>} el AuthResponse del backend (token, name, email, role)
 * @throws {Error} si el backend responde con un error (credenciales invalidas, etc.)
 */
const API_BASE = "http://localhost:8080/api/auth";
export async function login(email, password) {
    const response = await fetch(`${API_BASE}/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
    });

    const data = await response.json();

    if (!response.ok) {
        // El backend ya manda un mensaje claro gracias al GlobalExceptionHandler,
        // asi que lo reusamos en vez de inventar uno generico aqui.
        throw new Error(data.message || "No se pudo iniciar sesion");
    }

    return data;
}

/**
 * Envia los datos de un usuario nuevo al backend para registrarlo.
 * @param {object} userData - { name, email, password }
 * @returns {Promise<object>} el AuthResponse del backend
 * @throws {Error} si el backend responde con un error (email duplicado, etc.)
 */
export async function register(userData) {
    const response = await fetch(`${API_BASE}/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(userData),
    });

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.message || "No se pudo completar el registro");
    }

    return data;
}

/**
 * Envía el ID token de Google al backend para iniciar sesión o registrarse.
 * @param {string} credential - El ID token JWT devuelto por Google.
 * @returns {Promise<object>} Datos del usuario autenticado y el JWT de la app.
 */

export async function googleLogin(credential) {
    const response = await fetch('http://localhost:8080/api/auth/google', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ credential }),
    });

    if(!response){
        const errorData = response.json().catch(()=>({}));
        throw new Error(errorData.message || 'No se pudo autenticar con Google')
    }

    return response.json();
}


