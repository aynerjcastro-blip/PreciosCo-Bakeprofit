/*
 * Este archivo SOLO habla con el backend de productos (búsqueda, HU-06a y HU-06b).
 * No toca el DOM ni decide qué se muestra en pantalla: eso vive en los componentes.
 */

const API_BASE = 'http://localhost:8080/api/products'; // TODO: usar el cliente API central cuando exista

/**
 * Busca productos activos. Todos los filtros son opcionales y se combinan en el backend.
 * @param {{name?: string, categoryId?: number|string|null, storeIds?: Array<number|string>,
 *          minPrice?: string, maxPrice?: string, page?: number, size?: number}} filters
 *        page empieza en 0; minPrice y maxPrice vacíos no filtran
 * @param {{signal?: AbortSignal}} options
 * @returns {Promise<{content: object[], page: {size: number, number: number,
 *          totalElements: number, totalPages: number}}>}
 * @throws {Error} con `status` (código HTTP, 0 si no hubo conexión) y el mensaje del backend
 */
export async function searchProducts(
    { name, categoryId, storeIds = [], minPrice, maxPrice, page, size } = {},
    { signal } = {},
) {
    const query = new URLSearchParams();
    if (name) query.set('name', name);
    if (categoryId !== null && categoryId !== undefined) query.set('idCategory', categoryId);
    storeIds.forEach((storeId) => query.append('storeId', storeId));
    if (minPrice) query.set('minPrice', minPrice);
    if (maxPrice) query.set('maxPrice', maxPrice);
    if (page !== undefined) query.set('page', page);
    if (size !== undefined) query.set('size', size);

    let response;
    try {
        response = await fetch(`${API_BASE}/search?${query.toString()}`, { signal });
    } catch (error) {
        // Un AbortError no es un fallo de conexión: quien llama lo ignora
        if (error.name === 'AbortError') throw error;
        const connectionError = new Error('No se pudo conectar con el servidor');
        connectionError.status = 0;
        throw connectionError;
    }

    if (!response.ok) {
        const body = await response.json().catch(() => null);
        const error = new Error(body?.message ?? 'Error al buscar productos');
        error.status = response.status;
        throw error;
    }
    return response.json();
}
