package com.comprasco.bakeprofit.ai.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import com.comprasco.bakeprofit.ai.client.GeminiClient;
import com.comprasco.bakeprofit.ai.context.CatalogContextLoader;
import com.comprasco.bakeprofit.ai.dto.AiChatRequest;
import com.comprasco.bakeprofit.ai.dto.AiChatResponse;
import com.comprasco.bakeprofit.ai.dto.AiMessage;
import com.comprasco.bakeprofit.ai.exception.AiInvalidRequestException;
import com.comprasco.bakeprofit.ai.exception.AiMessageTooLongException;
import com.comprasco.bakeprofit.ai.prompt.SectionPrompt;
import com.comprasco.bakeprofit.auth.entity.Plan;
import com.comprasco.bakeprofit.auth.entity.User;

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

    public AiChatService(CatalogContextLoader catalogLoader,
            GeminiClient geminiClient,
            AiQuotaService quotaService) {
        this.catalogLoader = catalogLoader;
        this.geminiClient = geminiClient;
        this.quotaService = quotaService;
    }

    /** Chat ya validado y con la cuota reservada, listo para enviarse a Gemini. */
    public record PreparedChat(String systemPrompt, List<AiMessage> messages, AiChatResponse quotaInfo) {
    }

    /**
     * Procesa una consulta de chat IA sin streaming (fallback).
     */
    public AiChatResponse chat(AiChatRequest request, User user) throws IOException, InterruptedException {
        PreparedChat prepared = prepareChat(request, user);
        try {
            String reply = geminiClient.chat(prepared.systemPrompt(), prepared.messages());
            AiChatResponse q = prepared.quotaInfo();
            return new AiChatResponse(reply, q.queriesUsed(), q.queriesLimit(), q.plan());
        } catch (Exception e) {
            quotaService.refund(user);
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
        return reserve(systemPrompt, buildMessages(request), user);
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
        return reserve(systemPrompt, buildMessages(request), user);
    }

    /**
     * Envía el chat preparado a Gemini en streaming. Si Gemini falla, la cuota
     * reservada se devuelve. Si el cliente se desconecta a mitad de respuesta
     * (el callback lanza), la consulta sí se cuenta porque Gemini ya la cobró.
     */
    public AiChatResponse stream(PreparedChat prepared, User user, Consumer<String> onChunk)
            throws IOException, InterruptedException {
        try {
            geminiClient.chatStream(prepared.systemPrompt(), prepared.messages(), onChunk);
        } catch (ClientGoneException e) {
            throw e;
        } catch (Exception e) {
            quotaService.refund(user);
            throw e;
        }
        return prepared.quotaInfo();
    }

    /** Lanzada por el callback de chunks cuando el cliente ya cerró la conexión. */
    public static class ClientGoneException extends RuntimeException {
        public ClientGoneException() {
            super("El cliente cerró la conexión");
        }
    }

    // ── Helpers ───────────────────────────────────────────

    private PreparedChat reserve(String systemPrompt, List<AiMessage> messages, User user) {
        int used = quotaService.consume(user); // último paso: si algo antes falla, no se cobra
        AiChatResponse quotaInfo = new AiChatResponse("", used, quotaService.limitFor(user), user.getPlan().name());
        return new PreparedChat(systemPrompt, messages, quotaInfo);
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
