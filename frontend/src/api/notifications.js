/*
 * Este archivo SOLO habla con el backend de avisos (baja de precio de lo que sigo).
 * No toca el DOM ni decide qué se muestra en pantalla: eso vive en los componentes.
 *
 * Conceptos usados:
 * - fetch() con los métodos GET y PATCH.
 * - Header Authorization: las rutas de avisos exigen sesión con rol USER (JWT como "Bearer <token>").
 * - GET devuelve los avisos más recientes junto con la cantidad TOTAL de no leídos.
 */

import { getToken } from '../utils/auth-helper';

const API_BASE = 'http://localhost:8080/api/notifications'; // TODO: usar el cliente API central cuando exista

/**
 * Petición base: agrega el token y normaliza los errores.
 * @param {string} path - ruta relativa a /api/notifications
 * @param {{method?: string}} options
 * @returns {Promise<object|null>} el JSON de la respuesta, o null en un 204
 * @throws {Error} con `status` (código HTTP, 0 si no hubo conexión) y el mensaje del backend
 */
async function request(path, { method = 'GET' } = {}) {
    const headers = {};
    const token = getToken();
    if (token) headers.Authorization = `Bearer ${token}`;

    let response;
    try {
        response = await fetch(`${API_BASE}${path}`, { method, headers });
    } catch {
        const error = new Error('No se pudo conectar con el servidor');
        error.status = 0;
        throw error;
    }

    // 204: la acción salió bien y no hay cuerpo (marcar como leído)
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
 * Mis avisos (hasta 50, del más reciente al más antiguo) y cuántos están sin leer.
 * @returns {Promise<{unreadCount: number, notifications: object[]}>}
 *   cada aviso: {id, productId, productName, storeId, storeName, previousPrice, newPrice, read, createdAt}
 */
export function getNotifications() {
    return request('');
}

/**
 * Marca un aviso como leído. Repetirlo también responde bien.
 * @param {number} notificationId
 * @returns {Promise<null>}
 */
export function markNotificationRead(notificationId) {
    return request(`/${notificationId}/read`, { method: 'PATCH' });
}