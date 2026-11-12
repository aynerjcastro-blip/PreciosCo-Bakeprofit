import { useState, useEffect } from 'react';
import { buildSavingsDeal } from '../utils/buildSavingsDeal';
import { searchProducts } from '../api/products';
import { compareByProduct } from '../api/prices';

/**
 * Custom hook para manejar el ciclo de vida (fetch) del ahorro.
 * ¿Por qué? Evita que los componentes visuales se contaminen con llamadas a la API
 * y manejo de estados como 'isMounted' o 'isLoading'. El componente UI solo consume datos limpios.
 */
export function useSavingsDeal(searchTerm) {
    const [deal, setDeal] = useState(null);
    const [loadingState, setLoadingState] = useState({ searchTerm, isLoading: true });

    useEffect(() => {
        let isMounted = true;

        async function fetchRealDeal() {
            try {
                // La búsqueda es paginada (HU-06b): basta con el primer resultado
                const result = await searchProducts({ name: searchTerm, size: 1 });
                const product = result.content[0];

                if (!product || !isMounted) return;

                const prices = await compareByProduct(product.id);

                if (isMounted) {
                    const calculatedDeal = buildSavingsDeal(product, prices);
                    setDeal(calculatedDeal);
                }
            } catch (error) {
                console.error('Error cargando ahorro en vivo:', error);
            } finally {
                if (isMounted) setLoadingState({ searchTerm, isLoading: false });
            }
        }

        fetchRealDeal();
        return () => { isMounted = false; };
    }, [searchTerm]);

    const isLoading = loadingState.searchTerm !== searchTerm || loadingState.isLoading;
    return { deal, isLoading };
}
