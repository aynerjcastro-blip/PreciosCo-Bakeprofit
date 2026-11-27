package com.comprasco.bakeprofit.ai.pharmacy;

/**
 * Constantes de la sección de droguería del asistente de IA (HU-39).
 */
public final class PharmacySection {

    /** Slug de la sección (el mismo que envía el frontend). */
    public static final String SLUG = "droguerias";

    /**
     * Aviso obligatorio de RN-33 / RF63. Lo agrega el backend al final de CADA
     * respuesta de droguería: no depende de que el modelo lo escriba.
     */
    public static final String DISCLAIMER =
            "Esta información es orientativa y no sustituye el diagnóstico, la prescripción médica "
                    + "ni la orientación de un profesional de la salud.";

    private PharmacySection() {
    }
}
