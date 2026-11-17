package com.comprasco.bakeprofit.staging.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity 
@Getter 
@NoArgsConstructor 
@Table(name = "ean_size_overrides")
public class EanSizeOverride {
    @Id @Column(length = 14) 
    private String ean;

    @Column(precision = 10, scale = 2) 
    private java.math.BigDecimal netContent;

    @Enumerated(EnumType.STRING) @Column(length = 2) 
    private com.comprasco.bakeprofit.catalog.entity.UnitBase unitBase;
    
    @Column(columnDefinition = "text") 
    private String reason;
}
