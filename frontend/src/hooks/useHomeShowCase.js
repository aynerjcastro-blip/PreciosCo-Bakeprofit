import { useEffect, useState } from 'react';
import { searchProducts } from '../api/products';
import { findActiveStores } from '../api/stores';

/*
 * Datos REALES para el hero y el comparador de la landing:
 *  - los productos con más diferencia de precio entre tiendas (searchProducts ya llega ordenado
 *    por ahorro, DP-39, y trae las ofertas de cada tienda con precio, fecha y enlace);
 *  - las tiendas activas, para el listado de logos y el texto del hero.
 *
 * Se piden más productos de los que se muestran porque algunos no sirven para comparar (una sola
 * tienda o el mismo precio en todas). Si una de las dos peticiones falla, la otra se aprovecha:
 * Promise.allSettled espera a ambas sin lanzar error si una falla.
 */
const CANDIDATES = 12;
const SHOWN = 4;

export function useHomeShowcase() {
    const [state, setState] = useState({ status: 'loading', products: [], stores: [] });

    useEffect(() => {
        const controller = new AbortController();

        Promise.allSettled([
            searchProducts({ size: CANDIDATES }, { signal: controller.signal }),
            findActiveStores(),
        ]).then(([productsResult, storesResult]) => {
            if (controller.signal.aborted) return; // la página se cerró antes de recibir la respuesta

            const content = productsResult.status === 'fulfilled' ? productsResult.value.content : [];
            const products = content
                .filter((p) => (p.offers?.length ?? 0) >= 2 && Number(p.maxPrice) > Number(p.minPrice))
                .slice(0, SHOWN);
            const stores = storesResult.status === 'fulfilled' ? storesResult.value : [];

            setState({ status: 'ready', products, stores });
        });

        return () => controller.abort();
    }, []);

    return state;
}