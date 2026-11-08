package com.comprasco.bakeprofit.event;

/**
 * Se publica por cada fila nueva en {@code prices}, es decir, cada vez que el precio de
 * un producto cambió en una tienda (un precio repetido solo refresca la fila y no lo publica).
 * Es el punto donde HT-11 conecta el detector de bajas.
 *
 * <p>
 * Lo publica {@code StagingProcessor} fuera de toda transacción, después de que la
 * transacción de la oferta hizo commit y devolvió su conexión. Por eso:
 * </p>
 * <ul>
 *   <li>Se escucha con {@code @EventListener} y {@code @Transactional} propia (sin transacción
 *       exterior, Spring abre una nueva). Así hay una sola conexión a la vez (pool de 2, RP-04).</li>
 *   <li>No con {@code @TransactionalEventListener}: sin transacción activa no se ejecuta, salvo
 *       con {@code fallbackExecution = true}.</li>
 *   <li>No con {@code @Async}: tomaría una segunda conexión mientras el procesador sigue con la
 *       suya y dejaría sin conexión a las peticiones web.</li>
 *   <li>No dentro de la transacción del precio ({@code BEFORE_COMMIT}): en PostgreSQL un error en
 *       el aviso aborta la transacción y se perdería el precio.</li>
 * </ul>
 * <p>
 * Un error del listener se registra en el log y no cambia el estado de la oferta. Si la
 * aplicación se detiene entre el commit y la publicación, ese aviso se pierde.
 * </p>
 */
public record PriceRegisteredEvent(Long priceId, Long productId, Long storeId) {
}
