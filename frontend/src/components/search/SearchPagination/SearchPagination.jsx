import styles from './SearchPagination.module.css';

/**
 * Navegación entre páginas de resultados. No se muestra si todo cabe en una página.
 *
 * @param {{page: number, totalPages: number, onChange: (page: number) => void}} props
 *        page empieza en 1, como se muestra al usuario y en la URL
 */
export default function SearchPagination({ page, totalPages, onChange }) {
    if (totalPages <= 1) return null;

    // Una URL vieja puede apuntar más allá del final: "Anterior" lleva a la última página real
    const previous = Math.min(page - 1, totalPages);
    const isFirst = page <= 1;
    const isLast = page >= totalPages;

    return (
        <nav className={styles['pagination']} aria-label="Páginas de resultados">
            <button
                type="button"
                className={styles['pagination__button']}
                onClick={() => onChange(previous)}
                disabled={isFirst}
            >
                Anterior
            </button>
            <span className={styles['pagination__status']} aria-current="page">
                Página {page} de {totalPages}
            </span>
            <button
                type="button"
                className={styles['pagination__button']}
                onClick={() => onChange(page + 1)}
                disabled={isLast}
            >
                Siguiente
            </button>
        </nav>
    );
}
