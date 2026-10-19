/*
 * Este archivo SOLO habla con el backend de opiniones (calificar y comentar).
 * No toca el DOM ni decide qué se muestra en pantalla: eso vive en los componentes.
 *
 * Conceptos usados:
 * - fetch() con los métodos GET, POST, PUT y DELETE.
 * - URLSearchParams: arma el query string (?productId=1&storeId=2) sin concatenar a mano.
 * - Header Authorization: el JWT viaja como "Bearer <token>" en las rutas que exigen sesión.
 * - El id de una opinión es el de su calificación; es el que usan editar, eliminar y reportar.
 */

import { getToken } from '../utils/auth-helper';

const API_BASE = 'http://localhost:8080/api/reviews'; // TODO: usar el cliente API central cuando exista

/**
 * Arma "?a=1&b=2" ignorando los valores vacíos.
 * @param {object} params
 * @returns {string}
 */
function buildQuery(params) {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null) query.append(key, value);
    });
    const text = query.toString();
    return text ? `?${text}` : '';
}

/**
 * Petición base: agrega el token si la ruta lo exige y normaliza los errores.
 * @param {string} path - ruta relativa a /api/reviews
 * @param {{method?: string, body?: object, auth?: boolean}} options
 * @returns {Promise<object|null>} el JSON de la respuesta, o null en un 204
 * @throws {Error} con `status` (código HTTP, 0 si no hubo conexión) y el mensaje del backend
 */
async function request(path, { method = 'GET', body, auth = false } = {}) {
    const headers = {};
    if (body) headers['Content-Type'] = 'application/json';
    if (auth) {
        const token = getToken();
        if (token) headers.Authorization = `Bearer ${token}`;
    }

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

    // 204: la acción salió bien y no hay cuerpo (reportar, eliminar, "mi opinión" vacía)
    if (response.status === 204) return null;

    let data = null;
    try {
        data = await response.json();
    } catch {
        // Respuesta sin cuerpo JSON (por ejemplo un 403 vacío): se usa el mensaje genérico
    }

    if (!response.ok) {
        // El backend ya manda un mensaje claro gracias al GlobalExceptionHandler
        const error = new Error(data?.message || 'No se pudo completar la acción');
        error.status = response.status;
        throw error;
    }
    return data;
}

/**
 * Promedio y cantidad de calificaciones. Pública.
 * Con productId y storeId: el producto en esa tienda. Solo productId: en todas las tiendas.
 * @param {{productId?: number|string, storeId?: number|string}} filters
 * @returns {Promise<{averageScore: number|null, ratingCount: number}>}
 */
export function getSummary({ productId, storeId }) {
    return request(`/summary${buildQuery({ productId, storeId })}`);
}

/**
 * Opiniones con comentario visible, de la más reciente a la más antigua. Pública.
 * @param {{productId?: number|string, storeId?: number|string, page?: number, size?: number}} options
 * @returns {Promise<{content: object[], page: {totalElements: number}}>} una página de resultados
 */
export function getReviews({ productId, storeId, page = 0, size = 10 }) {
    return request(buildQuery({ productId, storeId, page, size }));
}

/**
 * Mi opinión sobre un producto en una tienda. Exige sesión.
 * @returns {Promise<object|null>} la opinión, o null si aún no he opinado (204)
 */
export function getMyReview(productId, storeId) {
    return request(`/me${buildQuery({ productId, storeId })}`, { auth: true });
}

/**
 * Califica y comenta en un solo paso. Exige sesión con rol USER.
 * @param {{productId: number, storeId: number, score: number, text: string}} payload
 * @returns {Promise<object>} la opinión creada
 */
export function createReview(payload) {
    return request('', { method: 'POST', body: payload, auth: true });
}

/**
 * Edita calificación y comentario juntos (dentro del plazo de edición).
 * @param {number} reviewId
 * @param {{score: number, text: string}} payload
 * @returns {Promise<object>} la opinión actualizada
 */
export function updateReview(reviewId, payload) {
    return request(`/${reviewId}`, { method: 'PUT', body: payload, auth: true });
}

/**
 * Elimina el comentario (borrado lógico). La calificación se conserva.
 * @param {number} reviewId
 * @returns {Promise<null>}
 */
export function deleteReviewComment(reviewId) {
    return request(`/${reviewId}/comment`, { method: 'DELETE', auth: true });
}

/**
 * Reporta el comentario de una opinión.
 * @param {number} reviewId
 * @param {{reason: string, details?: string}} payload - reason: SPAM, LENGUAJE_OFENSIVO, INFORMACION_FALSA u OTRO
 * @returns {Promise<null>}
 */
export function reportReview(reviewId, payload) {
    return request(`/${reviewId}/report`, { method: 'POST', body: payload, auth: true });
}