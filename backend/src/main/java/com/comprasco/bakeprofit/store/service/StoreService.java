package com.comprasco.bakeprofit.store.service;

import com.comprasco.bakeprofit.auth.exception.InactiveParentException;
import com.comprasco.bakeprofit.catalog.entity.Category;
import com.comprasco.bakeprofit.catalog.exception.InvalidCategoryHierarchyException;
import com.comprasco.bakeprofit.catalog.service.CategoryService;
import com.comprasco.bakeprofit.store.dto.StoreResponse;
import com.comprasco.bakeprofit.store.entity.Store;
import com.comprasco.bakeprofit.store.exception.StoreAlreadyExistsException;
import com.comprasco.bakeprofit.store.exception.StoreNotFoundException;
import com.comprasco.bakeprofit.store.repository.StoreRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StoreService {

    private static final String UNAVAILABLE_STORES_MESSAGE = "Tienda no encontrada o inactiva: %s";
    private static final String ID_SEPARATOR = ", ";

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

    /**
     * Comprueba que todas las tiendas existen y están activas; si no, 404 con los ids que
     * fallan. Para un visitante, una tienda inactiva no existe: por eso no responde 409.
     * Una sola consulta para todos los ids.
     */
    public void requireActive (Collection<Long> storeIds) {
        if (storeIds.isEmpty()) {
            return;
        }
        Set<Long> activeIds = storeRepository.findAllById(storeIds).stream()
                .filter(store -> Boolean.TRUE.equals(store.getActive()))
                .map(Store::getId)
                .collect(Collectors.toSet());
        List<Long> unavailable = storeIds.stream()
                .filter(id -> !activeIds.contains(id))
                .sorted()
                .toList();
        if (!unavailable.isEmpty()) {
            throw new StoreNotFoundException(UNAVAILABLE_STORES_MESSAGE.formatted(joinIds(unavailable)));
        }
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

    private static String joinIds (List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(ID_SEPARATOR));
    }
}
