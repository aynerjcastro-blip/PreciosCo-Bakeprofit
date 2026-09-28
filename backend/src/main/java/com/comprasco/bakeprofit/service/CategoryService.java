package com.comprasco.bakeprofit.service;

import com.comprasco.bakeprofit.entity.Category;
import com.comprasco.bakeprofit.exception.CategoryAlreadyExistsException;
import com.comprasco.bakeprofit.exception.CategoryNotFoundException;
import com.comprasco.bakeprofit.repository.CategoryRepository;
import com.comprasco.bakeprofit.repository.ProductRepository;
import com.comprasco.bakeprofit.dto.CategoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService (CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    /* CONSULTAS */

    public List<CategoryResponse> findAll () {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public List<CategoryResponse> findActive () {
        return categoryRepository.findByActiveTrueAndParentIsNotNull().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public List<CategoryResponse> findInactive () {
        return categoryRepository.findByActiveFalseAndParentIsNotNull().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public Category findById (Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Categoria no encontrada con id: " + id));
    }

    public CategoryResponse findByIdResponse (Long id) {
        return CategoryResponse.from(findById(id));
    }

    public List<CategoryResponse> searchByName (String name) {
        return categoryRepository.findByNameContainingIgnoreCaseAndActiveTrueAndParentIsNotNull(name).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    /* ESCRITURA */

    @Transactional
    public CategoryResponse create (String name, Long parentId) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new CategoryAlreadyExistsException(name);
        }

        Category category = new Category();
        category.setName(name);
        if (parentId != null) {
            category.setParent(findById(parentId));
        }
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update (Long id, String name, Long parentId) {
        Category category = findById(id);

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new CategoryAlreadyExistsException(name);
        }

        category.setName(name);
        if (parentId != null) {
            category.setParent(findById(parentId));
        }
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void activateCategory (Long id) {
        Category category = findById(id);
        
        category.setActive(true);
        if (category.getParent() == null) {
            categoryRepository.updateActiveByParentId(id, true);        
            productRepository.updateActiveByCategoryParentId(id, true);
        } else {
            productRepository.updateActiveByCategoryId(id, true);       
        }
        
        categoryRepository.save(category);
    }

    @Transactional
    public void deactivateCategory (Long id) {
        Category category = findById(id);

        category.setActive(false);
        if (category.getParent() == null) {
            categoryRepository.updateActiveByParentId(id, false);        
            productRepository.updateActiveByCategoryParentId(id, false);
        } else {
            productRepository.updateActiveByCategoryId(id, false);                   
        }
        
        categoryRepository.save(category);
    }
}