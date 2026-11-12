import { useState } from 'react';
import styles from './PriceRangeFilter.module.css';

const NEGATIVE_MESSAGE = 'El precio no puede ser negativo.';
const INVERTED_MESSAGE = 'El precio mínimo no puede ser mayor que el máximo.';

/**
 * Devuelve el error del rango o '' si es válido. Es la misma regla del backend (400);
 * revisarla aquí evita una petición que de antemano se sabe inválida.
 * @param {string} min
 * @param {string} max
 * @returns {string}
 */
function rangeError(min, max) {
    if ((min !== '' && Number(min) < 0) || (max !== '' && Number(max) < 0)) return NEGATIVE_MESSAGE;
    if (min !== '' && max !== '' && Number(min) > Number(max)) return INVERTED_MESSAGE;
    return '';
}

/**
 * Rango del precio vigente (HU-06b). Lo escrito es un borrador: se aplica con el botón o con
 * Enter, para no buscar con cada tecla ni con números a medio escribir ("1" mientras se
 * escribe "15000"). Los límites son inclusivos y se puede dejar cualquiera vacío.
 *
 * @param {{min: string, max: string, onApply: (min: string, max: string) => void}} props
 *        min y max son los valores aplicados, tal como están en la URL ('' si no hay)
 */
export default function PriceRangeFilter({ min, max, onApply }) {
    const [draftMin, setDraftMin] = useState(min);
    const [draftMax, setDraftMax] = useState(max);
    const [error, setError] = useState('');

    // Si la URL cambia por fuera (Atrás, Adelante o "Quitar precio"), el borrador la sigue.
    // Se ajusta durante el render, sin setState en un efecto (regla de react-hooks 7).
    const [appliedMin, setAppliedMin] = useState(min);
    const [appliedMax, setAppliedMax] = useState(max);
    if (min !== appliedMin || max !== appliedMax) {
        setAppliedMin(min);
        setAppliedMax(max);
        setDraftMin(min);
        setDraftMax(max);
        setError('');
    }

    const hasChanges = draftMin !== min || draftMax !== max;
    const hasRange = min !== '' || max !== '';

    function handleSubmit(event) {
        event.preventDefault();
        const message = rangeError(draftMin, draftMax);
        setError(message);
        if (!message) onApply(draftMin, draftMax);
    }

    return (
        <form className={styles['price-range']} onSubmit={handleSubmit} noValidate>
            <span className={styles['price-range__label']} id="price-range-label">Precio</span>
            <div className={styles['price-range__fields']} role="group" aria-labelledby="price-range-label">
                <label className={styles['price-range__field']}>
                    <span className={styles['price-range__hint']}>Desde</span>
                    <input
                        type="number"
                        inputMode="numeric"
                        min="0"
                        step="any"
                        placeholder="$ 0"
                        value={draftMin}
                        onChange={(event) => setDraftMin(event.target.value)}
                        aria-invalid={error !== ''}
                        aria-describedby={error ? 'price-range-error' : undefined}
                        className={styles['price-range__input']}
                    />
                </label>
                <label className={styles['price-range__field']}>
                    <span className={styles['price-range__hint']}>Hasta</span>
                    <input
                        type="number"
                        inputMode="numeric"
                        min="0"
                        step="any"
                        placeholder="Sin límite"
                        value={draftMax}
                        onChange={(event) => setDraftMax(event.target.value)}
                        aria-invalid={error !== ''}
                        aria-describedby={error ? 'price-range-error' : undefined}
                        className={styles['price-range__input']}
                    />
                </label>
                <button type="submit" className={styles['price-range__apply']} disabled={!hasChanges}>
                    Aplicar precio
                </button>
                {hasRange && (
                    <button type="button" className={styles['price-range__clear']} onClick={() => onApply('', '')}>
                        Quitar precio
                    </button>
                )}
            </div>
            {error && (
                <p id="price-range-error" className={styles['price-range__error']} role="alert">
                    {error}
                </p>
            )}
        </form>
    );
}
