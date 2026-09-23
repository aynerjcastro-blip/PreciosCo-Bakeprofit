/*
 * auth.js (capa api)
 *
 * Este archivo SOLO se encarga de hablar con el backend.
 * No toca el DOM, no muestra ni oculta nada en pantalla.
 * Esa responsabilidad vive en js/ui/login.js y js/ui/registro.js.
 *
 * Conceptos usados:
 * - fetch(): funcion nativa del navegador para hacer peticiones HTTP.
 * - async/await: forma de escribir codigo asincrono que se lee como
 *   si fuera secuencial, en vez de usar .then() encadenados.
 * - JSON.stringify: convierte el objeto JS a texto JSON antes de
 *   enviarlo en el body de la peticion.
 */


/**
 * Envia las credenciales de login al backend.
 * @param {string} email
 * @param {string} password
 * @returns {Promise<object>} el AuthResponse del backend (token, name, email, role)
 * @throws {Error} si el backend responde con un error (credenciales invalidas, etc.)
 */
export async function login(email, password) {
    const response = await fetch('${API_BASE}/login', {
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
    const response = await fetch('${API_BASE}/register', {
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