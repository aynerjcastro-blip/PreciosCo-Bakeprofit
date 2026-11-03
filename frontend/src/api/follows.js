/*
 * Este archivo SOLO habla con el backend de seguimiento (seguir productos y su aviso).
 * No toca el DOM ni decide qué se muestra en pantalla: eso vive en los componentes.
 *
 * Conceptos usados:
 * - fetch() con los métodos GET, POST, PUT y DELETE.
 * - Header Authorization: todas las rutas de seguimiento exigen sesión (JWT como "Bearer <token>").
 * - Un seguimiento es de un usuario y un producto; el precio objetivo es opcional
 *   (null = avisar ante cualquier reducción).
 */

import { getToken } from '../utils/auth-helper';

const API_BASE = 'http://localhost:8080/api/follows'; // TODO: usar el cliente API central cuando exista

/**
 * Petición base: agrega el token y normaliza los errores.
 * @param {string} path - ruta relativa a /api/follows
 * @param {{method?: string, body?: object}} options
 * @returns {Promise<object|null>} el JSON de la respuesta, o null en un 204
 * @throws {Error} con `status` (código HTTP, 0 si no hubo conexión) y el mensaje del backend
 */
async function request(path, { method = 'GET', body } = {}) {
    const headers = {};
    if (body) headers['Content-Type'] = 'application/json';
    const token = getToken();
    if (token) headers.Authorization = `Bearer ${token}`;

    let response;
    try {
        response = await fetch(`${API_BASE}${path}`, {
            method,
            headers,
            body: body ? JSON.stringify(body) : undefined,
        });
    } catch {
        const error = new Error('No se pudo conectar con el servidor');
        error.status = 0;
        throw error;
    }

    // 204: la acción salió bien y no hay cuerpo (dejar de seguir, "no sigues este producto")
    if (response.status === 204) return null;

    let data = null;
    try {
        data = await response.json();
    } catch {
        // Respuesta sin cuerpo JSON (por ejemplo un 403 vacío): se usa el mensaje genérico
    }

    if (!response.ok) {
        const error = new Error(data?.message || 'No se pudo completar la acción');
        error.status = response.status;
        throw error;
    }
    return data;
}

/**
 * Mis seguimientos, del más reciente al más antiguo.
 * @returns {Promise<object[]>} [{id, productId, productName, targetPrice, notificationActive, lowestPrice, registrationDate}]
 */
export function getFollows() {
    return request('');
}

/**
 * Mi seguimiento de un producto.
 * @returns {Promise<object|null>} el seguimiento, o null si no sigo ese producto (204)
 */
export function getMyFollow(productId) {
    return request(`/product/${productId}`);
}

/**
 * Sigue un producto. Solo rol USER.
 * @param {{productId: number, targetPrice?: number|null}} payload
 * @returns {Promise<object>} el seguimiento creado
 */
export function createFollow(payload) {
    return request('', { method: 'POST', body: payload });
}

/**
 * Cambia el precio objetivo y/o activa o desactiva el aviso. Reemplaza ambos valores.
 * @param {number} followId
 * @param {{notificationActive: boolean, targetPrice: number|null}} payload
 * @returns {Promise<object>} el seguimiento actualizado
 */
export function updateFollow(followId, payload) {
    return request(`/${followId}`, { method: 'PUT', body: payload });
}

/**
 * Deja de seguir el producto.
 * @param {number} followId
 * @returns {Promise<null>}
 */
export function deleteFollow(followId) {
    return request(`/${followId}`, { method: 'DELETE' });
}