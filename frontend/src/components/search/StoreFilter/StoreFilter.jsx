import styles from './StoreFilter.module.css';

/**
 * Selector de tiendas de la búsqueda (HU-06b). Se pueden elegir varias: un producto entra
 * si tiene precio vigente en alguna de ellas. Sin ninguna elegida se busca en todas las activas.
 *
 * @param {{stores: Array<{id: number, name: string}>, selectedIds: string[],
 *          onToggle: (id: string) => void, onClear: () => void}} props
 *        selectedIds son textos porque vienen de la URL
 */
export default function StoreFilter({ stores, selectedIds, onToggle, onClear }) {
    if (stores.length === 0) return null;

    const noneSelected = selectedIds.length === 0;

    return (
        <div className={styles['store-filter']} role="group" aria-labelledby="store-filter-label">
            <span id="store-filter-label" className={styles['store-filter__label']}>Tiendas</span>
            <div className={styles['store-filter__options']}>
                <button
                    type="button"
                    className={`${styles['store-filter__chip']} ${noneSelected ? styles['store-filter__chip--active'] : ''}`}
                    aria-pressed={noneSelected}
                    onClick={onClear}
                >
                    Todas
                </button>
                {stores.map((store) => {
                    const id = String(store.id);
                    const selected = selectedIds.includes(id);
                    return (
                        <button
                            key={id}
                            type="button"
                            className={`${styles['store-filter__chip']} ${selected ? styles['store-filter__chip--active'] : ''}`}
                            aria-pressed={selected}
                            onClick={() => onToggle(id)}
                        >
                            {store.name}
                        </button>
                    );
                })}
            </div>
        </div>
    );
}
