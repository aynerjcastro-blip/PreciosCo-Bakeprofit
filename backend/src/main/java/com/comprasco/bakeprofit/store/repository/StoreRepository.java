package com.comprasco.bakeprofit.store.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.comprasco.bakeprofit.store.entity.Store;

import java.util.List;


public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByNameContainingIgnoreCaseAndActiveTrue (String name);

    // La categoría (EAGER) se trae en la misma consulta: sin el entity graph había una
    // consulta extra por cada sección distinta (N+1) contra la BD remota.
    @EntityGraph(attributePaths = {"category", "category.parent"})
    List<Store> findByActiveTrue ();

    @EntityGraph(attributePaths = {"category", "category.parent"})
    List<Store> findByActiveTrueAndCategoryId(Long categoryId);

    List<Store> findByActiveFalse ();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    
    boolean existsByCategoryId (Long categoryId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Store s SET s.active = :active WHERE s.category.id = :categoryId")
    int updateActiveByCategoryId(@Param("categoryId") Long categoryId, @Param("active") boolean active);
}
