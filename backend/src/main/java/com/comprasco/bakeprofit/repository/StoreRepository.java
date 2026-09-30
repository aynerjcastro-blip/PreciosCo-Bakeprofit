package com.comprasco.bakeprofit.repository;

import com.comprasco.bakeprofit.entity.Store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByNameContainingIgnoreCaseAndActiveTrue (String name);

    List<Store> findByActiveTrue ();

    List<Store> findByActiveFalse ();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    
    boolean existsByCategoryId (Long categoryId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Store s SET s.active = :active WHERE s.category.id = :categoryId")
    int updateActiveByCategoryId(@Param("categoryId") Long categoryId, @Param("active") boolean active);
}