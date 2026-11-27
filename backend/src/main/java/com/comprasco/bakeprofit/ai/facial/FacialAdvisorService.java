package com.comprasco.bakeprofit.ai.facial;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.comprasco.bakeprofit.ai.dto.AiChatRequest;
import com.comprasco.bakeprofit.ai.dto.AiFacialResponse;
import com.comprasco.bakeprofit.ai.dto.AiMessage;
import com.comprasco.bakeprofit.ai.exception.AiInvalidRequestException;
import com.comprasco.bakeprofit.ai.facial.FacialProductFinder.FacialGroup;
import com.comprasco.bakeprofit.ai.facial.FacialProductFinder.FacialOption;
import com.comprasco.bakeprofit.ai.prompt.SectionPrompt;
import com.comprasco.bakeprofit.ai.service.AiChatService.PreparedChat;
import com.comprasco.bakeprofit.ai.service.AiQuotaService;
import com.comprasco.bakeprofit.auth.entity.User;

/**
 * Recomendaciones de cuidado facial según tipo de piel, necesidades y
 * presupuesto (HU-40, RF32, RN-17).
 *
 * <ol>
 * <li>Reserva 1 consulta de la cuota normal.</li>
 * <li>Gemini extrae el perfil (tipo de piel, necesidades, presupuesto por
 * producto, tipos de producto) en JSON.</li>
 * <li>Si falta el tipo de piel, se pregunta SIN llamar otra vez a Gemini y la
 * consulta se devuelve.</li>
 * <li>El backend busca opciones por tipo de producto DENTRO DEL PRESUPUESTO
 * ({@link FacialProductFinder}).</li>
 * <li>Se arma el prompt de redacción; el stream lo hace
 * {@link com.comprasco.bakeprofit.ai.service.AiChatService#stream}, que agrega
 * el aviso de orientación al final.</li>
 * </ol>
 */
@Service
public class FacialAdvisorService {

    private final AiQuotaService quotaService;
    private final SkinProfileExtractor profileExtractor;
    private final FacialProductFinder productFinder;

    public FacialAdvisorService(AiQuotaService quotaService, SkinProfileExtractor profileExtractor,
            FacialProductFinder productFinder) {
        this.quotaService = quotaService;
        this.profileExtractor = profileExtractor;
        this.productFinder = productFinder;
    }

    /**
     * @throws AiInvalidRequestException si el mensaje no es sobre cuidado facial
     * @throws com.comprasco.bakeprofit.ai.exception.AiQuotaExceededException si no hay cuota
     */
    public PreparedChat prepare(AiChatRequest request, User user) throws IOException, InterruptedException {
        int used = quotaService.consume(user); // primero: la extracción ya consume Gemini
        Runnable refund = () -> quotaService.refund(user);
        int limit = quotaService.limitFor(user);
        String plan = user.getPlan().name();
        try {
            List<AiMessage> messages = buildMessages(request);
            SkinProfile profile = profileExtractor.extract(messages);

            if (!profile.facialRequest()) {
                throw new AiInvalidRequestException("Este asistente recomienda productos de cuidado facial. "
                        + "Cuéntame tu tipo de piel, qué te gustaría mejorar y tu presupuesto.");
            }

            if (!profile.hasSkinType()) {
                refund.run(); // preguntar no se cobra
                AiFacialResponse done = new AiFacialResponse(Math.max(0, used - 1), limit, plan,
                        FacialSection.DISCLAIMER, null, profile.needs(), profile.budget(), List.of());
                return new PreparedChat(null, messages, done, () -> {
                }, FacialSection.DISCLAIMER, FacialSection.ASK_SKIN_TYPE);
            }

            List<FacialGroup> groups = productFinder.find(profile);
            String systemPrompt = SectionPrompt.buildFacial(profileText(profile), optionsText(groups));
            AiFacialResponse done = new AiFacialResponse(used, limit, plan, FacialSection.DISCLAIMER,
                    profile.skinType(), profile.needs(), profile.budget(), toDto(groups));
            return new PreparedChat(systemPrompt, messages, done, refund, FacialSection.DISCLAIMER);
        } catch (Exception e) {
            refund.run();
            throw e;
        }
    }

    // ── Texto para el prompt ──────────────────────────────

    static String profileText(SkinProfile profile) {
        return "- Tipo de piel: " + profile.skinType() + "\n"
                + "- Necesidades: " + (profile.needs().isEmpty() ? "no indicó" : String.join(", ", profile.needs()))
                + "\n"
                + "- Presupuesto por producto: "
                + (profile.budget() == null ? "no indicó" : "$" + plain(profile.budget()));
    }

    static String optionsText(List<FacialGroup> groups) {
        StringBuilder sb = new StringBuilder();
        for (FacialGroup g : groups) {
            sb.append("### ").append(g.type().label()).append('\n');
            if (!g.availableInCatalog()) {
                sb.append("NO HAY PRODUCTOS DE ESTE TIPO EN EL CATÁLOGO\n");
            } else if (g.withinBudget().isEmpty()) {
                sb.append("SIN OPCIONES DENTRO DEL PRESUPUESTO");
                if (g.cheapestOverBudget() != null) {
                    sb.append(". La más barata del catálogo (SUPERA el presupuesto): ")
                            .append(optionLine(g.cheapestOverBudget()));
                }
                sb.append('\n');
            } else {
                int i = 1;
                for (FacialOption o : g.withinBudget()) {
                    sb.append(i++).append(". ").append(optionLine(o)).append('\n');
                }
            }
        }
        return sb.toString();
    }

    private static String optionLine(FacialOption o) {
        String price = o.store() + " $" + plain(o.price());
        String priceText = o.url() == null ? price : "[" + price + "](" + o.url() + ")";
        return o.name() + (o.unit() == null ? "" : " " + o.unit())
                + (o.brand() == null ? "" : " | Marca: " + o.brand())
                + " | " + priceText;
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    // ── DTO del evento done ───────────────────────────────

    private static List<AiFacialResponse.Group> toDto(List<FacialGroup> groups) {
        return groups.stream().map(g -> new AiFacialResponse.Group(
                g.type().code(),
                g.type().label(),
                g.withinBudget().stream().map(FacialAdvisorService::toDto).toList(),
                g.cheapestOverBudget() == null ? null : toDto(g.cheapestOverBudget())))
                .toList();
    }

    private static AiFacialResponse.Option toDto(FacialOption o) {
        String name = o.unit() == null ? o.name() : o.name() + " " + o.unit();
        return new AiFacialResponse.Option(o.productId(), name, o.brand(), o.store(), o.price(), o.url());
    }

    private static List<AiMessage> buildMessages(AiChatRequest request) {
        List<AiMessage> messages = new ArrayList<>();
        if (request.history() != null) {
            for (AiChatRequest.Turn turn : request.history()) {
                messages.add(AiMessage.of("assistant".equals(turn.role()) ? "model" : "user", turn.content()));
            }
        }
        messages.add(AiMessage.of("user", request.message()));
        return messages;
    }
}
