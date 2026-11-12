package com.comprasco.bakeprofit.store.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.comprasco.bakeprofit.catalog.service.CategoryService;
import com.comprasco.bakeprofit.store.entity.Store;
import com.comprasco.bakeprofit.store.exception.StoreNotFoundException;
import com.comprasco.bakeprofit.store.repository.StoreRepository;


/** Tiendas del filtro de la búsqueda (HU-06b): inexistente o inactiva responde 404. */
@ExtendWith(MockitoExtension.class)
class StoreServiceRequireActiveTest {

    @Mock
    private StoreRepository storeRepository;
    @Mock
    private CategoryService categoryService;

    private StoreService storeService;

    @BeforeEach
    void setUp() {
        storeService = new StoreService(storeRepository, categoryService);
    }

    private static Store store(Long id, boolean active) {
        Store store = new Store();
        ReflectionTestUtils.setField(store, "id", id);
        store.setActive(active);
        return store;
    }

    @Test
    void sinTiendasNoConsultaLaBase() {
        storeService.requireActive(Set.of());

        verifyNoInteractions(storeRepository);
    }

    @Test
    void tiendasActivasPasan() {
        when(storeRepository.findAllById(any())).thenReturn(List.of(store(1L, true), store(2L, true)));

        assertThatCode(() -> storeService.requireActive(Set.of(1L, 2L))).doesNotThrowAnyException();
    }

    @Test
    void tiendaInexistenteResponde404ConSuId() {
        when(storeRepository.findAllById(any())).thenReturn(List.of(store(1L, true)));

        assertThatThrownBy(() -> storeService.requireActive(Set.of(1L, 99L)))
                .isInstanceOf(StoreNotFoundException.class)
                .hasMessage("Tienda no encontrada o inactiva: 99");
    }

    @Test
    void tiendaInactivaResponde404ComoSiNoExistiera() {
        when(storeRepository.findAllById(any())).thenReturn(List.of(store(1L, true), store(4L, false)));

        assertThatThrownBy(() -> storeService.requireActive(Set.of(4L, 1L, 7L)))
                .isInstanceOf(StoreNotFoundException.class)
                .hasMessage("Tienda no encontrada o inactiva: 4, 7");
    }
}
