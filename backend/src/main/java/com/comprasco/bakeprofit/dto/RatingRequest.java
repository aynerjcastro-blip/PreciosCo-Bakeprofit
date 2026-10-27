package com.comprasco.bakeprofit.dto;

import jakarta.validation.constraints.*;

public class RatingRequest {

    @NotNull(message = "El id del producto es obligatorio")
    private Long productId;

    @NotNull(message = "El id de la tienda es obligatorio")
    private Long storeId;

    @NotNull(message = "El puntaje es obligatorio")
    @Min(value = 1, message = "El puntaje mínimo es 1")
    @Max(value = 5, message = "El puntaje máximo es 5")
    private Integer score;

    @NotBlank(message = "El comentario no puede estar vacío")
    @Size(min = 10, max = 500, message = "El comentario debe tener entre 10 y 500 caracteres")
    private String comment;

    // Getters y Setters
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}