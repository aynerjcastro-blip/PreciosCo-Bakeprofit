import styles from './StoreFilter.module.css';

/**
 * Selector de tiendas de la búsqueda (HU-06b). Se pueden elegir varias: un producto entra
 * si tiene precio vigente en alguna de ellas. Sin ninguna elegida se busca en todas las activas.
 *
 * @param {{stores: Array<{id: number, name: string}>, selectedIds: string[],
 *          counts?: Record<string, number>,
 *          onToggle: (id: string) => void, onClear: () => void}} props
 *        selectedIds son textos porque vienen de la URL; counts (opcional) es el número de
 *        productos del resultado que tiene cada tienda, por id, y solo se muestra si el backend lo entrega
 */
export default function StoreFilter({ stores, selectedIds, counts, onToggle, onClear }) {
    if (stores.length === 0) return null;

    return (
        <div className={styles['store-filter']} role="group" aria-labelledby="store-filter-label">
            <div className={styles['store-filter__head']}>
                <span id="store-filter-label" className={styles['store-filter__label']}>Tiendas</span>
                {selectedIds.length > 0 && (
                    <button type="button" className={styles['store-filter__clear']} onClick={onClear}>
                        Todas
                    </button>
                )}
            </div>

            <ul className={styles['store-filter__options']}>
                {stores.map((store) => {
                    const id = String(store.id);
                    const count = counts?.[id];
                    return (
                        <li key={id}>
                            <label className={styles['store-filter__option']}>
                                <input
                                    type="checkbox"
                                    checked={selectedIds.includes(id)}
                                    onChange={() => onToggle(id)}
                                />
                                <span className={styles['store-filter__name']}>{store.name}</span>
                                {count !== undefined && <span className={styles['store-filter__count']}>{count}</span>}
                            </label>
                        </li>
                    );
                })}
            </ul>
        </div>
    );
}