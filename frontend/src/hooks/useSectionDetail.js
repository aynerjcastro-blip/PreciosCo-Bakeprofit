import { useEffect, useState } from 'react';
import { findActiveStores } from '@/api/stores';
import { findBestSavings } from '@/api/products';

/*
 * Datos extra para la página de sección rediseñada:
 *  - tiendas activas de esta sección (chips de confianza: "Comparamos en Farmatodo, La Rebaja…")
 *  - mejores ahorros por subcategoría (top savings + productos populares)
 *
 * Se dispara solo cuando la sección tiene id real (ya existe en la base).
 * Las dos peticiones van en paralelo con Promise.allSettled: si una falla, la otra
 * se aprovecha y el componente muestra lo que sí llegó.
 */

/**
 * @param {number|null} sectionId — id de la categoría raíz; null si la sección no existe aún
 * @returns {{stores: Array<{id: number, name: string}>, topSaving: object|null,
 *            popular: Array, detailStatus: 'idle'|'loading'|'ready'|'error'}}
 */
export function useSectionDetail(sectionId) {
    const [state, setState] = useState({
        sectionId: null,
        stores: [],
        topSaving: null,
        popular: [],
    });

    useEffect(() => {
        // Sin id real no hay nada que pedir (sección "Próximamente")
        if (!sectionId) return;

        const controller = new AbortController();

        Promise.allSettled([
            findActiveStores({ sectionId, signal: controller.signal }),
            findBestSavings({ sectionId, limit: 4 }, { signal: controller.signal }),
        ]).then(([storesResult, savingsResult]) => {
            if (controller.signal.aborted) return;

            const stores = storesResult.status === 'fulfilled' ? storesResult.value : [];

            // findBestSavings devuelve Array<BestSavingsGroup>, cada grupo con .products[]
            // Aplanamos todos los productos y ordenamos por mayor ahorro
            const groups = savingsResult.status === 'fulfilled' ? savingsResult.value : [];
            const allProducts = groups.flatMap((g) => g.products);
            allProducts.sort((a, b) => (b.savingsPct ?? 0) - (a.savingsPct ?? 0));

            // El de mayor ahorro absoluto (maxPrice - minPrice) como "ahorro destacado"
            const topSaving = allProducts.length > 0 ? allProducts[0] : null;

            // Los siguientes 4 como "productos populares" (excluyendo el destacado)
            const popular = allProducts.filter((p) => p.id !== topSaving?.id).slice(0, 4);

            setState({ sectionId, stores, topSaving, popular });
        });

        return () => controller.abort();
    }, [sectionId]);

    if (!sectionId) {
        return { detailStatus: 'idle', stores: [], topSaving: null, popular: [] };
    }

    if (state.sectionId !== sectionId) {
        return { detailStatus: 'loading', stores: [], topSaving: null, popular: [] };
    }

    return { ...state, detailStatus: 'ready' };
}