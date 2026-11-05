import { useState, useEffect } from 'react';
import { buildSavingsDeal } from '../utils/buildSavingsDeal';

const API_BASE = 'http://localhost:8080/api';

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
                const resSearch = await fetch(`${API_BASE}/products/search?name=${searchTerm}`);
                const products = await resSearch.json();
                
                if (!products.length || !isMounted) return;

                const product = products[0];
                const resPrices = await fetch(`${API_BASE}/prices/compare?productId=${product.id}`);
                const prices = await resPrices.json();

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
