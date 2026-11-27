package com.comprasco.bakeprofit.ai.facial;

/**
 * Constantes de la sección de cuidado facial del asistente de IA (HU-40).
 */
public final class FacialSection {

    /** Slug de la sección (el mismo que envía el frontend). */
    public static final String SLUG = "cuidado-facial";

    /**
     * Aviso de orientación de RN-17 / RF32. Lo agrega el backend al final de
     * CADA recomendación: no depende de que el modelo lo escriba.
     */
    public static final String DISCLAIMER =
            "Esta recomendación es orientativa y no sustituye la valoración de un dermatólogo "
                    + "u otro profesional de la salud.";

    /** Respuesta cuando no se conoce el tipo de piel: se pregunta y no se cobra la consulta. */
    public static final String ASK_SKIN_TYPE =
            "Para recomendarte productos que te sirvan, cuéntame tu tipo de piel: grasa, seca, mixta, "
                    + "normal o sensible. Si quieres, dime también qué te gustaría mejorar (acné, manchas, "
                    + "arrugas, hidratación...) y tu presupuesto por producto.";

    private FacialSection() {
    }
}
