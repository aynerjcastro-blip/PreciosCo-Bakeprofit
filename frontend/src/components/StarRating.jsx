import { useId, useState } from 'react';
import styles from './StarRating.module.css';

const STARS = [1, 2, 3, 4, 5];

// Estrella de 5 puntas (el relleno y el borde los define el CSS)
function StarIcon({ className }) {
    return (
        <svg className={className} viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
        </svg>
    );
}

/**
 * Estrellas de calificación.
 * - Sin `onChange`: solo muestra `value` (admite decimales, por ejemplo un promedio de 4,5).
 * - Con `onChange`: selector accesible (grupo de radios; se maneja con flechas del teclado).
 *
 * @param {number} value - calificación de 0 a 5
 * @param {function} [onChange] - recibe el puntaje elegido (1 a 5)
 * @param {'sm'|'md'} [size] - tamaño al mostrar
 * @param {string} [label] - texto para lectores de pantalla
 * @param {boolean} [decorative] - true si el valor ya se lee en el texto de al lado
 */
export default function StarRating({ value = 0, onChange, size = 'md', label, decorative = false }) {
    const groupName = useId();
    const [hovered, setHovered] = useState(0);

    // ---- Modo selector ----
    if (onChange) {
        const shown = hovered || value;
        return (
            <fieldset className={styles.picker}>
                <legend className={styles.picker__legend}>{label ?? 'Calificación'}</legend>
                <div className={styles.picker__stars} onMouseLeave={() => setHovered(0)}>
                    {STARS.map((n) => (
                        <label
                            key={n}
                            className={`${styles.picker__star} ${n <= shown ? styles['picker__star--on'] : ''} ${n === value ? styles['picker__star--selected'] : ''}`}
                            onMouseEnter={() => setHovered(n)}
                        >
                            <input
                                type="radio"
                                className={styles.picker__input}
                                name={groupName}
                                value={n}
                                checked={value === n}
                                onChange={() => onChange(n)}
                                aria-label={`${n} de 5`}
                            />
                            <StarIcon className={styles.picker__icon} />
                        </label>
                    ))}
                </div>
                <span className={styles.picker__hint} aria-live="polite">
                    {value ? `${value} de 5` : 'Elige de 1 a 5 estrellas'}
                </span>
            </fieldset>
        );
    }

    // ---- Modo solo lectura: una fila vacía y otra rellena recortada al porcentaje ----
    const percent = Math.max(0, Math.min(5, Number(value) || 0)) * 20;
    const a11y = decorative
        ? { 'aria-hidden': 'true' }
        : { role: 'img', 'aria-label': label ?? `${value} de 5` };

    return (
        <span className={`${styles.stars} ${styles[`stars--${size}`]}`} {...a11y}>
            <span className={styles.stars__row}>
                {STARS.map((n) => <StarIcon key={n} />)}
            </span>
            <span className={`${styles.stars__row} ${styles['stars__row--filled']}`} style={{ width: `${percent}%` }}>
                {STARS.map((n) => <StarIcon key={n} />)}
            </span>
        </span>
    );
}
