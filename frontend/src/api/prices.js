/*
 * Este archivo SOLO habla con el backend de precios (comparador).
 * No toca el DOM ni decide qué se muestra en pantalla: eso vive en los componentes.
 */

const API_BASE = 'http://localhost:8080/api/prices'; // TODO: usar el cliente API central cuando exista

/**
 * Precios vigentes de un producto en cada tienda activa, de menor a mayor.
 * @param {number|string} productId
 * @returns {Promise<object[]>} [{productName, storeId, storeName, value, source,
 *          registrationDate, productUrl, imageUrl, lastSeenAt}]
 * @throws {Error} con `status` (código HTTP, 0 si no hubo conexión)
 */
export async function compareByProduct(productId) {
    const query = new URLSearchParams({ productId }).toString();

    let response;
    try {
        response = await fetch(`${API_BASE}/compare?${query}`);
    } catch {
        const error = new Error('No se pudo conectar con el servidor');
        error.status = 0;
        throw error;
    }

    if (!response.ok) {
        const error = new Error('Error al consultar precios');
        error.status = response.status;
        throw error;
    }
    return response.json();
}
