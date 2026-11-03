import StarRating from './StarRating';
import { formatScore } from '../utils/format-helper';
import styles from './RatingSummary.module.css';

/**
 * Promedio y cantidad de calificaciones: estrellas + "4,5 (7)".
 *
 * @param {object|null|undefined} summary - { averageScore, ratingCount };
 *        undefined = cargando, null = no disponible
 * @param {'sm'|'md'} [size]
 * @param {boolean} [detailed] - "4,2 de 5 (18 opiniones)" en vez de "4,2 (18)"
 * @param {boolean} [centered]
 * @param {function} [onClick] - si existe, el resumen es un botón
 * @param {string} [ariaLabel] - texto del botón para lectores de pantalla
 */
export default function RatingSummary({ summary, size = 'md', detailed = false, centered = false, onClick, ariaLabel }) {
    const Wrapper = onClick ? 'button' : 'div';
    const wrapperProps = onClick ? { type: 'button', onClick, 'aria-label': ariaLabel } : {};
    const classes = `${styles['rating-summary']} ${centered ? styles['rating-summary--centered'] : ''}`;

    let content;
    if (summary === undefined) {
        content = <span className={styles['rating-summary__muted']}>Cargando calificaciones...</span>;
    } else if (summary === null) {
        content = <span className={styles['rating-summary__muted']}>Calificaciones no disponibles</span>;
    } else if (summary.ratingCount === 0) {
        content = <span className={styles['rating-summary__muted']}>Sin calificaciones</span>;
    } else {
        const count = summary.ratingCount;
        content = (
            <>
                <StarRating value={Number(summary.averageScore)} size={size} decorative />
                <span className={styles['rating-summary__score']}>
                    {formatScore(summary.averageScore)}{detailed && ' de 5'}
                </span>
                <span className={styles['rating-summary__count']}>
                    ({count}{detailed && ` ${count === 1 ? 'opinión' : 'opiniones'}`})
                </span>
            </>
        );
    }

    return (
        <Wrapper className={classes} {...wrapperProps}>
            {content}
        </Wrapper>
    );
}
