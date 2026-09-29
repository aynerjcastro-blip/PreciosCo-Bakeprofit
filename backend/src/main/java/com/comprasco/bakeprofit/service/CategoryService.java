package com.comprasco.bakeprofit.service;

import com.comprasco.bakeprofit.entity.Category;
import com.comprasco.bakeprofit.exception.CategoryAlreadyExistsException;
import com.comprasco.bakeprofit.exception.CategoryNotFoundException;
import com.comprasco.bakeprofit.exception.InvalidCategoryHierarchyException;
import com.comprasco.bakeprofit.exception.InactiveParentException;
import com.comprasco.bakeprofit.repository.CategoryRepository;
import com.comprasco.bakeprofit.repository.ProductRepository;
import com.comprasco.bakeprofit.repository.StoreRepository;
import com.comprasco.bakeprofit.dto.CategoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    public CategoryService (CategoryRepository categoryRepository, ProductRepository productRepository,
                            StoreRepository storeRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
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

        if (parentId != null) {
            Category parent = findById(parentId);
            if (parent.getParent() != null) {
                throw new InvalidCategoryHierarchyException("La categoría padre debe ser una categoría raíz, no una subcategoría");
            }

            if (!Boolean.TRUE.equals(parent.getActive())) {
                throw new InactiveParentException(
                        "No se puede crear la categoría '" + name +
                        "' porque la categoría raíz '" + parent.getName() + "' está inactiva.");
            }

            category.setParent(parent);
        }
        category.setName(name);
    
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update (Long id, String name, Long parentId) {
        Category category = findById(id);

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new CategoryAlreadyExistsException(name);
        }

        if (parentId != null) {
            if (parentId.equals(id)) {
                throw new InvalidCategoryHierarchyException("Una categoría no puede ser su propia categoría padre");
            }

            Category parent = findById(parentId);
            if (parent.getParent() != null) {
                throw new InvalidCategoryHierarchyException("La categoría padre debe ser una categoría raíz, no una subcategoría");
            }

            if (!Boolean.TRUE.equals(parent.getActive())) {
                throw new InactiveParentException(
                        "No se puede actualizar la categoría '" + category.getName() +
                        "' porque la nueva categoría raíz '" + parent.getName() + "' está inactiva.");
            }

            if (categoryRepository.existsByParentId(id)) {
                throw new InvalidCategoryHierarchyException("No se puede asignar padre a una categoría que ya tiene subcategorías");
            }

            if (storeRepository.existsByCategoryId(id)) {
                throw new InvalidCategoryHierarchyException("No se puede asignar padre a una categoría que ya tiene tiendas en su dominio");
            }

            category.setParent(parent);
        }
        category.setName(name);

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void activateCategory (Long id) {
        Category category = findById(id);

        if (category.getParent() != null && !Boolean.TRUE.equals(category.getParent().getActive())) {
            throw new InactiveParentException(
                    "No se puede activar la categoría '" + category.getName() +
                    "' porque su categoría raíz '" + category.getParent().getName() + "' está inactiva.");
        }
        
        category.setActive(true);
        if (category.getParent() == null) {
            categoryRepository.updateActiveByParentId(id, true);        
            productRepository.updateActiveByCategoryParentId(id, true);
            storeRepository.updateActiveByCategoryId(id, true);
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
            storeRepository.updateActiveByCategoryId(id, false);
        } else {
            productRepository.updateActiveByCategoryId(id, false);                   
        }
        
        categoryRepository.save(category);
    }
}