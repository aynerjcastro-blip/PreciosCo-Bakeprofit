package com.comprasco.bakeprofit.price.dto;

/**
 * Foto de un producto tal como la publica una tienda en su precio vigente.
 * La usa la búsqueda para mostrar una vista previa sin cargar cada precio.
 */
public record ProductImage(Long productId, String imageUrl) {}