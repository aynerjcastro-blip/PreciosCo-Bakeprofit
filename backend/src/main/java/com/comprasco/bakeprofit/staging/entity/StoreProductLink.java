package com.comprasco.bakeprofit.staging.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity 
@Getter 
@NoArgsConstructor 
@Table(name = "store_product_links")
public class StoreProductLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;

    private Long storeId;

    @Column(length = 100) 
    private String storeSku;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id")
    private com.comprasco.bakeprofit.catalog.entity.Product product;
    
    @Column(columnDefinition = "text") 
    private String reason;
}
