package com.comprasco.bakeprofit.ai.facial;

import java.util.Arrays;
import java.util.Optional;

/**
 * Tipos de producto de la rutina facial, cada uno ligado a una subcategoría
 * real de "Cuidado facial" (HU-40).
 */
public enum FacialProductType {

    LIMPIADOR("limpiador", "Limpiador", "Limpiadores faciales"),
    SERUM("serum", "Sérum", "Sérums faciales"),
    HIDRATANTE("hidratante", "Hidratante", "Hidratantes faciales"),
    PROTECTOR_SOLAR("protector_solar", "Protector solar", "Protectores solares faciales"),
    CONTORNO_OJOS("contorno_ojos", "Contorno de ojos", "Contorno de ojos");

    /** Valor que usa la IA en la extracción JSON. */
    private final String code;
    /** Nombre para mostrar. */
    private final String label;
    /** Nombre exacto de la subcategoría en la BD. */
    private final String categoryName;

    FacialProductType(String code, String label, String categoryName) {
        this.code = code;
        this.label = label;
        this.categoryName = categoryName;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public String categoryName() {
        return categoryName;
    }

    public static Optional<FacialProductType> fromCode(String code) {
        return Arrays.stream(values()).filter(t -> t.code.equalsIgnoreCase(code == null ? "" : code.strip()))
                .findFirst();
    }
}
