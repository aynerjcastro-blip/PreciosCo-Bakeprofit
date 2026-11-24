package com.comprasco.bakeprofit.ai.prompt;

import java.util.Map;

/**
 * Prompt del sistema por sección. El texto de contexto (productos y precios
 * reales) se inyecta donde aparece {@code {catalog}} en la plantilla.
 *
 * <p>
 * Al final de cada respuesta, la IA debe incluir un bloque
 * {@code [SUGERENCIAS]} con 2-3 consultas sugeridas para que el frontend
 * las muestre como chips clicables.
 * </p>
 */
public final class SectionPrompt {

        /**
         * Instrucción común que se agrega al final de cada prompt para que
         * Gemini genere sugerencias de follow-up.
         *
         * <p>
         * Formato estricto para facilitar el parsing en el frontend:
         * </p>
         * 
         * <pre>
         * [SUGERENCIAS]
         * - Texto de sugerencia 1
         * - Texto de sugerencia 2
         * - Texto de sugerencia 3
         * </pre>
         */
        private static final String SUGGESTION_INSTRUCTION = """

                        SUGERENCIAS DE SEGUIMIENTO:
                        Al final de CADA respuesta, incluye EXACTAMENTE este bloque:
                        [SUGERENCIAS]
                        - Primera consulta sugerida relacionada
                        - Segunda consulta sugerida relacionada
                        - Tercera consulta sugerida relacionada

                        Las sugerencias deben ser consultas breves (máx 6 palabras) \
                        que el usuario probablemente quiera hacer después de tu respuesta. \
                        Deben ser específicas al tema de la conversación y basadas en \
                        productos del catálogo. No repitas la consulta original.""";

        private static final Map<String, String> TEMPLATES = Map.of(

                        "supermercados",
                        """
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
                                        """
                                        + SUGGESTION_INSTRUCTION,

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
                                        """ + SUGGESTION_INSTRUCTION,

                        "cuidado-facial",
                        """
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
                                        """
                                        + SUGGESTION_INSTRUCTION);

        /**
         * Devuelve el system prompt de la sección con el catálogo inyectado.
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