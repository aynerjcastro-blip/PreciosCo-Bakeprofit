/*
 * auth.js (capa api)
 *
 * Este archivo SOLO se encarga de hablar con el backend.
 * No toca el DOM, no muestra ni oculta nada en pantalla.
 * Esa responsabilidad vive en js/ui/login.js y js/ui/registro.js.
 * 
 * Refactorizado para utilizar el cliente API centralizado (apiClient),
 * eliminando URLs hardcodeadas y duplicación de código fetch.
 */

import apiClient from './client'; // Importamos el cliente centralizado

/**
 * Envia las credenciales de login al backend.
 * @param {string} email
 * @param {string} password
 * @returns {Promise<object>} el AuthResponse del backend (token, name, email, role)
 * @throws {Error} si el backend responde con un error (credenciales invalidas, etc.)
 */
export async function login(email, password) {
    // apiClient.post ya serializa a JSON, añade headers y maneja errores automáticamente
    return await apiClient.post('/api/auth/login', { email, password });
}

/**
 * Envia los datos de un usuario nuevo al backend para registrarlo.
 * @param {object} userData - { name, email, password }
 * @returns {Promise<object>} el AuthResponse del backend
 * @throws {Error} si el backend responde con un error (email duplicado, etc.)
 */
export async function register(userData) {
    return await apiClient.post('/api/auth/register', userData);
}