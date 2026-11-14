/*
 * Cliente HTTP compartido: un solo lugar para la URL base y el manejo de errores.
 * Así products.js, landing.js y los que vengan no repiten el mismo try/catch.
 */

// Vite lee VITE_API_URL del archivo .env; sin él usamos el backend local.
export const API_ORIGIN = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

/**
 * GET que devuelve el JSON o lanza un Error con `status` (0 si no hubo conexión).
 * @param {string} url URL completa
 * @param {{signal?: AbortSignal, fallbackMessage?: string}} options
 */
export async function getJson(url, { signal, fallbackMessage = 'Error en la petición' } = {}) {
    let response;
    try {
        response = await fetch(url, { signal });
    } catch (error) {
        if (error.name === 'AbortError') throw error; // cancelar no es un fallo
        const connectionError = new Error('No se pudo conectar con el servidor');
        connectionError.status = 0;
        throw connectionError;
    }

    if (!response.ok) {
        const body = await response.json().catch(() => null);
        const error = new Error(body?.message ?? fallbackMessage);
        error.status = response.status;
        throw error;
    }
    return response.json();
}