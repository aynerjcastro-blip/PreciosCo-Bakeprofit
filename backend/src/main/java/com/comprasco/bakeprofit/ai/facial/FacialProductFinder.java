package com.comprasco.bakeprofit.ai.facial;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.ai.context.CatalogContextLoader;
import com.comprasco.bakeprofit.ai.text.SearchText;
import com.comprasco.bakeprofit.catalog.repository.CategoryRepository;
import com.comprasco.bakeprofit.catalog.repository.ProductRepository;
import com.comprasco.bakeprofit.price.dto.CatalogOfferRow;
import com.comprasco.bakeprofit.price.repository.PriceRepository;

/**
 * Busca en el catálogo de Cuidado facial las opciones de cada tipo de producto
 * DENTRO DEL PRESUPUESTO (HU-40, RF32).
 *
 * <p>
 * El presupuesto lo verifica el backend, no la IA: un producto entra si su
 * precio vigente más barato es menor o igual al presupuesto. Dentro del
 * presupuesto se ordena por coincidencia del nombre con la necesidad y el tipo
 * de piel (p. ej. "Oil Control" para piel grasa, "Anti Manchas" para manchas) y
 * luego por precio. Si un tipo no tiene nada dentro del presupuesto, se informa
 * la opción más barata que lo supera.
 * </p>
 */
@Component
public class FacialProductFinder {

    static final String SECTION_ROOT_NAME = "Cuidado facial";
    /** Opciones que se muestran por tipo de producto. */
    static final int MAX_OPTIONS = 3;
    /** Productos por subcategoría que se consideran (la más grande tiene ~200). */
    private static final int MAX_PRODUCTS_PER_TYPE = 400;

    private static final int NEED_WEIGHT = 2;
    private static final int SKIN_WEIGHT = 1;
    /** Productos infantiles: quedan al final salvo que el usuario los pida. */
    private static final int KIDS_PENALTY = -5;
    private static final List<String> KIDS_TERMS = List.of("kids", "kid", "nino", "ninos", "nina", "ninas",
            "infantil", "bebe", "bebes", "baby", "pediatrico");

    /** Palabras del nombre que delatan para qué tipo de piel es un producto (normalizadas). */
    private static final Map<String, List<String>> SKIN_TERMS = Map.of(
            "grasa", List.of("oil control", "oil free", "grasa", "graso", "matificante", "mat", "sebo",
                    "sebum", "purificante", "effaclar", "acnimat", "normaderm", "sebium", "ac control"),
            "seca", List.of("seca", "seco", "nutritiva", "nutritivo", "reparadora", "reparador", "rich", "riche",
                    "intensa", "intensiva", "ceramidas", "ceramida", "balsamo", "lipikar", "atoderm", "xemose"),
            "mixta", List.of("mixta", "mixto", "oil control", "matificante", "mat", "gel", "fluido", "ligero"),
            "sensible", List.of("sensible", "sensibles", "sensitive", "calmante", "toleriane", "sensibio",
                    "cicaplast", "rojeces", "rosacea", "sin perfume"),
            "normal", List.of());

    /** Palabras del nombre asociadas a cada necesidad (normalizadas). */
    private static final Map<String, List<String>> NEED_TERMS = Map.of(
            "acne", List.of("acne", "imperfecciones", "anti imperfecciones", "ac control", "effaclar", "acnimat",
                    "salicilico", "niacinamida", "biretix", "normaderm", "sebium", "blemish", "purificante"),
            "brillo", List.of("oil control", "matificante", "mat", "sebo", "oil free"),
            "poros", List.of("poros", "poro", "niacinamida", "salicilico", "purificante"),
            "manchas", List.of("manchas", "anti manchas", "antimanchas", "antimarcas", "melasma", "pigment",
                    "despigmentante", "vitamina c", "vitamin c", "niacinamida", "mela", "aclarante", "unificador"),
            "arrugas", List.of("arrugas", "anti arrugas", "antiedad", "anti edad", "retinol", "lift", "liftactiv",
                    "firmeza", "colageno", "peptidos", "age"),
            "hidratacion", List.of("hidratante", "hidratacion", "acido hialuronico", "hialuronico", "hyalu",
                    "aqua", "water", "ceramidas", "hydra", "hydrance"),
            "sensibilidad", List.of("sensible", "calmante", "rojeces", "toleriane", "cicaplast", "sensibio"),
            "ojeras", List.of("ojeras", "ojos", "bolsas", "iluminador"),
            "luminosidad", List.of("vitamina c", "vitamin c", "luminosidad", "radiance", "glow", "iluminador"),
            "proteccion_solar", List.of("spf", "fps", "solar", "uv"));

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PriceRepository priceRepository;

    public FacialProductFinder(CategoryRepository categoryRepository, ProductRepository productRepository,
            PriceRepository priceRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.priceRepository = priceRepository;
    }

    /** Una opción del catálogo con su precio más barato. */
    public record FacialOption(Long productId, String name, String unit, String brand, String store,
            BigDecimal price, String url) {
    }

    /**
     * Resultado de un tipo de producto.
     *
     * @param withinBudget       hasta 3 opciones dentro del presupuesto (vacío si no hay)
     * @param cheapestOverBudget la opción más barata que supera el presupuesto, solo si
     *                           no hay ninguna dentro; null en otro caso
     * @param availableInCatalog false si la subcategoría no tiene productos con precio
     */
    public record FacialGroup(FacialProductType type, List<FacialOption> withinBudget,
            FacialOption cheapestOverBudget, boolean availableInCatalog) {
    }

    public List<FacialGroup> find(SkinProfile profile) {
        // 1. Productos con precio de cada subcategoría pedida
        Map<FacialProductType, List<Long>> idsByType = new EnumMap<>(FacialProductType.class);
        Set<Long> allIds = new LinkedHashSet<>();
        for (FacialProductType type : profile.productTypes()) {
            List<Long> ids = categoryRepository
                    .findActiveChildIdByRootAndName(SECTION_ROOT_NAME, type.categoryName())
                    .map(id -> productRepository.findPricedActiveIdsByCategoryIds(
                            List.of(id), Limit.of(MAX_PRODUCTS_PER_TYPE)))
                    .orElse(List.of());
            idsByType.put(type, ids);
            allIds.addAll(ids);
        }

        // 2. Precios vigentes de todos, en una sola consulta
        Map<Long, List<CatalogOfferRow>> offers = new LinkedHashMap<>();
        if (!allIds.isEmpty()) {
            for (CatalogOfferRow row : priceRepository.findCatalogOffersByProductIds(allIds)) {
                offers.computeIfAbsent(row.productId(), k -> new ArrayList<>()).add(row);
            }
        }

        // 3. Por tipo: dentro del presupuesto, ordenado por coincidencia y precio
        List<FacialGroup> groups = new ArrayList<>();
        for (FacialProductType type : profile.productTypes()) {
            List<FacialOption> options = idsByType.get(type).stream()
                    .filter(offers::containsKey)
                    .map(id -> toOption(offers.get(id)))
                    .toList();
            groups.add(group(type, options, profile));
        }
        return groups;
    }

    static FacialGroup group(FacialProductType type, List<FacialOption> options, SkinProfile profile) {
        if (options.isEmpty()) {
            return new FacialGroup(type, List.of(), null, false);
        }
        BigDecimal budget = profile.budget();
        List<FacialOption> within = options.stream()
                .filter(o -> budget == null || o.price().compareTo(budget) <= 0)
                .sorted(Comparator.comparingInt((FacialOption o) -> -score(o, profile))
                        .thenComparing(FacialOption::price))
                .limit(MAX_OPTIONS)
                .toList();
        FacialOption cheapestOver = within.isEmpty()
                ? options.stream().min(Comparator.comparing(FacialOption::price)).orElse(null)
                : null;
        return new FacialGroup(type, within, cheapestOver, true);
    }

    /** Coincidencias del nombre (y marca) con las necesidades (peso 2) y el tipo de piel (peso 1). */
    static int score(FacialOption option, SkinProfile profile) {
        String text = " " + SearchText.normalize(option.name() + " " + (option.brand() == null ? "" : option.brand()))
                + " ";
        int score = 0;
        for (String need : profile.needs()) {
            if (matchesAny(text, NEED_TERMS.getOrDefault(need, List.of()))) {
                score += NEED_WEIGHT;
            }
        }
        if (profile.hasSkinType() && matchesAny(text, SKIN_TERMS.getOrDefault(profile.skinType(), List.of()))) {
            score += SKIN_WEIGHT;
        }
        if (matchesAny(text, KIDS_TERMS)) {
            score += KIDS_PENALTY;
        }
        return score;
    }

    private static boolean matchesAny(String paddedText, List<String> terms) {
        return terms.stream().anyMatch(t -> SearchText.containsWords(paddedText, t));
    }

    private static FacialOption toOption(List<CatalogOfferRow> rows) {
        CatalogOfferRow cheapest = rows.stream().min(Comparator.comparing(CatalogOfferRow::value)).orElseThrow();
        return new FacialOption(cheapest.productId(), cheapest.productName(),
                unitIfMissing(cheapest.productName(), cheapest.unit()), cheapest.brandName(),
                cheapest.storeName(), cheapest.value(), CatalogContextLoader.safeUrl(cheapest.productUrl()));
    }

    /**
     * La unidad ("30 ml") solo si el nombre no trae ya ese tamaño ("Sérum 30ml"),
     * para no mostrar "30ml 30 ml".
     */
    static String unitIfMissing(String name, String unit) {
        if (unit == null || unit.isBlank()) {
            return null;
        }
        String compactName = SearchText.normalize(name).replace(" ", "");
        String compactUnit = SearchText.normalize(unit).replace(" ", "");
        return compactName.contains(compactUnit) ? null : unit;
    }
}
