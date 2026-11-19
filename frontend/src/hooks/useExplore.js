import { useEffect, useState } from 'react';
import { findBestSavings, searchProducts } from '@/api/products';
import { findActiveStores } from '@/api/stores';
import { useSections } from '@/hooks/useSection';

/*
 * Datos para la página Explorar.
 *
 * Cuatro fuentes en paralelo (Promise.allSettled para degradación elegante):
 *  1. Secciones (del caché de módulo de useSections: nunca duplica la petición)
 *  2. Best-savings cross-sección (sin sectionId → trae todas)
 *  3. Tiendas activas (sin sectionId → todas)
 *  4. Productos por tienda (top N tiendas, 4 productos cada una)
 *
 * Los productos se agrupan por tienda y por sección para las dos vistas de la página.
 */

/** Máximo de tiendas para las que traemos productos (evita N+1 masivo) */
const MAX_STORE_PRODUCTS = 6;
/** Productos por tienda */
const PRODUCTS_PER_STORE = 4;

/**
 * Pide 4 productos con mayor ahorro de una tienda.
 * @param {{id: number, name: string, category: string}} store
 * @param {AbortSignal} signal
 * @returns {Promise<{store, products: Array}>}
 */
async function fetchStoreProducts(store, signal) {
    const result = await searchProducts(
        { storeIds: [store.id], size: PRODUCTS_PER_STORE },
        { signal },
    );
    return { store, products: result.content ?? [] };
}

/**
 * @returns {{
 *   status: 'loading'|'ready'|'error',
 *   sections: Array,
 *   savingsBySection: Map<number, Array>,
 *   allSavings: Array,
 *   storeGroups: Array<{store: {id,name,category}, products: Array}>,
 *   stores: Array,
 * }}
 */
export function useExplore() {
    const { sections } = useSections();
    const [state, setState] = useState({
        status: 'loading',
        allSavings: [],
        savingsBySection: new Map(),
        storeGroups: [],
        stores: [],
    });

    useEffect(() => {
        const controller = new AbortController();

        Promise.allSettled([
            findBestSavings({ limit: 8 }, { signal: controller.signal }),
            findActiveStores({ signal: controller.signal }),
        ]).then(async ([savingsResult, storesResult]) => {
            if (controller.signal.aborted) return;

            const groups = savingsResult.status === 'fulfilled' ? savingsResult.value : [];
            const stores = storesResult.status === 'fulfilled' ? storesResult.value : [];

            // 1) Aplanar todos los productos con su sectionId para poder filtrar por tab
            const allProducts = groups.flatMap((g) =>
                g.products.map((p) => ({ ...p, sectionId: g.sectionId, sectionName: g.sectionName })),
            );
            allProducts.sort((a, b) => (b.savingsPct ?? 0) - (a.savingsPct ?? 0));

            // 2) Agrupar por sección
            const bySection = new Map();
            for (const p of allProducts) {
                if (!bySection.has(p.sectionId)) bySection.set(p.sectionId, []);
                bySection.get(p.sectionId).push(p);
            }

            // 3) Productos por tienda: top N tiendas, 4 productos cada una
            let storeGroups = [];
            const topStores = stores.slice(0, MAX_STORE_PRODUCTS);
            if (topStores.length > 0) {
                const storeResults = await Promise.allSettled(
                    topStores.map((s) => fetchStoreProducts(s, controller.signal)),
                );
                storeGroups = storeResults
                    .filter((r) => r.status === 'fulfilled' && r.value.products.length > 0)
                    .map((r) => r.value);
            }

            if (controller.signal.aborted) return;

            setState({
                status: 'ready',
                allSavings: allProducts,
                savingsBySection: bySection,
                storeGroups,
                stores,
            });
        });

        return () => controller.abort();
    }, []);

    return { ...state, sections };
}