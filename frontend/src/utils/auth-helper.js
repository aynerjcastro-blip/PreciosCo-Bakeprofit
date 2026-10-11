/*
 * auth-helper.js
 *
 * Este archivo centraliza todo lo relacionado con el manejo del JWT
 * en el navegador. Ninguna otra parte del codigo deberia leer o escribir
 * el token directo en localStorage, siempre a traves de estas funciones.
 *
 * Conceptos usados:
 * - localStorage: guarda datos en el navegador que persisten aunque
 *   se cierre la pestana o el navegador (a diferencia de sessionStorage).
 * - JSON.stringify / JSON.parse: convierten un objeto de JS a texto y
 *   viceversa, porque localStorage solo guarda strings.
 */

const TOKEN_KEY = "preciosco_token";
const USER_KEY = "preciosco_user";

/**
 * Guarda el token JWT y los datos basicos del usuario despues de un
 * login o registro exitoso.
 * @param {string} token - el JWT devuelto por el backend
 * @param {object} user - datos del usuario (name, email, role)
 */
export function saveSession(token, user) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
}

/**
 * Obtiene el token guardado, o null si no hay sesion activa.
 * @returns {string|null}
 */
export function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

/**
 * Obtiene los datos del usuario guardados, o null si no hay sesion.
 * @returns {object|null}
 */
export function getUser() {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
}

/**
 * Indica si el usuario tiene una sesion activa.
 * @returns {boolean}
 */
export function isLoggedIn() {
    return getToken() !== null;
}

/**
 * Elimina el token y los datos del usuario (logout).
 */
export function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
}

/**
 * Arma el header de autorizacion listo para usar en fetch().
 * Ejemplo de uso:
 *   fetch(url, { headers: { ...authHeader() } })
 * Si no hay token, devuelve un objeto vacio para no romper la peticion.
 * @returns {object}
 */
export function authHeader() {
    const token = getToken();
    return token ? { Authorization: "Bearer " + token } : {};
}