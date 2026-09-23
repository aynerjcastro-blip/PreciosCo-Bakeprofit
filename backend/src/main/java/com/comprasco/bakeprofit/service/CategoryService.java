package com.comprasco.bakeprofit.service;

import com.comprasco.bakeprofit.entity.Category;
import com.comprasco.bakeprofit.exception.CategoryAlreadyExistsException;
import com.comprasco.bakeprofit.exception.CategoryNotFoundException;
import com.comprasco.bakeprofit.repository.CategoryRepository;
import com.comprasco.bakeprofit.repository.ProductRepository;
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

    public List<Category> findAll () {
        return categoryRepository.findAll();
    }

    public List<Category> findActive () {
        return categoryRepository.findByActiveTrue();
    }

    public List<Category> findInactive () {
        return categoryRepository.findByActiveFalse();
    }

    public Category findById (Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Categoria no encontrada con id: " + id));
    }

    public List<Category> searchByName (String name) {
        return categoryRepository.findByNameContainingIgnoreCaseAndActiveTrue(name);
    }

    /* ESCRITURA */

    @Transactional
    public Category create (String name) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new CategoryAlreadyExistsException(name);
        }

        Category category = new Category();
        category.setName(name);

        return categoryRepository.save(category);
    }

    @Transactional
    public Category update (Long id, String name) {
        Category category = findById(id);

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new CategoryAlreadyExistsException(name);
        }

        category.setName(name);
        return categoryRepository.save(category);
    }

    @Transactional
    public void activateCategory (Long id) {
        Category category = findById(id);
        category.setActive(true);
        productRepository.updateActiveByCategoryId(id, true);

        categoryRepository.save(category);
    }

    @Transactional
    public void deactivateCategory (Long id) {
        Category category = findById(id);
        category.setActive(false);
        productRepository.updateActiveByCategoryId(id, false);

        categoryRepository.save(category);
    }
}