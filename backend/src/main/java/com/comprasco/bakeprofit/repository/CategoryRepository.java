package com.comprasco.bakeprofit.repository;

import com.comprasco.bakeprofit.entity.Category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Buscar por nombre (coincidencia parcial, sin distinguir mayúsculas/minúsculas)
    List<Category> findByNameContainingIgnoreCaseAndActiveTrue (String name);

    List<Category> findByActiveTrue ();

    List<Category> findByActiveFalse ();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}