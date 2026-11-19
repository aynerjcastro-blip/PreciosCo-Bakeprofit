/*
 * Mapeo nombre de tienda → ruta del logo en /public/assets/stores/.
 *
 * Las claves están normalizadas (minúsculas, sin tildes, sin espacios extra) para
 * comparar con el nombre que llega del backend. Si una tienda no tiene logo,
 * el componente muestra la inicial como fallback.
 *
 * Para agregar un logo nuevo:
 *  1. Pon el .png en frontend/public/assets/stores/
 *  2. Agrega la entrada aquí con la clave normalizada
 */

const STORE_LOGOS = {
    d1: '/assets/stores/d1.png',
    exito: '/assets/stores/exito.png',
    olimpica: '/assets/stores/olimpica.png',
    jumbo: '/assets/stores/jumbo.png',
    'la rebaja': '/assets/stores/larebaja.png',
    larebaja: '/assets/stores/larebaja.png',
    farmanorte: '/assets/stores/farmanorte.png',
    farmatodo: '/assets/stores/farmatodo.png',
    bellapiel: '/assets/stores/bellapiel.png',
    cutis: '/assets/stores/cutis.png',
    medipiel: '/assets/stores/medipiel.png',
};

/**
 * Normaliza un nombre para buscar en el mapa: quita tildes, baja a minúsculas, trim.
 */
function normalize(name) {
    return name
        .normalize('NFD')
        .replace(/[̀-ͯ]/g, '')
        .toLowerCase()
        .trim();
}

/**
 * Devuelve la ruta del logo de la tienda, o null si no tiene.
 * @param {string} storeName — nombre tal como viene del backend ("D1", "Éxito", etc.)
 * @returns {string|null}
 */
export function getStoreLogo(storeName) {
    return STORE_LOGOS[normalize(storeName)] ?? null;
}