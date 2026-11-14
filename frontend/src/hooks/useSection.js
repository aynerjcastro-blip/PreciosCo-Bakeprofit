import { useCallback, useEffect, useMemo, useState } from 'react';
import { getLandingStats } from '../api/landing';
import { SECTIONS, sectionKey } from '../config/sections';

/*
 * Caché de módulo: la variable vive mientras la pestaña esté abierta, fuera de cualquier
 * componente. Si la landing, la página de sección y el buscador piden las secciones,
 * hay UNA sola petición. Caduca a los 5 minutos para no mostrar cifras viejas.
 */
const CACHE_MS = 5 * 60 * 1000;
let cache = { promise: null, at: 0 };

function loadStats() {
    const expired = Date.now() - cache.at > CACHE_MS;
    if (!cache.promise || expired) {
        const promise = getLandingStats().catch((error) => {
            cache = { promise: null, at: 0 }; // un fallo no se guarda: el siguiente intento vuelve a pedir
            throw error;
        });
        cache = { promise, at: Date.now() };
    }
    return cache.promise;
}

// Une la configuración (textos) con los datos reales (id y conteos).
function buildSections(stats) {
    if (!stats) return [];
    return SECTIONS.map((config) => {
        const real = stats.sections.find((s) => sectionKey(s.name) === sectionKey(config.rootName));
        const productCount = real?.productCount ?? 0;
        return {
            ...config,
            id: real?.id ?? null,        // null si la categoría raíz aún no existe en la base
            productCount,
            storeCount: real?.storeCount ?? 0,
            available: productCount > 0, // "Próximamente" se decide con datos, no a mano
        };
    });
}

/**
 * @returns {{status: 'loading'|'ready'|'error', sections: Array, stats: object|null,
 *            error: Error|null, reload: () => void}}
 */
export function useSections() {
    // Un solo estado para status+datos+error: evita combinaciones imposibles (cargando Y con error)
    const [state, setState] = useState({ status: 'loading', stats: null, error: null });
    const [attempt, setAttempt] = useState(0);

    useEffect(() => {
        let cancelled = false; // la petición compartida no se puede abortar; ignoramos su resultado
        loadStats()
            .then((stats) => {
                if (!cancelled) setState({ status: 'ready', stats, error: null });
            })
            .catch((error) => {
                if (!cancelled) setState({ status: 'error', stats: null, error });
            });
        return () => {
            cancelled = true; // se ejecuta al desmontar o antes de repetir el efecto
        };
    }, [attempt]);

    // setState va en el manejador, no dentro del efecto (regla de lint de react-hooks 7)
    const reload = useCallback(() => {
        setState({ status: 'loading', stats: null, error: null });
        setAttempt((n) => n + 1);
    }, []);

    const sections = useMemo(() => buildSections(state.stats), [state.stats]);

    return { ...state, sections, reload };
}