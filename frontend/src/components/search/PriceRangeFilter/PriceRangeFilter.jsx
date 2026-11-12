import { useState } from 'react';
import styles from './PriceRangeFilter.module.css';

const NEGATIVE_MESSAGE = 'El precio no puede ser negativo.';
const INVERTED_MESSAGE = 'El precio mínimo no puede ser mayor que el máximo.';

const formatCop = (value) => `$${Number(value).toLocaleString('es-CO')}`;

/**
 * Rangos rápidos. Son un atajo para llenar mínimo y máximo, no otra regla: los límites son
 * inclusivos, así que un precio igual a un corte entra en los dos rangos vecinos.
 * Los cortes son los de un supermercado; se ajustan aquí cuando el catálogo cambie de escala.
 * min o max vacío = sin ese límite (mismas cadenas que viajan en la URL).
 */
const PRICE_PRESETS = [
    { id: 'hasta-10', label: `Hasta ${formatCop(10000)}`, min: '', max: '10000' },
    { id: '10-a-30', label: `${formatCop(10000)} a ${formatCop(30000)}`, min: '10000', max: '30000' },
    { id: 'mas-de-30', label: `Más de ${formatCop(30000)}`, min: '30000', max: '' },
];

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

function ArrowIcon() {
    return (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="m9 6 6 6-6 6" />
        </svg>
    );
}

/**
 * Rango del precio vigente (HU-06b). Tiene dos formas de elegirlo: un rango rápido, que se
 * aplica al instante, o los campos Mínimo y Máximo, que son un borrador y se aplican con el
 * botón o con Enter, para no buscar con cada tecla ni con números a medio escribir ("1"
 * mientras se escribe "15000"). Los límites son inclusivos y se puede dejar cualquiera vacío.
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

    // Un rango rápido siempre es válido: se aplica sin pasar por el borrador
    function applyPreset(preset) {
        setError('');
        onApply(preset.min, preset.max);
    }

    return (
        <form className={styles['price-range']} onSubmit={handleSubmit} noValidate>
            <span className={styles['price-range__label']} id="price-range-label">Precio</span>

            <ul className={styles['price-range__presets']} aria-labelledby="price-range-label">
                {PRICE_PRESETS.map((preset) => {
                    const active = preset.min === min && preset.max === max;
                    return (
                        <li key={preset.id}>
                            <button
                                type="button"
                                className={`${styles['price-range__preset']} ${active ? styles['price-range__preset--active'] : ''}`}
                                aria-pressed={active}
                                onClick={() => applyPreset(preset)}
                            >
                                {preset.label}
                            </button>
                        </li>
                    );
                })}
            </ul>

            <div className={styles['price-range__fields']} role="group" aria-label="Rango personalizado">
                <label className={styles['price-range__field']}>
                    <span className={styles['visually-hidden']}>Precio mínimo</span>
                    <input
                        type="number"
                        inputMode="numeric"
                        min="0"
                        step="any"
                        placeholder="Mínimo"
                        value={draftMin}
                        onChange={(event) => setDraftMin(event.target.value)}
                        aria-invalid={error !== ''}
                        aria-describedby={error ? 'price-range-error' : undefined}
                        className={styles['price-range__input']}
                    />
                </label>
                <span className={styles['price-range__dash']} aria-hidden="true">—</span>
                <label className={styles['price-range__field']}>
                    <span className={styles['visually-hidden']}>Precio máximo</span>
                    <input
                        type="number"
                        inputMode="numeric"
                        min="0"
                        step="any"
                        placeholder="Máximo"
                        value={draftMax}
                        onChange={(event) => setDraftMax(event.target.value)}
                        aria-invalid={error !== ''}
                        aria-describedby={error ? 'price-range-error' : undefined}
                        className={styles['price-range__input']}
                    />
                </label>
                <button
                    type="submit"
                    className={styles['price-range__apply']}
                    disabled={!hasChanges}
                    aria-label="Aplicar precio"
                >
                    <ArrowIcon />
                </button>
            </div>

            {error && (
                <p id="price-range-error" className={styles['price-range__error']} role="alert">
                    {error}
                </p>
            )}

            {hasRange && (
                <button type="button" className={styles['price-range__clear']} onClick={() => onApply('', '')}>
                    Quitar precio
                </button>
            )}
        </form>
    );
}