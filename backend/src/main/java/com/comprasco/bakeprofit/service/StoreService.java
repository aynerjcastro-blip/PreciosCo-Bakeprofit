package com.comprasco.bakeprofit.service;

import com.comprasco.bakeprofit.entity.Store;
import com.comprasco.bakeprofit.entity.Category;
import com.comprasco.bakeprofit.dto.StoreResponse;
import com.comprasco.bakeprofit.exception.StoreAlreadyExistsException;
import com.comprasco.bakeprofit.exception.StoreNotFoundException;
import com.comprasco.bakeprofit.exception.InvalidCategoryHierarchyException;
import com.comprasco.bakeprofit.exception.InactiveParentException;
import com.comprasco.bakeprofit.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final CategoryService categoryService;

    public StoreService(StoreRepository storeRepository, CategoryService categoryService) {
        this.storeRepository = storeRepository;
        this.categoryService = categoryService;
    }

    /* CONSULTAS */

    public List<StoreResponse> findAll () {
        return storeRepository.findAll().stream()
                .map(StoreResponse::from)
                .toList();
    }

    public Store findById (Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new StoreNotFoundException("Tienda no encontrada con id: " + id));
    }

    public StoreResponse findByIdResponse (Long id) {
        return StoreResponse.from(findById(id));
    }

    public List<StoreResponse> searchByName(String name) {
        return storeRepository.findByNameContainingIgnoreCaseAndActiveTrue(name).stream()
                .map(StoreResponse::from)
                .toList();
    }

    public List<StoreResponse> findActive () {
        return storeRepository.findByActiveTrue().stream()
                .map(StoreResponse::from)
                .toList();
    }

    public List<StoreResponse> findInactive () {
        return storeRepository.findByActiveFalse().stream()
                .map(StoreResponse::from)
                .toList();
    }

    /* ESCRITURA */

    @Transactional
    public StoreResponse create (String name, Long categoryId) {
        if (storeRepository.existsByNameIgnoreCase(name)) {
            throw new StoreAlreadyExistsException(name);
        }

        Store store = new Store();
        store.setName(name);
        store.setCategory(resolveRootCategory(categoryId, "crear"));

        return StoreResponse.from(storeRepository.save(store));
    }

    @Transactional
    public StoreResponse update(Long id, String name, Long categoryId) {
        Store store = findById(id);

        if (storeRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new StoreAlreadyExistsException(name);
        }
        
        store.setName(name);
        store.setCategory(resolveRootCategory(categoryId, "actualizar"));

        return StoreResponse.from(storeRepository.save(store));
    }

    @Transactional
    public void deactivateStore (Long id) {
        Store store = findById(id);
        store.setActive(false);

        storeRepository.save(store);
    }

    @Transactional
    public void activateStore (Long id) {
        Store store = findById(id);

        if (!Boolean.TRUE.equals(store.getCategory().getActive())) {
            throw new InactiveParentException(
                    "No se puede activar la tienda '" + store.getName() +
                    "' porque su categoría raíz '" + store.getCategory().getName() + "' está inactiva.");
        }

        store.setActive(true);

        storeRepository.save(store);
    }

    private Category resolveRootCategory (Long categoryId, String action) {
        Category category = categoryService.findById(categoryId);
        if (category.getParent() != null) {
            throw new InvalidCategoryHierarchyException("La categoría de una tienda debe ser una categoría raíz");
        }
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new InactiveParentException(
                    "No se puede " + action + " la tienda " +
                    "porque su categoría raíz '" + category.getName() + "' está inactiva.");
        }
        return category;
    }
}