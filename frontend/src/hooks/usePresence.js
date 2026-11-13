import { useEffect, useState } from 'react';

/**
 * Mantiene montado un panel el tiempo justo para que se vea su animación de salida.
 *
 * - `mounted`: renderizar el panel solo si es true.
 * - `closing`: true mientras el panel se está yendo; sirve para elegir la animación de salida.
 *
 * Con "reducir movimiento" no hay animación y el panel se desmonta de inmediato.
 *
 * @param {boolean} open si el panel debe estar abierto
 * @param {number} exitMs lo que dura la animación de salida (debe coincidir con el CSS)
 * @returns {{mounted: boolean, closing: boolean}}
 */
export function usePresence(open, exitMs = 150) {
    const [kept, setKept] = useState(open);

    // Al abrir, el panel se monta en el mismo render (ajuste durante el render, sin efecto)
    if (open && !kept) {
        setKept(true);
    }

    useEffect(() => {
        if (open || !kept) return undefined;

        const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        const timer = setTimeout(() => setKept(false), reduced ? 0 : exitMs);
        return () => clearTimeout(timer);
    }, [open, kept, exitMs]);

    return { mounted: open || kept, closing: !open && kept };
}