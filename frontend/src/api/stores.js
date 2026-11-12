/*
 * Este archivo SOLO habla con el backend de tiendas.
 * No toca el DOM ni decide qué se muestra en pantalla: eso vive en los componentes.
 */

const API_BASE = 'http://localhost:8080/api/stores'; // TODO: usar el cliente API central cuando exista

/**
 * Tiendas activas, las únicas que se pueden usar como filtro de la búsqueda (HU-06b).
 * @returns {Promise<Array<{id: number, name: string, category: string}>>}
 * @throws {Error} con `status` (código HTTP, 0 si no hubo conexión)
 */
export async function findActiveStores() {
    let response;
    try {
        response = await fetch(`${API_BASE}/active`);
    } catch {
        const error = new Error('No se pudo conectar con el servidor');
        error.status = 0;
        throw error;
    }

    if (!response.ok) {
        const error = new Error('Error al consultar las tiendas');
        error.status = response.status;
        throw error;
    }
    return response.json();
}
