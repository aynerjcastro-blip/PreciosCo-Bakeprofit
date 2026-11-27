package com.comprasco.bakeprofit.ai.context;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.ai.exception.AiInvalidRequestException;
import com.comprasco.bakeprofit.ai.pharmacy.PharmacySection;
import com.comprasco.bakeprofit.ai.pharmacy.PrescriptionFilter;
import com.comprasco.bakeprofit.catalog.event.ProductPrescriptionChangedEvent;
import com.comprasco.bakeprofit.catalog.repository.CategoryRepository;
import com.comprasco.bakeprofit.catalog.repository.ProductRepository;
import com.comprasco.bakeprofit.notification.event.PriceRegisteredEvent;
import com.comprasco.bakeprofit.price.dto.CatalogOfferRow;
import com.comprasco.bakeprofit.price.repository.PriceRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Carga productos y precios reales de la BD para inyectarlos como contexto
 * en el prompt de la IA.
 *
 * <p>
 * Rendimiento: el catálogo se arma con 3 consultas de solo lectura
 * (subcategorías, IDs de producto y una proyección plana con marca, tienda,
 * precio y URL) en lugar de cargar entidades {@code Product}, cuyas relaciones
 * EAGER generaban una consulta por marca (N+1, ~200 idas y vueltas a la BD).
 * Además, el texto resultante se guarda en caché por sección y se invalida
 * cuando se registra un precio nuevo.
 * </p>
 */
@Component
public class CatalogContextLoader {

    private static final Logger log = LoggerFactory.getLogger(CatalogContextLoader.class);

    private static final int MAX_PRODUCTS = 200;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PriceRepository priceRepository;
    private final PrescriptionFilter prescriptionFilter;
    private final Cache<String, String> cache;

    public CatalogContextLoader(CategoryRepository categoryRepository,
            ProductRepository productRepository,
            PriceRepository priceRepository,
            PrescriptionFilter prescriptionFilter,
            @Value("${preciosco.ai.catalog-cache-ttl:PT30M}") Duration cacheTtl) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.priceRepository = priceRepository;
        this.prescriptionFilter = prescriptionFilter;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(cacheTtl)
                .maximumSize(10)
                .build();
    }

    /**
     * Genera un bloque de texto con los productos y precios de la sección.
     *
     * <p>
     * Formato de cada línea (cada precio es un enlace Markdown a la tienda
     * cuando hay URL):
     * {@code - Arroz Diana 500 g | Marca: Diana | Precios: [D1 $4500](https://...), Éxito $5200}
     * </p>
     *
     * @param sectionSlug slug de la sección ("supermercados", "droguerias",
     *                    "cuidado-facial")
     * @return texto con el catálogo formateado, listo para el prompt
     */
    public String loadForSection(String sectionSlug) {
        String rootName = resolveRootName(sectionSlug); // valida antes de tocar la caché
        return cache.get(sectionSlug, slug -> build(slug, rootName));
    }

    /** Un precio nuevo cambia el catálogo: se descarta la caché. */
    @EventListener
    public void onPriceRegistered(PriceRegisteredEvent event) {
        cache.invalidateAll();
    }

    /**
     * Un producto se marcó o desmarcó como de fórmula médica (HU-39): se descarta
     * la caché después del commit, para que la siguiente consulta lea la marca nueva.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPrescriptionChanged(ProductPrescriptionChangedEvent event) {
        cache.invalidateAll();
    }

    private String build(String sectionSlug, String rootName) {
        long start = System.nanoTime();

        // 1. Subcategorías activas de la sección
        List<Long> categoryIds = categoryRepository.findActiveChildIdsByRootName(rootName);
        if (categoryIds.isEmpty()) {
            return "(No hay categorías activas en esta sección)";
        }

        // 2. Productos activos con al menos un precio en tienda activa (limitados)
        List<Long> productIds = productRepository.findPricedActiveIdsByCategoryIds(
                categoryIds, Limit.of(MAX_PRODUCTS));
        if (productIds.isEmpty()) {
            return "(No hay productos disponibles en esta sección)";
        }

        // 3. Precios vigentes con todos los datos necesarios, en una sola consulta
        List<CatalogOfferRow> rows = priceRepository.findCatalogOffersByProductIds(productIds);

        // 3b. Droguería: fuera los productos de fórmula médica ANTES de que lleguen al modelo
        //     (HU-39 · RF63 · RN-33). Independiente del prompt y del scraper.
        if (PharmacySection.SLUG.equals(sectionSlug)) {
            PrescriptionFilter.Result filtered = prescriptionFilter.exclude(rows);
            rows = filtered.allowed();
            log.info("Catálogo IA de droguería: {} producto(s) de fórmula médica excluido(s) (RN-33)",
                    filtered.excludedProducts());
        }

        Map<Long, List<CatalogOfferRow>> byProduct = new LinkedHashMap<>();
        for (CatalogOfferRow row : rows) {
            byProduct.computeIfAbsent(row.productId(), id -> new ArrayList<>()).add(row);
        }

        // 4. Formatear como texto plano
        StringBuilder sb = new StringBuilder();
        for (List<CatalogOfferRow> offers : byProduct.values()) {
            CatalogOfferRow first = offers.getFirst();
            sb.append("- ").append(first.productName());
            if (first.unit() != null) {
                sb.append(" ").append(first.unit());
            }
            if (first.brandName() != null) {
                sb.append(" | Marca: ").append(first.brandName());
            }
            sb.append(" | Precios: ");
            for (int i = 0; i < offers.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                appendOffer(sb, offers.get(i));
            }
            sb.append("\n");
        }

        log.debug("Catálogo IA '{}' armado: {} productos en {} ms",
                rootName, byProduct.size(), (System.nanoTime() - start) / 1_000_000);

        return sb.isEmpty()
                ? "(No hay productos con precios disponibles en esta sección)"
                : sb.toString();
    }

    /** {@code [Tienda $precio](url)} si hay URL http(s) válida; si no, texto plano. */
    private static void appendOffer(StringBuilder sb, CatalogOfferRow offer) {
        String label = offer.storeName() + " $" + offer.value().toPlainString();
        String url = safeUrl(offer.productUrl());
        if (url == null) {
            sb.append(label);
        } else {
            sb.append('[').append(label).append("](").append(url).append(')');
        }
    }

    /**
     * Solo URLs http(s). Se codifican espacios y paréntesis para que no rompan
     * la sintaxis de enlace Markdown.
     */
    public static String safeUrl(String url) {
        if (url == null) {
            return null;
        }
        String u = url.trim();
        if (!(u.startsWith("https://") || u.startsWith("http://"))) {
            return null;
        }
        return u.replace(" ", "%20").replace("(", "%28").replace(")", "%29");
    }

    /**
     * Convierte el slug del frontend al nombre de la categoría raíz en la BD.
     * Los slugs vienen de section.js del frontend y los rootName coinciden
     * con lo que se guardó en la tabla categories.
     */
    private String resolveRootName(String slug) {
        return switch (slug) {
            case "supermercados" -> "Supermercado";
            case "droguerias" -> "Droguería";
            case "cuidado-facial" -> "Cuidado facial";
            default -> throw new AiInvalidRequestException("Sección desconocida: " + slug);
        };
    }
}
