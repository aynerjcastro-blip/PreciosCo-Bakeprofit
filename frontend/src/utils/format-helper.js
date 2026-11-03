/*
 * format-helper.js
 *
 * Formatos de números y fechas para mostrar en pantalla, en español de Colombia.
 * Centralizarlos evita repetir toLocaleString en cada componente.
 */

/** 4.5 -> "4,5" */
export function formatScore(score) {
    return Number(score).toLocaleString('es-CO', {
        minimumFractionDigits: 1,
        maximumFractionDigits: 1,
    });
}

/** "2026-10-19T10:30:00" -> "19/10/2026" (mismo formato que "Actualizado:" en el comparador) */
export function formatDate(iso) {
    return new Date(iso).toLocaleDateString('es-CO');
}

/** "2026-10-19T15:40:00" -> "19/10/2026, 3:40 p. m." */
export function formatDateTime(iso) {
    return new Date(iso).toLocaleString('es-CO', { dateStyle: 'short', timeStyle: 'short' });
}