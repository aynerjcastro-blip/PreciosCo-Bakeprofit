package com.comprasco.bakeprofit.price.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import com.comprasco.bakeprofit.catalog.entity.Product;
import com.comprasco.bakeprofit.core.utils.TextLimits;
import com.comprasco.bakeprofit.store.entity.Store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Periodo durante el cual un producto tuvo el mismo precio en una tienda.
 *
 * <p>
 * {@code prices} es un historial por periodos: cada fila empieza cuando el precio
 * cambia ({@code registrationDate}) y {@code lastSeenAt} dice cuándo se vio por última
 * vez ese mismo valor. Si una oferta trae el mismo valor que el último precio del par
 * producto-tienda, solo se refrescan {@code lastSeenAt} y los enlaces; si trae otro,
 * se abre una fila nueva. {@code value}, {@code registrationDate}, {@code product} y
 * {@code store} nunca cambian: por eso no hay setters, y dos filas seguidas del mismo
 * par siempre tienen valores distintos, que es lo que compara el detector de bajas.
 * </p>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "prices",
    indexes = @Index(name = "idx_prices_product_store", columnList = "product_id, store_id"))
public class Price {

    /** Longitud de {@code prices.source} (VARCHAR(255)). */
    public static final int SOURCE_MAX_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal value;

    @Column(nullable = false)
    private LocalDateTime registrationDate;

    @Column(nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(length = SOURCE_MAX_LENGTH)
    private String source;

    // Enlace directo al producto en la tienda
    @Column(name = "product_url", columnDefinition = "text")
    private String productUrl;

    // Imagen del producto en la tienda
    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_prices_products"))
    private Product product;

    @ManyToOne
    @JoinColumn(name = "store_id", nullable = false, foreignKey = @ForeignKey(name = "fk_prices_stores"))
    private Store store;

    /** Abre un periodo nuevo: el precio se registra y se ve por primera vez ahora. */
    public static Price observed(Product product, Store store, BigDecimal value, String source,
                                 String productUrl, String imageUrl) {
        Price price = new Price();
        price.product = Objects.requireNonNull(product);
        price.store = Objects.requireNonNull(store);
        price.value = Objects.requireNonNull(value);
        price.source = TextLimits.truncate(source, SOURCE_MAX_LENGTH);
        price.productUrl = productUrl;
        price.imageUrl = imageUrl;
        price.registrationDate = LocalDateTime.now();
        price.lastSeenAt = price.registrationDate;
        return price;
    }

    /** Compara como número: 4500 y 4500.00 son el mismo precio. */
    public boolean hasValue(BigDecimal otherValue) {
        return value.compareTo(otherValue) == 0;
    }

    /**
     * El mismo precio se volvió a ver. Un enlace vacío no reemplaza al guardado: una
     * oferta sin imagen no debe borrar la que ya funcionaba.
     */
    public void refreshSighting(String newProductUrl, String newImageUrl) {
        this.lastSeenAt = LocalDateTime.now();
        if (hasText(newProductUrl)) {
            this.productUrl = newProductUrl;
        }
        if (hasText(newImageUrl)) {
            this.imageUrl = newImageUrl;
        }
    }

    private static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }
}