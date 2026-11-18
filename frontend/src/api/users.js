import { getToken } from '../utils/auth-helper';

const API_BASE = 'http://localhost:8080/api/users/me'; // TODO: usar el cliente API central cuando exista

/**
 * Petición al backend con el JWT de la sesión.
 * Lanza un Error con el mensaje del backend y `status` con el código HTTP
 * (0 cuando no hubo conexión), para que cada pantalla decida qué mostrar.
 *
 * @param {string} path - ruta relativa a API_BASE ('' para el perfil, '/password' para la contraseña)
 * @param {{method?: string, body?: object}} [options]
 * @returns {Promise<object|null>} cuerpo JSON de la respuesta, o null si fue 204
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
        const error = new Error('No se pudo conectar con el servidor. Intenta de nuevo en unos minutos.');
        error.status = 0;
        throw error;
    }

    // 204: la acción salió bien y no hay cuerpo (cambio de contraseña)
    if (response.status === 204) return null;

    let data = null;
    try {
        data = await response.json();
    } catch {
        // Respuesta sin cuerpo JSON: se usa el mensaje genérico
    }

    if (!response.ok) {
        const error = new Error(data?.message || 'No se pudo completar la acción.');
        error.status = response.status;
        throw error;
    }
    return data;
}

/**
 * Perfil del usuario autenticado: name, email, role y emailEditable.
 * `emailEditable` es true solo en cuentas locales (con contraseña propia).
 */
export function getMyProfile() {
    return request('');
}

/**
 * Cambia la contraseña del usuario autenticado.
 *
 * @param {string} currentPassword - contraseña vigente
 * @param {string} newPassword - contraseña nueva (mínimo 8 caracteres)
 * @throws {Error} 400 si la actual no coincide o la nueva es igual; 403 si la cuenta es de Google
 */
export function changePassword(currentPassword, newPassword) {
    return request('/password', { method: 'PUT', body: { currentPassword, newPassword } });
}