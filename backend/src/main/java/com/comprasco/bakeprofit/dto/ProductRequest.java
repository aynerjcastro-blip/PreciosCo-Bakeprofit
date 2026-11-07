package com.comprasco.bakeprofit.dto;

import java.math.BigDecimal;

import com.comprasco.bakeprofit.entity.Brand;
import com.comprasco.bakeprofit.entity.Product;
import com.comprasco.bakeprofit.entity.UnitBase;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar o actualizar un producto.
 *
 * <p>
 * Los campos de identidad (marca, EAN, contenido neto, unidad base y referencia del
 * fabricante) son opcionales. Al actualizar: si la petición no trae ninguno, se
 * conserva la identidad que el producto ya tiene; si trae alguno, se reemplaza toda
 * la identidad y los campos omitidos quedan vacíos. Las reglas entre campos
 * (contenido con unidad, referencia con marca) y la evaluación del EAN son de negocio
 * y las aplica ProductService.
 * </p>
 */
@Schema(description = "Datos del producto. Al actualizar: sin ningún campo de identidad (brandName, ean, "
        + "netContent, unitBase, manufacturerRef) se conserva la identidad existente; con alguno, "
        + "se reemplaza toda la identidad y los campos omitidos quedan vacíos.")
public record ProductRequest(
    @Schema(description = "Nombre del producto", example = "Maiz crispeta 1kg")
    @NotBlank(message = "El nombre es obligatorio")
    String name,

    @Schema(description = "Cantidad de contenido del producto", example = "x5 o 100g o 250ml")
    @NotBlank(message = "La cantidad de unidades es obligatoria")
    String unit,

    @Schema(description = "Id de la categoría a la que pertenece", example = "1")
    @NotNull(message = "La categoria es obligatoria")
    Long idCategory,

    @Schema(description = "Marca del producto. Si no existe, se registra. Obligatoria si se envía la referencia del fabricante",
            example = "Diana", nullable = true)
    @Size(max = Brand.NAME_MAX_LENGTH, message = ProductRequest.BRAND_SIZE_MESSAGE)
    String brandName,

    @Schema(description = "Código de barras GTIN: EAN-8, UPC-A (12), EAN-13 o GTIN-14 del fabricante. "
            + "Se guarda con 14 dígitos; se rechazan los de relleno, los de uso interno de una tienda y los de peso variable",
            example = "7702552000141", nullable = true)
    @Pattern(regexp = ProductRequest.EAN_REGEX, message = ProductRequest.EAN_MESSAGE)
    String ean,

    @Schema(description = "Contenido neto expresado en la unidad base. Va siempre junto con unitBase",
            example = "1000", nullable = true)
    @DecimalMin(value = ProductRequest.NET_CONTENT_EXCLUSIVE_MINIMUM, inclusive = false,
                message = ProductRequest.NET_CONTENT_POSITIVE_MESSAGE)
    @Digits(integer = Product.NET_CONTENT_INTEGER_DIGITS, fraction = Product.NET_CONTENT_FRACTION_DIGITS,
            message = ProductRequest.NET_CONTENT_FORMAT_MESSAGE)
    BigDecimal netContent,

    @Schema(description = "Unidad base del contenido neto: G (gramos), ML (mililitros) o UN (unidades)",
            example = "G", nullable = true)
    UnitBase unitBase,

    @Schema(description = "Referencia o código del fabricante (repuestos). Exige marca",
            example = "0 986 494 525", nullable = true)
    @Size(max = Product.MANUFACTURER_REF_MAX_LENGTH, message = ProductRequest.MANUFACTURER_REF_SIZE_MESSAGE)
    String manufacturerRef
) {
    // Formato de entrada; el dígito verificador y la comparabilidad los evalúa Gtin.assess
    private static final String EAN_REGEX = "^([0-9]{8}|[0-9]{12,14})$";
    private static final String EAN_MESSAGE = "El EAN debe tener 8, 12, 13 o 14 dígitos";
    private static final String NET_CONTENT_EXCLUSIVE_MINIMUM = "0";
    private static final String NET_CONTENT_POSITIVE_MESSAGE = "El contenido neto debe ser mayor que cero";
    private static final String NET_CONTENT_FORMAT_MESSAGE =
            "El contenido neto admite hasta {integer} enteros y {fraction} decimales";
    private static final String BRAND_SIZE_MESSAGE = "La marca admite máximo {max} caracteres";
    private static final String MANUFACTURER_REF_SIZE_MESSAGE =
            "La referencia del fabricante admite máximo {max} caracteres";

    // Recorta antes de validar: "" o "   " cuentan como no enviados, no como marca o EAN vacíos
    public ProductRequest {
        brandName = blankToNull(brandName);
        ean = blankToNull(ean);
        manufacturerRef = blankToNull(manufacturerRef);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}