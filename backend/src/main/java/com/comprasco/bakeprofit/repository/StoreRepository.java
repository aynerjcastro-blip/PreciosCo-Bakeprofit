package com.comprasco.bakeprofit.repository;

import com.comprasco.bakeprofit.entity.Store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByNameContainingIgnoreCaseAndActiveTrue (String name);

    List<Store> findByActiveTrue ();

    List<Store> findByActiveFalse ();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Modifying
    @Query("UPDATE Store s SET s.active = :active WHERE s.category.id = :categoryId")
    int updateActiveByCategoryId(@Param("categoryId") Long categoryId, @Param("active") boolean active);
}