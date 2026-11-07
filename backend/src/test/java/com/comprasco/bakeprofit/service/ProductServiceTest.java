package com.comprasco.bakeprofit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.comprasco.bakeprofit.dto.ProductRequest;
import com.comprasco.bakeprofit.dto.ProductResponse;
import com.comprasco.bakeprofit.entity.Brand;
import com.comprasco.bakeprofit.entity.Category;
import com.comprasco.bakeprofit.entity.Product;
import com.comprasco.bakeprofit.entity.UnitBase;
import com.comprasco.bakeprofit.exception.InvalidProductIdentityException;
import com.comprasco.bakeprofit.exception.ProductAlreadyExistsException;
import com.comprasco.bakeprofit.repository.BrandRepository;
import com.comprasco.bakeprofit.repository.ProductRepository;

/**
 * Reglas de identidad de ProductService (HT-09). BrandService es real sobre un
 * repositorio simulado, para comprobar que una marca existente se reutiliza.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Long CATEGORY_ID = 3L;
    private static final String EAN = "7702552000141";
    private static final String GTIN_14 = "07702552000141";
    private static final BigDecimal ONE_KILO = new BigDecimal("1000");
    private static final String UNIQUE_VIOLATION_SQLSTATE = "23505";
    private static final String CHECK_VIOLATION_SQLSTATE = "23514";

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryService categoryService;
    @Mock
    private BrandRepository brandRepository;

    private ProductService productService;
    private Category leafCategory;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, categoryService, new BrandService(brandRepository));

        Category root = new Category();
        root.setName("Supermercado");
        leafCategory = new Category();
        leafCategory.setName("Granos y cereales");
        leafCategory.setParent(root);
        when(categoryService.findById(CATEGORY_ID)).thenReturn(leafCategory);
    }

    private static ProductRequest request(String brand, String ean, BigDecimal content,
                                          UnitBase unit, String reference) {
        return new ProductRequest("Arroz Diana 1 kg", "1 kg", CATEGORY_ID, brand, ean, content, unit, reference);
    }

    private void saveReturnsTheProduct() {
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(call -> call.getArgument(0));
    }

    // Lo que Spring lanza cuando PostgreSQL rechaza el INSERT: Hibernate ya extrajo el nombre
    private static DataIntegrityViolationException violationOf(String constraint, String sqlState) {
        SQLException sqlException = new SQLException("violates constraint \"" + constraint + "\"", sqlState);
        return new DataIntegrityViolationException("could not execute statement",
                new ConstraintViolationException("could not execute statement", sqlException, constraint));
    }

    @Test
    void contenidoSinUnidadResponde400() {
        ProductRequest request = request(null, null, ONE_KILO, null, null);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(InvalidProductIdentityException.class);
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void referenciaSinMarcaResponde400() {
        ProductRequest request = request(null, null, null, null, "ABC-123");

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(InvalidProductIdentityException.class);
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void eanRepetidoResponde409() {
        Product existing = new Product();
        existing.setId(10L);
        existing.setName("Arroz Diana 1000 g");
        when(productRepository.findByEan(GTIN_14)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> productService.create(request(null, EAN, ONE_KILO, UnitBase.G, null)))
                .isInstanceOf(ProductAlreadyExistsException.class)
                .hasMessageContaining(EAN);
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void upcAYSuEan13SonElMismoProducto() {
        Product existing = new Product();
        existing.setId(11L);
        existing.setName("Chicle");
        // Guardado desde el EAN-13 "0036000291452"; llega como UPC-A de 12 dígitos
        when(productRepository.findByEan("00036000291452")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> productService.create(request(null, "036000291452", null, null, null)))
                .isInstanceOf(ProductAlreadyExistsException.class);
    }

    @Test
    void digitoVerificadorIncorrectoResponde400() {
        assertThatThrownBy(() -> productService.create(request(null, "7702552000142", null, null, null)))
                .isInstanceOf(InvalidProductIdentityException.class)
                .hasMessage("El código 7702552000142 no es un EAN válido: revisa el dígito verificador");
        verify(productRepository, never()).findByEan(anyString());
    }

    @Test
    void eanDeCirculacionRestringidaResponde400() {
        // Válido según el módulo 10, pero con prefijo 2: código interno de la tienda
        assertThatThrownBy(() -> productService.create(request(null, "2000123456782", null, null, null)))
                .isInstanceOf(InvalidProductIdentityException.class)
                .hasMessage("El código 2000123456782 es de uso interno de una tienda "
                        + "y no identifica el producto entre tiendas");
        verify(productRepository, never()).findByEan(anyString());
    }

    @Test
    void eanDePurosCerosResponde400() {
        assertThatThrownBy(() -> productService.create(request(null, "0000000000000", null, null, null)))
                .isInstanceOf(InvalidProductIdentityException.class)
                .hasMessage("El código 0000000000000 es un valor de relleno y no corresponde a un producto real");
    }

    @Test
    void eanDePesoVariableResponde400() {
        assertThatThrownBy(() -> productService.create(request(null, "90012345678908", null, null, null)))
                .isInstanceOf(InvalidProductIdentityException.class)
                .hasMessage("El código 90012345678908 corresponde a un producto de peso variable "
                        + "y su precio no es comparable entre tiendas");
        verify(productRepository, never()).findByEan(anyString());
    }

    @Test
    void elEanSeGuardaConCatorceDigitosYSeMuestraConTrece() {
        saveReturnsTheProduct();

        ProductResponse response = productService.create(request(null, EAN, ONE_KILO, UnitBase.G, null));

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEan()).isEqualTo(GTIN_14);
        assertThat(response.ean()).isEqualTo(EAN);
    }

    @Test
    void choqueSimultaneoEnElIndiceDeEanResponde409() {
        doThrow(violationOf("uq_products_ean", UNIQUE_VIOLATION_SQLSTATE))
                .when(productRepository).saveAndFlush(any(Product.class));

        assertThatThrownBy(() -> productService.create(request(null, EAN, ONE_KILO, UnitBase.G, null)))
                .isInstanceOf(ProductAlreadyExistsException.class);
    }

    @Test
    void choqueSimultaneoDetectadoSoloPorElMensajeDePostgresResponde409() {
        // Sin ConstraintViolationException de Hibernate: queda el SQLState 23505 y el mensaje
        SQLException sqlException = new SQLException(
                "ERROR: duplicate key value violates unique constraint \"uq_products_brand_ref\"",
                UNIQUE_VIOLATION_SQLSTATE);
        doThrow(new DataIntegrityViolationException("could not execute statement", sqlException))
                .when(productRepository).saveAndFlush(any(Product.class));
        Brand bosch = Brand.named("Bosch");
        when(brandRepository.findByNameMatchingIndex("Bosch")).thenReturn(Optional.of(bosch));

        assertThatThrownBy(() -> productService.create(request("Bosch", null, null, null, "ABC-123")))
                .isInstanceOf(ProductAlreadyExistsException.class);
    }

    @Test
    void violacionDeUnCheckNoSeTraduceA409() {
        DataIntegrityViolationException checkViolation = violationOf("chk_products_ean", CHECK_VIOLATION_SQLSTATE);
        doThrow(checkViolation).when(productRepository).saveAndFlush(any(Product.class));

        assertThatThrownBy(() -> productService.create(request(null, EAN, ONE_KILO, UnitBase.G, null)))
                .isSameAs(checkViolation);
    }

    @Test
    void marcaMasReferenciaRepetidaConOtraEscrituraResponde409() {
        Brand bosch = Brand.named("Bosch");
        when(brandRepository.findByNameMatchingIndex("Bosch")).thenReturn(Optional.of(bosch));
        // "ABC-123" ya está guardada; "abc.123" se normaliza a lo mismo antes de consultar
        when(productRepository.existsByBrandAndManufacturerRefNorm(bosch, "ABC123")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request("Bosch", null, null, null, "abc.123")))
                .isInstanceOf(ProductAlreadyExistsException.class);
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void marcaExistenteConOtrasMayusculasSeReutiliza() {
        Brand diana = Brand.named("Diana");
        when(brandRepository.findByNameMatchingIndex("DIANA")).thenReturn(Optional.of(diana));
        saveReturnsTheProduct();

        ProductResponse response = productService.create(request("  DIANA ", EAN, ONE_KILO, UnitBase.G, null));

        // La BD no inserta "DIANA" (ON CONFLICT) y la búsqueda devuelve la "Diana" existente
        assertThat(response.brand()).isEqualTo("Diana");
        verify(brandRepository).insertIfAbsent("DIANA");
        verify(brandRepository, never()).save(any());
    }

    @Test
    void productoSinCamposNuevosSeCreaComoAntes() {
        saveReturnsTheProduct();

        ProductResponse response = productService.create(request(null, null, null, null, null));

        assertThat(response.name()).isEqualTo("Arroz Diana 1 kg");
        assertThat(response.category()).isEqualTo("Granos y cereales");
        assertThat(response.brand()).isNull();
        assertThat(response.ean()).isNull();
        verifyNoInteractions(brandRepository);
        verify(productRepository, never()).findByEan(anyString());
    }

    @Test
    void editarSinCamposDeIdentidadConservaToda() {
        Brand diana = Brand.named("Diana");
        Product stored = new Product();
        stored.setId(5L);
        stored.setName("Arroz");
        stored.setCategory(leafCategory);
        stored.setBrand(diana);
        stored.setEan(GTIN_14);
        stored.setNetContent(ONE_KILO);
        stored.setUnitBase(UnitBase.G);
        when(productRepository.findById(5L)).thenReturn(Optional.of(stored));
        saveReturnsTheProduct();

        ProductResponse response = productService.update(5L, request(null, null, null, null, null));

        assertThat(response.name()).isEqualTo("Arroz Diana 1 kg");
        assertThat(response.brand()).isEqualTo("Diana");
        assertThat(response.ean()).isEqualTo(EAN);
        assertThat(response.netContent()).isEqualByComparingTo(ONE_KILO);
        assertThat(response.unitBase()).isEqualTo(UnitBase.G);
        verifyNoInteractions(brandRepository);
        verify(productRepository, never()).findByEan(anyString());
    }

    @Test
    void editarConUnCampoDeIdentidadReemplazaToda() {
        Product stored = new Product();
        stored.setId(5L);
        stored.setCategory(leafCategory);
        stored.setBrand(Brand.named("Diana"));
        stored.setEan(GTIN_14);
        when(productRepository.findById(5L)).thenReturn(Optional.of(stored));
        saveReturnsTheProduct();

        ProductResponse response = productService.update(5L, request(null, null, ONE_KILO, UnitBase.G, null));

        assertThat(response.brand()).isNull();
        assertThat(response.ean()).isNull();
        assertThat(response.netContent()).isEqualByComparingTo(ONE_KILO);
    }

    @Test
    void editarConSuPropioEanNoEsDuplicado() {
        Product stored = new Product();
        stored.setId(5L);
        stored.setCategory(leafCategory);
        stored.setEan(GTIN_14);
        when(productRepository.findById(5L)).thenReturn(Optional.of(stored));
        when(productRepository.findByEan(GTIN_14)).thenReturn(Optional.of(stored));
        saveReturnsTheProduct();

        ProductResponse response = productService.update(5L, request(null, EAN, ONE_KILO, UnitBase.G, null));

        assertThat(response.netContent()).isEqualByComparingTo(ONE_KILO);
    }
}