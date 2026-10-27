/*
 * follow-helper.js
 *
 * Textos y formatos compartidos por el botón Seguir y la página Seguidos,
 * para que el precio objetivo se describa igual en los dos sitios.
 */

/** 5000 -> "$5.000" (mismo formato de precio que el comparador) */
export function formatPrice(value) {
    return `$${Number(value).toLocaleString('es-CO')}`;
}

/** Describe cuándo avisa un seguimiento, según tenga o no precio objetivo. */
export function describeTarget(targetPrice) {
    return targetPrice == null
        ? 'Te avisamos ante cualquier reducción de precio'
        : `Te avisamos si baja a ${formatPrice(targetPrice)} o menos`;
}