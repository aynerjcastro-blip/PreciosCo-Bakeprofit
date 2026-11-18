package com.comprasco.bakeprofit.staging.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "store_sku_ean_map")
public class StoreSkuEanMap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long storeId;
    @Column(nullable = false, length = 100)
    private String storeSku;
    @Column(nullable = false, length = 14)
    private String ean;
    @Column(nullable = false, columnDefinition = "text")
    private String reason;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static StoreSkuEanMap learned(Long storeId, String storeSku, String ean) {
        StoreSkuEanMap mapping = new StoreSkuEanMap();
        mapping.storeId = Objects.requireNonNull(storeId);
        mapping.storeSku = Objects.requireNonNull(storeSku);
        mapping.ean = Objects.requireNonNull(ean);
        mapping.reason = "Coincidencia automática por marca, tamaño y nombre; revisar evidencia de la oferta";
        mapping.createdAt = LocalDateTime.now();
        return mapping;
    }
}
