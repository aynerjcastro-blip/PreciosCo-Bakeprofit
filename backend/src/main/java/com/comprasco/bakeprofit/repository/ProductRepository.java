package com.comprasco.bakeprofit.repository;

import com.comprasco.bakeprofit.entity.Product;
import com.comprasco.bakeprofit.entity.Category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCaseAndActiveTrue (String name);

    List<Product> findByCategoryAndActiveTrue (Category category);

    List<Product> findByNameContainingIgnoreCaseAndCategoryAndActiveTrue (String name, Category category);

    List<Product> findByActiveTrue ();

    List<Product> findByActiveFalse ();

    @Modifying
    @Query("UPDATE Product p SET p.active = :active WHERE p.category.id = :categoryId")
    int updateActiveByCategoryId(@Param("categoryId") Long categoryId, @Param("active") boolean active);

    @Modifying
    @Query("UPDATE Product p SET p.active = :active WHERE p.category.parent.id = :parentId")
    int updateActiveByCategoryParentId(@Param("parentId") Long parentId, @Param("active") boolean active);
}