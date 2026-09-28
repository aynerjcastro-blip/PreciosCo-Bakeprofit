package com.comprasco.bakeprofit.repository;

import com.comprasco.bakeprofit.entity.Category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Buscar por nombre (coincidencia parcial, sin distinguir mayúsculas/minúsculas)
    List<Category> findByNameContainingIgnoreCaseAndActiveTrueAndParentIsNotNull (String name);

    List<Category> findByActiveTrue ();

    List<Category> findByActiveFalse ();

    List<Category> findByActiveTrueAndParentIsNotNull ();

    List<Category> findByActiveFalseAndParentIsNotNull ();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Modifying
    @Query("UPDATE Category c SET c.active = :active WHERE c.parent.id = :parentId")
    int updateActiveByParentId(@Param("parentId") Long parentId, @Param("active") boolean active);
}