package com.comprasco.bakeprofit.ai.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import com.comprasco.bakeprofit.ai.client.GeminiClient;
import com.comprasco.bakeprofit.ai.context.CatalogContextLoader;
import com.comprasco.bakeprofit.ai.dto.AiChatRequest;
import com.comprasco.bakeprofit.ai.dto.AiChatResponse;
import com.comprasco.bakeprofit.ai.dto.AiMessage;
import com.comprasco.bakeprofit.ai.dto.AiRecipeResponse;
import com.comprasco.bakeprofit.ai.exception.AiInvalidRequestException;
import com.comprasco.bakeprofit.ai.exception.AiMessageTooLongException;
import com.comprasco.bakeprofit.ai.prompt.SectionPrompt;
import com.comprasco.bakeprofit.ai.recipe.ExtractedRecipe;
import com.comprasco.bakeprofit.ai.recipe.MatchedIngredient;
import com.comprasco.bakeprofit.ai.recipe.RecipeExtractor;
import com.comprasco.bakeprofit.ai.recipe.RecipeIngredientMatcher;
import com.comprasco.bakeprofit.auth.entity.Plan;
import com.comprasco.bakeprofit.auth.entity.User;
import com.comprasco.bakeprofit.price.dto.CatalogOfferRow;

/**
 * Orquesta la consulta de chat IA en dos fases:
 *
 * <ol>
 * <li>{@code prepare*}: valida la petición, arma el prompt con el catálogo y
 * reserva la cuota. Se llama ANTES de empezar a escribir la respuesta SSE,
 * para que los errores (400, 429) salgan como respuestas HTTP normales.</li>
 * <li>{@link #stream}: llama a Gemini en streaming. Si falla, devuelve la
 * cuota reservada.</li>
 * </ol>
 */
@Service
public class AiChatService {

    /** Límite de caracteres para usuarios FREE en cotización (RF60). */
    private static final int FREE_COTIZACION_CHAR_LIMIT = 200;

    private final CatalogContextLoader catalogLoader;
    private final GeminiClient geminiClient;
    private final AiQuotaService quotaService;
    private final RecipeExtractor recipeExtractor;
    private final RecipeIngredientMatcher ingredientMatcher;

    public AiChatService(CatalogContextLoader catalogLoader,
            GeminiClient geminiClient,
            AiQuotaService quotaService,
            RecipeExtractor recipeExtractor,
            RecipeIngredientMatcher ingredientMatcher) {
        this.catalogLoader = catalogLoader;
        this.geminiClient = geminiClient;
        this.quotaService = quotaService;
        this.recipeExtractor = recipeExtractor;
        this.ingredientMatcher = ingredientMatcher;
    }

    /**
     * Chat ya validado y con la cuota reservada, listo para enviarse a Gemini.
     *
     * @param donePayload lo que se envía en el evento {@code done} al terminar
     * @param refund      devuelve la cuota reservada si Gemini falla
     */
    public record PreparedChat(String systemPrompt, List<AiMessage> messages, Object donePayload,
            Runnable refund) {
    }

    /**
     * Procesa una consulta de chat IA sin streaming (fallback).
     */
    public AiChatResponse chat(AiChatRequest request, User user) throws IOException, InterruptedException {
        PreparedChat prepared = prepareChat(request, user);
        try {
            String reply = geminiClient.chat(prepared.systemPrompt(), prepared.messages());
            AiChatResponse q = (AiChatResponse) prepared.donePayload();
            return new AiChatResponse(reply, q.queriesUsed(), q.queriesLimit(), q.plan());
        } catch (Exception e) {
            prepared.refund().run();
            throw e;
        }
    }

    /**
     * Valida y prepara una consulta del chat general de una sección.
     *
     * @throws AiInvalidRequestException si falta la sección
     * @throws com.comprasco.bakeprofit.ai.exception.AiQuotaExceededException si
     *         no hay cuota
     */
    public PreparedChat prepareChat(AiChatRequest request, User user) {
        if (request.section() == null || request.section().isBlank()) {
            throw new AiInvalidRequestException("La sección es obligatoria");
        }
        String catalogText = catalogLoader.loadForSection(request.section());
        String systemPrompt = SectionPrompt.build(request.section(), catalogText);
        return reserveQuery(systemPrompt, buildMessages(request), user);
    }

    /**
     * Valida y prepara una cotización de lista de productos (HU-37,
     * SCRUM-324). Siempre usa la sección "supermercados".
     *
     * @throws AiMessageTooLongException si un usuario FREE excede 200
     *                                   caracteres en el mensaje o en algún turno
     *                                   suyo del historial (RF60)
     */
    public PreparedChat prepareCotizacion(AiChatRequest request, User user) {
        if (user.getPlan() != Plan.PREMIUM) {
            checkFreeLength(request.message());
            if (request.history() != null) {
                request.history().stream()
                        .filter(t -> "user".equals(t.role()))
                        .forEach(t -> checkFreeLength(t.content()));
            }
        }
        String catalogText = catalogLoader.loadForSection("supermercados");
        String systemPrompt = SectionPrompt.buildCotizacion(catalogText);
        return reserveQuery(systemPrompt, buildMessages(request), user);
    }

    /**
     * Prepara una receta (SCRUM-325, RF30), con opción B:
     * <ol>
     * <li>Reserva una receta de la cuota propia de recetas (no gasta
     * consultas).</li>
     * <li>Gemini extrae el plato y los ingredientes en JSON.</li>
     * <li>Cada ingrediente se busca en TODO el catálogo de supermercados: la
     * disponibilidad la decide la base de datos.</li>
     * <li>Se arma el prompt de redacción con esa disponibilidad.</li>
     * </ol>
     * Si algo falla antes del stream, la receta se devuelve.
     *
     * @throws com.comprasco.bakeprofit.ai.exception.AiRecipeQuotaExceededException
     *         si el usuario FREE agotó sus recetas del mes
     * @throws AiInvalidRequestException si el mensaje no pide un plato
     */
    public PreparedChat prepareReceta(AiChatRequest request, User user) throws IOException, InterruptedException {
        int used = quotaService.consumeRecipe(user); // primero: la extracción ya consume Gemini
        Runnable refund = () -> quotaService.refundRecipe(user);
        try {
            List<AiMessage> messages = buildMessages(request);
            ExtractedRecipe recipe = recipeExtractor.extract(messages);
            if (!recipe.recipe()) {
                throw new AiInvalidRequestException(
                        "No identifiqué un plato en tu mensaje. Prueba con algo como \"ajiaco para 4 personas\".");
            }
            int servings = recipe.servings() > 0 ? recipe.servings() : 4;
            List<MatchedIngredient> matched = ingredientMatcher.match(recipe.ingredients());

            String systemPrompt = SectionPrompt.buildReceta(recipe.dish(), servings, availabilityText(matched));
            AiRecipeResponse done = recipeDone(used, user, recipe.dish(), servings, matched);
            return new PreparedChat(systemPrompt, messages, done, refund);
        } catch (Exception e) {
            refund.run();
            throw e;
        }
    }

    /**
     * Envía el chat preparado a Gemini en streaming. Si Gemini falla, la cuota
     * reservada se devuelve. Si el cliente se desconecta a mitad de respuesta
     * (el callback lanza), la consulta sí se cuenta porque Gemini ya la cobró.
     *
     * @return el contenido del evento {@code done}
     */
    public Object stream(PreparedChat prepared, Consumer<String> onChunk)
            throws IOException, InterruptedException {
        try {
            geminiClient.chatStream(prepared.systemPrompt(), prepared.messages(), onChunk);
        } catch (ClientGoneException e) {
            throw e;
        } catch (Exception e) {
            prepared.refund().run();
            throw e;
        }
        return prepared.donePayload();
    }

    /** Lanzada por el callback de chunks cuando el cliente ya cerró la conexión. */
    public static class ClientGoneException extends RuntimeException {
        public ClientGoneException() {
            super("El cliente cerró la conexión");
        }
    }

    // ── Helpers ───────────────────────────────────────────

    private PreparedChat reserveQuery(String systemPrompt, List<AiMessage> messages, User user) {
        int used = quotaService.consume(user); // último paso: si algo antes falla, no se cobra
        AiChatResponse quotaInfo = new AiChatResponse("", used, quotaService.limitFor(user), user.getPlan().name());
        return new PreparedChat(systemPrompt, messages, quotaInfo, () -> quotaService.refund(user));
    }

    /** Una línea por ingrediente con la disponibilidad ya verificada, para el prompt. */
    static String availabilityText(List<MatchedIngredient> matched) {
        StringBuilder sb = new StringBuilder();
        for (MatchedIngredient m : matched) {
            sb.append("- ").append(m.ingredient().name());
            if (!m.ingredient().quantity().isBlank()) {
                sb.append(" (").append(m.ingredient().quantity()).append(')');
            }
            if (m.available()) {
                sb.append(": DISPONIBLE (").append(m.best().name()).append(')');
            } else {
                sb.append(": NO DISPONIBLE");
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private AiRecipeResponse recipeDone(int used, User user, String dish, int servings,
            List<MatchedIngredient> matched) {
        BigDecimal total = BigDecimal.ZERO;
        List<AiRecipeResponse.Ingredient> ingredients = new ArrayList<>();
        for (MatchedIngredient m : matched) {
            if (!m.available()) {
                ingredients.add(new AiRecipeResponse.Ingredient(
                        m.ingredient().name(), m.ingredient().quantity(), false, null, null, null, null));
                continue;
            }
            MatchedIngredient.ProductMatch product = m.best();
            CatalogOfferRow offer = product.cheapest();
            total = total.add(offer.value());
            String productName = product.unit() == null ? product.name() : product.name() + " " + product.unit();
            ingredients.add(new AiRecipeResponse.Ingredient(
                    m.ingredient().name(), m.ingredient().quantity(), true,
                    productName, offer.storeName(), offer.value(),
                    CatalogContextLoader.safeUrl(offer.productUrl())));
        }
        return new AiRecipeResponse(used, quotaService.recipeLimitFor(user), user.getPlan().name(),
                dish, servings, ingredients, total);
    }

    private static void checkFreeLength(String text) {
        if (text != null && text.length() > FREE_COTIZACION_CHAR_LIMIT) {
            throw new AiMessageTooLongException(text.length(), FREE_COTIZACION_CHAR_LIMIT);
        }
    }

    private List<AiMessage> buildMessages(AiChatRequest request) {
        List<AiMessage> messages = new ArrayList<>();

        if (request.history() != null) {
            for (AiChatRequest.Turn turn : request.history()) {
                // El DTO ya garantiza role ∈ {user, assistant}
                String geminiRole = "assistant".equals(turn.role()) ? "model" : "user";
                messages.add(AiMessage.of(geminiRole, turn.content()));
            }
        }

        messages.add(AiMessage.of("user", request.message()));
        return messages;
    }
}
