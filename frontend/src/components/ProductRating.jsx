import { useState, useEffect } from 'react';
import RatingSummary from './RatingSummary';
import { getSummary } from '../api/reviews';

/**
 * Promedio general del producto (todas las tiendas), bajo el título del comparador.
 * Se vuelve a pedir cuando cambia `refreshKey` (alguien opinó, editó o eliminó).
 */
export default function ProductRating({ productId, refreshKey = 0 }) {
    const [summary, setSummary] = useState(undefined); // undefined = cargando, null = no disponible

    useEffect(() => {
        let cancelled = false;

        const loadSummary = async () => {
            try {
                const data = await getSummary({ productId });
                if (!cancelled) setSummary(data);
            } catch (err) {
                console.error(err);
                if (!cancelled) setSummary(null);
            }
        };

        loadSummary();
        return () => { cancelled = true; };
    }, [productId, refreshKey]);

    return <RatingSummary summary={summary} size="md" detailed centered />;
}
