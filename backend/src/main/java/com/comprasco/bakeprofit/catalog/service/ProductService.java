package com.comprasco.bakeprofit.catalog.service;

import com.comprasco.bakeprofit.auth.exception.InactiveParentException;
import com.comprasco.bakeprofit.catalog.dto.ProductRequest;
import com.comprasco.bakeprofit.catalog.dto.ProductResponse;
import com.comprasco.bakeprofit.catalog.dto.ProductSearchCriteria;
import com.comprasco.bakeprofit.catalog.entity.Brand;
import com.comprasco.bakeprofit.catalog.entity.Category;
import com.comprasco.bakeprofit.catalog.entity.Product;
import com.comprasco.bakeprofit.catalog.exception.InvalidCategoryHierarchyException;
import com.comprasco.bakeprofit.catalog.exception.InvalidProductIdentityException;
import com.comprasco.bakeprofit.catalog.exception.ProductAlreadyExistsException;
import com.comprasco.bakeprofit.catalog.exception.ProductNotFoundException;
import com.comprasco.bakeprofit.catalog.repository.ProductRepository;
import com.comprasco.bakeprofit.catalog.repository.ProductSpecifications;
import com.comprasco.bakeprofit.catalog.util.Gtin;
import com.comprasco.bakeprofit.catalog.util.ManufacturerReference;
import com.comprasco.bakeprofit.store.service.StoreService;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final String CONTENT_PAIR_MESSAGE =
            "El contenido neto y la unidad base van juntos: envía los dos o ninguno.";
    private static final String REFERENCE_WITHOUT_BRAND_MESSAGE =
            "La referencia del fabricante exige una marca.";
    private static final String EMPTY_REFERENCE_MESSAGE =
            "La referencia del fabricante debe tener al menos una letra o un número.";
    private static final String DUPLICATE_EAN_MESSAGE =
            "Ya existe un producto con el EAN %s: '%s'.";
    private static final String DUPLICATE_REFERENCE_MESSAGE =
            "Ya existe un producto de la marca '%s' con la referencia '%s'.";
    private static final String DUPLICATE_IDENTITY_MESSAGE =
            "Ya existe un producto con el mismo EAN o con la misma marca y referencia del fabricante.";

    // Restricciones de identidad del script: solo su violación es un producto repetido (409)
    private static final String UQ_PRODUCTS_EAN = "uq_products_ean";
    private static final String UQ_PRODUCTS_BRAND_REF = "uq_products_brand_ref";
    private static final Set<String> IDENTITY_CONSTRAINTS = Set.of(UQ_PRODUCTS_EAN, UQ_PRODUCTS_BRAND_REF);
    // Respaldo si Hibernate no extrae el nombre: SQLState unique_violation y nombre entre comillas
    private static final String UNIQUE_VIOLATION_SQLSTATE = "23505";
    private static final Pattern CONSTRAINT_IN_MESSAGE = Pattern.compile("constraint \"([^\"]+)\"");

    /** Tamaño máximo de una página de la búsqueda (el mismo límite que las opiniones). */
    public static final int MAX_PAGE_SIZE = 50;
    private static final Sort SEARCH_ORDER = Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final StoreService storeService;

    public ProductService (ProductRepository productRepository,
                           CategoryService categoryService,
                           BrandService brandService,
                           StoreService storeService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.storeService = storeService;
    }

    public List<ProductResponse> findAll () {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    public Product findById (Long id) {
        return productRepository.findById(id).
                orElseThrow(() -> new ProductNotFoundException("Producto no encontrado con id: " + id));
    }

    public ProductResponse findByIdResponse (Long id) {
        return ProductResponse.from(findById(id));
    }

    public List<ProductResponse> findActive () {
        return productRepository.findByActiveTrue().stream()
                .map(ProductResponse::from)
                .toList();
    }

    public List<ProductResponse> findInactive () {
        return productRepository.findByActiveFalse().stream()
                .map(ProductResponse::from)
                .toList();
    }

    /**
     * Busca productos activos con los filtros de HU-06a (nombre y categoría) y HU-06b
     * (tiendas y rango del precio vigente). Todos se resuelven en SQL, así que cada página
     * está completa y el total es exacto.
     *
     * <p>
     * Orden: categoría inexistente (404) -> tienda inexistente o inactiva (404) -> consulta.
     * El rango ya llega validado en los filtros (400). El cliente no elige el orden: por
     * nombre y, si empatan, por id, para que las páginas no repitan ni salten productos.
     * </p>
     */
    public PagedModel<ProductResponse> search (ProductSearchCriteria criteria, int page, int size) {
        if (criteria.categoryId() != null) {
            categoryService.findById(criteria.categoryId());
        }
        storeService.requireActive(criteria.storeIds());

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                SEARCH_ORDER);

        return new PagedModel<>(productRepository
                .findAll(ProductSpecifications.matching(criteria), pageable)
                .map(ProductResponse::from));
    }

    /**
     * Registra un producto. Orden: categoría (404/400/409) -> identidad incoherente (400)
     * -> EAN o marca + referencia repetidos (409). Todo se comprueba antes de tocar la
     * entidad; el índice único de la BD es la segunda barrera.
     */
    @Transactional
    public ProductResponse create (ProductRequest request) {
        Category category = categoryService.findById(request.idCategory());

        if (category.getParent() == null) {
            throw new InvalidCategoryHierarchyException("Los productos no pueden pertencer a una categoría raíz");
        }

        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new InactiveParentException(
                    "No se puede crear el producto '" + request.name() +
                    "' porque la categoría '" + category.getName() + "' está inactiva.");
        }

        ResolvedIdentity identity = prepareIdentity(request, null);

        Product product = new Product();
        product.setName(request.name());
        product.setUnit(request.unit());
        product.setCategory(category);
        applyIdentity(product, request, identity);

        return ProductResponse.from(saveUnique(product));
    }

    /**
     * Actualiza un producto. Si la petición no trae ningún campo de identidad, la
     * identidad actual se conserva: así un cliente anterior a HT-09 no borra el EAN
     * al editar el nombre. Si trae alguno, la identidad se reemplaza completa.
     */
    @Transactional
    public ProductResponse update (Long id, ProductRequest request) {
        Product product = findById(id);
        Category category = categoryService.findById(request.idCategory());

        if (category.getParent() == null) {
            throw new InvalidCategoryHierarchyException("Los productos no pueden pertencer a una categoría raíz");
        }

        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new InactiveParentException(
                    "No se puede actualizar el producto '" + product.getName() +
                    "' porque la nueva categoría '" + category.getName() + "' está inactiva.");
        }

        if (carriesIdentity(request)) {
            ResolvedIdentity identity = prepareIdentity(request, id);
            applyIdentity(product, request, identity);
        }

        product.setName(request.name());
        product.setUnit(request.unit());
        product.setCategory(category);

        return ProductResponse.from(saveUnique(product));
    }

    @Transactional
    public void activateProduct (Long id) {
        Product product = findById(id);

        if (!Boolean.TRUE.equals(product.getCategory().getActive())) {
            throw new InactiveParentException(
                    "No se puede activar el producto '" + product.getName() +
                    "' porque su categoría '" + product.getCategory().getName() + "' está inactiva.");
        }

        product.setActive(true);

        productRepository.save(product);
    }

    @Transactional
    public void deactivateProduct (Long id) {
        Product product = findById(id);
        product.setActive(false);

        productRepository.save(product);
    }

    /* IDENTIDAD (HT-09) */

    /** Marca resuelta y EAN ya normalizado a GTIN-14, listos para asignar al producto. */
    private record ResolvedIdentity (Brand brand, String gtin) {
    }

    // Valida y comprueba duplicados con los datos de la petición; productId es null al crear
    private ResolvedIdentity prepareIdentity (ProductRequest request, Long productId) {
        validateIdentity(request);
        String gtin = Gtin.normalize(request.ean());
        ensureUniqueEan(gtin, request.ean(), productId);
        Brand brand = resolveBrand(request.brandName());
        ensureUniqueReference(brand, request.manufacturerRef(), productId);
        return new ResolvedIdentity(brand, gtin);
    }

    private void validateIdentity (ProductRequest request) {
        if (request.ean() != null) {
            Gtin.assess(request.ean()).rejectionMessage(request.ean()).ifPresent(message -> {
                throw new InvalidProductIdentityException(message);
            });
        }
        if ((request.netContent() == null) != (request.unitBase() == null)) {
            throw new InvalidProductIdentityException(CONTENT_PAIR_MESSAGE);
        }
        if (request.manufacturerRef() == null) {
            return;
        }
        if (request.brandName() == null) {
            throw new InvalidProductIdentityException(REFERENCE_WITHOUT_BRAND_MESSAGE);
        }
        if (ManufacturerReference.normalize(request.manufacturerRef()).isEmpty()) {
            throw new InvalidProductIdentityException(EMPTY_REFERENCE_MESSAGE);
        }
    }

    // Busca por el GTIN-14 normalizado; el mensaje muestra el código tal como lo envió el cliente
    private void ensureUniqueEan (String gtin, String requestedEan, Long productId) {
        if (gtin == null) {
            return;
        }
        productRepository.findByEan(gtin)
                .filter(existing -> !existing.getId().equals(productId))
                .ifPresent(existing -> {
                    throw new ProductAlreadyExistsException(
                            DUPLICATE_EAN_MESSAGE.formatted(requestedEan, existing.getName()));
                });
    }

    private void ensureUniqueReference (Brand brand, String reference, Long productId) {
        if (reference == null) {
            return;
        }
        String normalizedReference = ManufacturerReference.normalize(reference);
        boolean taken = productId == null
                ? productRepository.existsByBrandAndManufacturerRefNorm(brand, normalizedReference)
                : productRepository.existsByBrandAndManufacturerRefNormAndIdNot(brand, normalizedReference, productId);
        if (taken) {
            throw new ProductAlreadyExistsException(
                    DUPLICATE_REFERENCE_MESSAGE.formatted(brand.getName(), reference));
        }
    }

    private Brand resolveBrand (String brandName) {
        return brandName == null ? null : brandService.resolveByName(brandName);
    }

    private static void applyIdentity (Product product, ProductRequest request, ResolvedIdentity identity) {
        product.setBrand(identity.brand());
        product.setEan(identity.gtin());
        product.setNetContent(request.netContent());
        product.setUnitBase(request.unitBase());
        product.setManufacturerRef(request.manufacturerRef());
    }

    private static boolean carriesIdentity (ProductRequest request) {
        return request.brandName() != null
                || request.ean() != null
                || request.netContent() != null
                || request.unitBase() != null
                || request.manufacturerRef() != null;
    }

    private Product saveUnique (Product product) {
        try {
            return productRepository.saveAndFlush(product);
        } catch (DataIntegrityViolationException e) {
            // Dos peticiones simultáneas pasaron la pre-comprobación: gana el índice único de la BD.
            // Cualquier otra violación (un CHECK, una FK) es un error de programación y no se disfraza de 409
            if (violatesIdentityConstraint(e)) {
                throw new ProductAlreadyExistsException(DUPLICATE_IDENTITY_MESSAGE);
            }
            throw e;
        }
    }

    private static boolean violatesIdentityConstraint (DataIntegrityViolationException exception) {
        return violatedConstraintName(exception)
                .map(IDENTITY_CONSTRAINTS::contains)
                .orElse(false);
    }

    // Primero el nombre que extrae Hibernate; si no lo trae, el mensaje de PostgreSQL de una violación única
    private static Optional<String> violatedConstraintName (DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException hibernateViolation
                    && hibernateViolation.getConstraintName() != null) {
                return Optional.of(hibernateViolation.getConstraintName());
            }
            if (cause instanceof SQLException sqlException
                    && UNIQUE_VIOLATION_SQLSTATE.equals(sqlException.getSQLState())) {
                return constraintNamedIn(sqlException.getMessage());
            }
        }
        return Optional.empty();
    }

    private static Optional<String> constraintNamedIn (String message) {
        if (message == null) {
            return Optional.empty();
        }
        Matcher matcher = CONSTRAINT_IN_MESSAGE.matcher(message);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}