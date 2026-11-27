package com.comprasco.bakeprofit.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comprasco.bakeprofit.catalog.entity.Category;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Buscar por nombre (coincidencia parcial, sin distinguir mayúsculas/minúsculas)
    List<Category> findByNameContainingIgnoreCaseAndActiveTrueAndParentIsNotNull (String name);

    List<Category> findByActiveTrue ();

    List<Category> findByActiveFalse ();

    List<Category> findByActiveTrueAndParentIsNotNull ();

    List<Category> findByParentIdAndActiveTrue(Long parentId);

    List<Category> findByParentIsNullAndActiveTrue();

    List<Category> findByActiveFalseAndParentIsNotNull ();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByParentId(Long parentId);

    /** Categorías raíz: los dominios (Supermercado, Droguería, ...). */
    List<Category> findByParentIsNull ();

    /**
     * IDs de las subcategorías activas de la categoría raíz con ese nombre
     * (sin distinguir mayúsculas). Solo IDs, en una consulta: lo usa el
     * contexto del asistente de IA.
     */
    @Query("""
            SELECT c.id FROM Category c
            WHERE c.active = true
              AND c.parent.parent IS NULL
              AND LOWER(c.parent.name) = LOWER(:rootName)
            """)
    List<Long> findActiveChildIdsByRootName(@Param("rootName") String rootName);

    /** ID de una subcategoría activa por su nombre y el de su sección (sin distinguir mayúsculas). */
    @Query("""
            SELECT c.id FROM Category c
            WHERE c.active = true
              AND c.parent.parent IS NULL
              AND LOWER(c.parent.name) = LOWER(:rootName)
              AND LOWER(c.name) = LOWER(:name)
            """)
    java.util.Optional<Long> findActiveChildIdByRootAndName(@Param("rootName") String rootName,
            @Param("name") String name);

    /** ID de la categoría raíz (sección) con ese nombre, sin distinguir mayúsculas. */
    @Query("SELECT c.id FROM Category c WHERE c.parent IS NULL AND LOWER(c.name) = LOWER(:name)")
    java.util.Optional<Long> findRootIdByName(@Param("name") String name);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Category c SET c.active = :active WHERE c.parent.id = :parentId")
    int updateActiveByParentId(@Param("parentId") Long parentId, @Param("active") boolean active);
}
