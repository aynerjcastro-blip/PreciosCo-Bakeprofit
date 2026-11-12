package com.comprasco.bakeprofit.catalog.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comprasco.bakeprofit.catalog.entity.Brand;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    /**
     * Registra la marca si no existe otra con el mismo nombre sin tildes ni mayúsculas.
     *
     * <p>
     * {@code ON CONFLICT ((lower(f_unaccent(name))))} usa el índice único {@code uq_brands_name},
     * que tiene exactamente esa expresión: si cambia una, cambia la otra, o PostgreSQL no
     * infiere el índice y el INSERT falla. "ALQUERIA" no inserta si ya existe "Alquería".
     * Si otra transacción está insertando el mismo nombre, esta espera a que termine y
     * no inserta. No lanza excepción, así que la transacción que la llama sigue sana
     * (capturar una violación única la dejaría abortada en PostgreSQL).
     * </p>
     *
     * @return 1 si insertó la marca, 0 si ya existía
     */
    @Modifying
    @Query(value = "INSERT INTO brands (name) VALUES (:name) ON CONFLICT ((lower(f_unaccent(name)))) DO NOTHING",
           nativeQuery = true)
    int insertIfAbsent(@Param("name") String name);

    /**
     * Busca la marca con la misma expresión que el índice {@code uq_brands_name}: sin tildes
     * ni mayúsculas.
     *
     * <p>
     * No se usa {@code findByNameIgnoreCase} porque Spring Data lo traduce a
     * {@code upper(name) = upper(:name)}, y {@code upper} y {@code lower} no son
     * simétricos en Unicode (la sigma final griega "ς" y la "σ" comparten mayúscula
     * pero no minúscula). Si la búsqueda y {@link #insertIfAbsent(String)} comparan
     * distinto, el INSERT puede no insertar porque {@code lower} ya ve la marca
     * mientras la búsqueda con {@code upper} no la encuentra, o al revés, devolver una
     * marca que el índice considera distinta. Por la misma razón es una consulta nativa:
     * {@code f_unaccent} es una función propia de la base (SCRUM-140).
     * </p>
     */
    @Query(value = "SELECT * FROM brands WHERE lower(f_unaccent(name)) = lower(f_unaccent(:name))",
           nativeQuery = true)
    Optional<Brand> findByNameMatchingIndex(@Param("name") String name);
}