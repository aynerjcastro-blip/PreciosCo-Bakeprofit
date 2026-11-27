package com.comprasco.bakeprofit.ai.recipe;

import java.util.List;

/**
 * Búsqueda de productos del catálogo de supermercados por palabras del nombre.
 * Separada del matcher para poder probarlo sin base de datos.
 */
public interface CatalogProductSearch {

    /**
     * IDs de productos activos con precio en una tienda activa de
     * supermercados cuyo nombre contiene todas las palabras (sin distinguir
     * tildes ni mayúsculas), del nombre más corto (más genérico) al más largo.
     */
    List<Long> searchIds(List<String> words, int limit);
}
