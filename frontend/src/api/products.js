/*
 * Este archivo SOLO habla con el backend de productos (búsqueda, HU-06a, HU-06b y HT-13).
 * No toca el DOM ni decide qué se muestra en pantalla: eso vive en los componentes.
 */

const API_BASE = 'http://localhost:8080/api/products'; // TODO: usar el cliente API central cuando exista

/**
 * Busca productos activos con filtros. Todos son opcionales y se combinan en el backend.
 * Llega ordenado por mayor ahorro (DP-39): la interfaz no elige el orden (DP-36).
 * Sin ningún filtro, la pantalla debe usar findBestSavings en lugar de esta función.
 * @param {{name?: string, sectionId?: number|string|null, categoryId?: number|string|null,
 *          storeIds?: Array<number|string>, minPrice?: string, maxPrice?: string,
 *          page?: number, size?: number}} filters
 *        sectionId es el id de la categoría raíz; page empieza en 0; minPrice y maxPrice vacíos no filtran
 * @param {{signal?: AbortSignal}} options
 * @returns {Promise<{content: object[], page: {size: number, number: number,
 *          totalElements: number, totalPages: number}}>}
 * @throws {Error} con `status` (código HTTP, 0 si no hubo conexión) y el mensaje del backend
 */
export async function searchProducts(
    { name, sectionId, categoryId, storeIds = [], minPrice, maxPrice, page, size } = {},
    { signal } = {},
) {
    const query = new URLSearchParams();
    if (name) query.set('name', name);
    if (sectionId !== null && sectionId !== undefined) query.set('sectionId', sectionId);
    if (categoryId !== null && categoryId !== undefined) query.set('idCategory', categoryId);
    storeIds.forEach((storeId) => query.append('storeId', storeId));
    if (minPrice) query.set('minPrice', minPrice);
    if (maxPrice) query.set('maxPrice', maxPrice);
    if (page !== undefined) query.set('page', page);
    if (size !== undefined) query.set('size', size);
    query.set('sort', 'SAVINGS');

    return getJson(`${API_BASE}/search?${query.toString()}`, signal, 'Error al buscar productos');
}

/**
 * Los mejores ahorros de cada subcategoría: lo que muestra la búsqueda sin filtros (HT-13, DP-39).
 * @param {{sectionId?: number|string|null, limit?: number}} params limit va de 1 a 20 (5 por defecto)
 * @param {{signal?: AbortSignal}} options
 * @returns {Promise<Array<{sectionId: number, sectionName: string, categoryId: number,
 *          categoryName: string, products: Array<{id: number, name: string, minPrice: number,
 *          maxPrice: number, savingsPct: number, storeCount: number}>}>>}
 * @throws {Error} con `status` (código HTTP, 0 si no hubo conexión) y el mensaje del backend
 */
export async function findBestSavings({ sectionId, limit } = {}, { signal } = {}) {
    const query = new URLSearchParams();
    if (sectionId !== null && sectionId !== undefined) query.set('sectionId', sectionId);
    if (limit !== undefined) query.set('limit', limit);
    return getJson(`${API_BASE}/best-savings?${query.toString()}`, signal, 'Error al cargar los mejores ahorros');
}

// GET con el mismo manejo de errores para todas las funciones de este archivo
async function getJson(url, signal, fallbackMessage) {
    let response;
    try {
        response = await fetch(url, { signal });
    } catch (error) {
        // Un AbortError no es un fallo de conexión: quien llama lo ignora
        if (error.name === 'AbortError') throw error;
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
