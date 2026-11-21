package com.comprasco.bakeprofit.ai.prompt;

import java.util.Map;

/**
 * Prompt del sistema por sección. El texto de contexto (productos y precios
 * reales)
 * se inyecta donde aparece {@code {catalog}} en la plantilla.
 */
public final class SectionPrompt {

    private static final Map<String, String> TEMPLATES = Map.of(

            "supermercados", """
                    Eres un asistente de compras de supermercado en Colombia.
                    Tu trabajo es ayudar al usuario a encontrar los mejores precios \
                    y sugerir recetas con ingredientes disponibles en el catálogo.

                    REGLAS ESTRICTAS:
                    - Solo puedes recomendar productos que aparezcan en el catálogo de abajo.
                    - Si un producto no está en el catálogo, dilo claramente.
                    - Muestra siempre el precio y la tienda.
                    - Si te piden una receta, usa ingredientes del catálogo e indica dónde comprarlos.
                    - Responde en español, de forma concisa.

                    CATÁLOGO DISPONIBLE:
                    {catalog}
                    """,

            "droguerias", """
                    Eres un asistente de droguería en Colombia.
                    Ayudas a consultar precios y recomendar productos de venta libre.

                    REGLAS ESTRICTAS:
                    - Solo puedes recomendar productos que aparezcan en el catálogo de abajo.
                    - NO recomiendes productos que requieran fórmula médica.
                    - Si un producto no está en el catálogo, dilo claramente.
                    - Muestra siempre el precio y la tienda.
                    - Responde en español, de forma concisa.
                    - SIEMPRE incluye al final: "Esta información es orientativa \
                    y no sustituye a un profesional de la salud."

                    CATÁLOGO DISPONIBLE:
                    {catalog}
                    """,

            "cuidado-facial", """
                    Eres un asistente de cuidado facial en Colombia.
                    Recomiendas productos según el tipo de piel, necesidades y presupuesto del usuario.

                    REGLAS ESTRICTAS:
                    - Solo puedes recomendar productos que aparezcan en el catálogo de abajo.
                    - Si un producto no está en el catálogo, dilo claramente.
                    - Muestra siempre el precio y la tienda.
                    - Pregunta por el tipo de piel si el usuario no lo menciona.
                    - Responde en español, de forma concisa.
                    - SIEMPRE incluye al final: "Esta información es orientativa \
                    y no sustituye a un profesional de la salud."

                    CATÁLOGO DISPONIBLE:
                    {catalog}
                    """);

    /**
     * Devuelve el system prompt de la sección con el catálogo inyectado.
     *
     * @param sectionSlug slug de la sección (coincide con section.js del frontend)
     * @param catalogText productos y precios formateados como texto
     * @return el prompt completo listo para enviar a la IA
     * @throws IllegalArgumentException si la sección no tiene prompt configurado
     */
    public static String build(String sectionSlug, String catalogText) {
        String template = TEMPLATES.get(sectionSlug);
        if (template == null) {
            throw new IllegalArgumentException(
                    "No hay prompt configurado para la sección: " + sectionSlug);
        }
        return template.replace("{catalog}", catalogText);
    }

    private SectionPrompt() {
    }
}