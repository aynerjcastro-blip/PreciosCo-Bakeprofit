package com.comprasco.bakeprofit.ai.context;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.catalog.entity.Category;
import com.comprasco.bakeprofit.catalog.entity.Product;
import com.comprasco.bakeprofit.catalog.repository.CategoryRepository;
import com.comprasco.bakeprofit.catalog.repository.ProductRepository;
import com.comprasco.bakeprofit.price.dto.ProductOffer;
import com.comprasco.bakeprofit.price.repository.PriceRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

/**
 * Carga productos y precios reales de la BD para inyectarlos como contexto
 * en el prompt de la IA.
 *
 * <p>
 * No es un "RAG con embeddings" — es una consulta SQL directa.
 * El catálogo de PreciosCo es lo bastante pequeño para que esto funcione
 * sin infraestructura adicional.
 * </p>
 */
@Component
public class CatalogContextLoader {

    private static final int MAX_PRODUCTS = 200;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PriceRepository priceRepository;

    public CatalogContextLoader(CategoryRepository categoryRepository,
            ProductRepository productRepository,
            PriceRepository priceRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.priceRepository = priceRepository;
    }

    /**
     * Genera un bloque de texto con los productos y precios de la sección.
     *
     * <p>
     * Formato de cada línea:
     * {@code Producto: Arroz Diana 500g | Marca: Diana | Tienda: D1 $4.500, Éxito $5.200}
     * </p>
     *
     * @param sectionSlug slug de la sección ("supermercados", "droguerias",
     *                    "cuidado-facial")
     * @return texto con el catálogo formateado, listo para el prompt
     */
    public String loadForSection(String sectionSlug) {
        // 1. Encontrar la categoría raíz que corresponde a esta sección
        String rootName = resolveRootName(sectionSlug);
        Category root = categoryRepository.findByParentIsNull().stream()
                .filter(c -> c.getName().equalsIgnoreCase(rootName))
                .findFirst()
                .orElse(null);

        if (root == null) {
            return "(No hay productos disponibles en esta sección)";
        }

        // 2. Categorías hijas activas de esta raíz
        List<Long> childCategoryIds = categoryRepository
                .findByParentIdAndActiveTrue(root.getId())
                .stream()
                .map(Category::getId)
                .toList();

        if (childCategoryIds.isEmpty()) {
            return "(No hay categorías activas en esta sección)";
        }

        // 3. Productos activos de esas categorías (limitados)
        Specification<Product> spec = (r, q, cb) -> cb.and(
                r.get("category").get("id").in(childCategoryIds),
                cb.isTrue(r.get("active")));
        List<Product> products = productRepository.findAll(spec, PageRequest.of(0, MAX_PRODUCTS))
                .getContent();

        if (products.isEmpty()) {
            return "(No hay productos disponibles en esta sección)";
        }

        // 4. Precios vigentes de esos productos, agrupados por producto
        List<Long> productIds = products.stream().map(Product::getId).toList();
        Map<Long, List<ProductOffer>> offersByProduct = priceRepository
                .findCurrentOffersByProductIds(productIds)
                .stream()
                .collect(Collectors.groupingBy(ProductOffer::productId));

        // 5. Formatear como texto plano
        StringBuilder sb = new StringBuilder();
        for (Product product : products) {
            List<ProductOffer> offers = offersByProduct.get(product.getId());
            if (offers == null || offers.isEmpty()) {
                continue; // Sin precio vigente, no lo mostramos
            }

            sb.append("- ");
            sb.append(product.getName());
            if (product.getUnit() != null) {
                sb.append(" ").append(product.getUnit());
            }
            if (product.getBrand() != null) {
                sb.append(" | Marca: ").append(product.getBrand().getName());
            }
            sb.append(" | Precios: ");

            String priceList = offers.stream()
                    .map(o -> o.storeName() + " $" + o.value().toPlainString())
                    .collect(Collectors.joining(", "));
            sb.append(priceList);
            sb.append("\n");
        }

        return sb.isEmpty()
                ? "(No hay productos con precios disponibles en esta sección)"
                : sb.toString();
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
            default -> throw new IllegalArgumentException("Sección desconocida: " + slug);
        };
    }
}