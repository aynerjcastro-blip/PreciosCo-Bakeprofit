package com.comprasco.bakeprofit.catalog.entity;

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

/**
 * Marca de un producto. El nombre es único sin distinguir mayúsculas: lo garantiza
 * el índice funcional {@code uq_brands_name} del script, que JPA no puede declarar.
 * Solo {@code BrandService} crea marcas.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "brands")
public class Brand {

    public static final int NAME_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    public static Brand named(String name) {
        Brand brand = new Brand();
        brand.name = Objects.requireNonNull(name);
        brand.registrationDate = LocalDateTime.now();
        return brand;
    }
}