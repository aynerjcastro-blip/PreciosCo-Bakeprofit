import { API_ORIGIN, getJson } from './http';

/*
 * Solo habla con el backend de la landing. No decide qué se muestra.
 */

/**
 * Cifras reales de la plataforma.
 * @param {{signal?: AbortSignal}} options
 * @returns {Promise<{totalProducts: number, totalStores: number, lastUpdate: string|null,
 *          sections: Array<{id: number, name: string, productCount: number, storeCount: number}>}>}
 * @throws {Error} con `status` (código HTTP, 0 si no hubo conexión)
 */
export function getLandingStats({ signal } = {}) {
    return getJson(`${API_ORIGIN}/api/landing/stats`, {
        signal,
        fallbackMessage: 'Error al cargar la información de inicio',
    });
}