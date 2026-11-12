package com.comprasco.bakeprofit.price.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.comprasco.bakeprofit.follow.repository.FollowRepository;
import com.comprasco.bakeprofit.notification.event.PriceRegisteredEvent;
import com.comprasco.bakeprofit.notification.service.NotificationService;


@ExtendWith(MockitoExtension.class)
class PriceDropListenerTest {

    private static final PriceRegisteredEvent EVENT = new PriceRegisteredEvent(55L, 7L, 1L);

    @Mock
    private FollowRepository followRepository;
    @Mock
    private NotificationService notificationService;

    private PriceDropListener listener;

    @BeforeEach
    void setUp() {
        listener = new PriceDropListener(followRepository, notificationService);
    }

    @Test
    void revisaCadaSeguimientoActivoDelProducto() {
        when(followRepository.findActiveIdsByProductId(7L)).thenReturn(List.of(10L, 11L));

        listener.onPriceRegistered(EVENT);

        verify(notificationService).detectForFollow(10L);
        verify(notificationService).detectForFollow(11L);
    }

    @Test
    void unErrorEnUnSeguimientoNoDetieneLosDemas() {
        when(followRepository.findActiveIdsByProductId(7L)).thenReturn(List.of(10L, 11L));
        when(notificationService.detectForFollow(10L)).thenThrow(new IllegalStateException("BD caída"));

        listener.onPriceRegistered(EVENT);

        verify(notificationService).detectForFollow(11L);
    }

    @Test
    void sinSeguimientosNoHaceNada() {
        when(followRepository.findActiveIdsByProductId(7L)).thenReturn(List.of());

        listener.onPriceRegistered(EVENT);

        verifyNoInteractions(notificationService);
    }
}
