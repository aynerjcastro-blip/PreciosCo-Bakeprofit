package com.comprasco.bakeprofit.ai.facial;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.ai.client.GeminiClient;
import com.comprasco.bakeprofit.ai.dto.AiMessage;
import com.comprasco.bakeprofit.ai.prompt.SectionPrompt;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Primer paso de la recomendación facial (HU-40): Gemini lee la conversación y
 * devuelve en JSON estructurado el tipo de piel, las necesidades, el
 * presupuesto por producto y los tipos de producto a recomendar.
 */
@Component
public class SkinProfileExtractor {

    static final List<String> SKIN_TYPES = List.of("grasa", "seca", "mixta", "normal", "sensible");
    static final List<String> NEEDS = List.of("acne", "brillo", "poros", "manchas", "arrugas", "hidratacion",
            "sensibilidad", "ojeras", "luminosidad", "proteccion_solar");

    /** Rutina básica cuando el usuario no pide un tipo de producto concreto. */
    static final List<FacialProductType> BASIC_ROUTINE = List.of(
            FacialProductType.LIMPIADOR, FacialProductType.HIDRATANTE, FacialProductType.PROTECTOR_SOLAR);

    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "isFacialRequest", Map.of("type", "BOOLEAN"),
                    "skinType", Map.of("type", "STRING",
                            "enum", concat(SKIN_TYPES, List.of("desconocido"))),
                    "needs", Map.of("type", "ARRAY", "items", Map.of("type", "STRING", "enum", NEEDS)),
                    "budget", Map.of("type", "INTEGER"),
                    "productTypes", Map.of("type", "ARRAY", "items", Map.of("type", "STRING",
                            "enum", Arrays.stream(FacialProductType.values()).map(FacialProductType::code).toList()))),
            "required", List.of("isFacialRequest", "skinType", "needs", "budget", "productTypes"));

    private final GeminiClient geminiClient;

    public SkinProfileExtractor(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public SkinProfile extract(List<AiMessage> messages) throws IOException, InterruptedException {
        return parse(geminiClient.generateJson(SectionPrompt.FACIAL_EXTRACTION, messages, SCHEMA));
    }

    /** Convierte y sanea: valores fuera de las listas se descartan; presupuesto <= 0 = sin presupuesto. */
    static SkinProfile parse(JsonNode json) {
        boolean facial = json.path("isFacialRequest").asBoolean(false);

        String skin = json.path("skinType").asText("").strip().toLowerCase(Locale.ROOT);
        if (!SKIN_TYPES.contains(skin)) {
            skin = "";
        }

        Set<String> needs = new LinkedHashSet<>();
        for (JsonNode n : json.path("needs")) {
            String need = n.asText("").strip().toLowerCase(Locale.ROOT);
            if (NEEDS.contains(need)) {
                needs.add(need);
            }
        }

        long rawBudget = json.path("budget").asLong(0);
        BigDecimal budget = rawBudget > 0 ? BigDecimal.valueOf(rawBudget) : null;

        Set<FacialProductType> types = new LinkedHashSet<>();
        for (JsonNode t : json.path("productTypes")) {
            FacialProductType.fromCode(t.asText()).ifPresent(types::add);
        }
        if (types.isEmpty()) {
            types.addAll(BASIC_ROUTINE);
        }

        return new SkinProfile(facial, skin, List.copyOf(needs), budget, orderedForRoutine(types));
    }

    /** Orden de aplicación: limpiador, sérum, contorno, hidratante, protector. */
    private static List<FacialProductType> orderedForRoutine(Set<FacialProductType> types) {
        List<FacialProductType> order = List.of(FacialProductType.LIMPIADOR, FacialProductType.SERUM,
                FacialProductType.CONTORNO_OJOS, FacialProductType.HIDRATANTE, FacialProductType.PROTECTOR_SOLAR);
        return order.stream().filter(types::contains).toList();
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }
}
