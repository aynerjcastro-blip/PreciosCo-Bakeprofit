package com.comprasco.bakeprofit.catalog.dto;

/**
 * Orden de la búsqueda (parámetro {@code sort}, reservado y extensible: un orden nuevo es un valor más).
 *
 * <p>
 * El usuario no lo elige en la interfaz (DP-36): el frontend manda SAVINGS. Los dos órdenes
 * desempatan por nombre y luego por id, para que las páginas no repitan ni salten productos.
 * </p>
 */
public enum ProductSort {
    /** Mayor ahorro primero (DP-39); con ahorro 0 (un solo precio o sin precio) van al final. Por defecto. */
    SAVINGS,
    /** Por nombre: el orden anterior a HT-13. */
    NAME
}
