import { useState, useEffect } from 'react';
import ReportDialog from './ReportDialog';
import StarRating from './StarRating';
import { getReviews } from '../api/reviews';
import { useAuth } from '../context/useAuth'; // Importar el hook del contexto
import { formatDate } from '../utils/format-helper';
import styles from './ReviewsSection.module.css';
const PAGE_SIZE = 10;

/**
 * Sección compartida de opiniones del producto, filtrable por tienda con chips.
 *
 * @param {number|string} productId
 * @param {{id: number, name: string}[]} stores - tiendas de la comparación
 * @param {number|null} selectedStoreId - null = todas las tiendas
 * @param {function} onSelectStore - recibe el id de la tienda (o null)
 * @param {number} refreshKey - al cambiar, se vuelve a pedir la primera página
 */
export default function ReviewsSection({ productId, stores, selectedStoreId, onSelectStore, refreshKey = 0 }) {
    // Obtenemos user e isAuthenticated desde el AuthContext reactivo
    const { user, isAuthenticated } = useAuth();

    // Solo el rol USER puede reportar
    const canReport = isAuthenticated && user?.role === 'USER';
    const selectedStore = stores.find((store) => store.id === selectedStoreId) ?? null;

    const [items, setItems] = useState([]);
    const [total, setTotal] = useState(0);
    const [page, setPage] = useState(0);
    const [status, setStatus] = useState('loading'); // 'loading' | 'error' | 'empty' | 'success'
    const [loadingMore, setLoadingMore] = useState(false);
    const [moreError, setMoreError] = useState(false);
    const [attempt, setAttempt] = useState(0);
    const [reporting, setReporting] = useState(null);

    // Primera página: se repite al cambiar de tienda, al refrescar o al reintentar
    useEffect(() => {
        let cancelled = false;

        const loadFirstPage = async () => {
            try {
                const data = await getReviews({
                    productId,
                    storeId: selectedStoreId ?? undefined,
                    page: 0,
                    size: PAGE_SIZE,
                });
                if (cancelled) return;
                setItems(data.content);
                setTotal(data.page?.totalElements ?? data.content.length);
                setPage(0);
                setMoreError(false);
                setStatus(data.content.length === 0 ? 'empty' : 'success');
            } catch (err) {
                console.error(err);
                if (!cancelled) setStatus('error');
            }
        };

        loadFirstPage();
        return () => { cancelled = true; };
    }, [productId, selectedStoreId, refreshKey, attempt]);

    const handleLoadMore = async () => {
        setLoadingMore(true);
        setMoreError(false);
        try {
            const data = await getReviews({
                productId,
                storeId: selectedStoreId ?? undefined,
                page: page + 1,
                size: PAGE_SIZE,
            });
            setItems((prev) => [...prev, ...data.content]);
            setTotal(data.page?.totalElements ?? total);
            setPage(page + 1);
        } catch (err) {
            console.error(err);
            setMoreError(true);
        } finally {
            setLoadingMore(false);
        }
    };

    const hasMore = items.length < total;

    return (
        <section id="opiniones" className={styles.reviews} aria-labelledby="opiniones-title">
            <h2 id="opiniones-title" className={styles.reviews__title}>Opiniones</h2>

            <div className={styles.reviews__chips} role="group" aria-label="Filtrar opiniones por tienda">
                <button
                    type="button"
                    className={`${styles.reviews__chip} ${selectedStoreId === null ? styles['reviews__chip--active'] : ''}`}
                    aria-pressed={selectedStoreId === null}
                    onClick={() => onSelectStore(null)}
                >
                    Todas
                </button>
                {stores.map((store) => (
                    <button
                        key={store.id}
                        type="button"
                        className={`${styles.reviews__chip} ${selectedStoreId === store.id ? styles['reviews__chip--active'] : ''}`}
                        aria-pressed={selectedStoreId === store.id}
                        onClick={() => onSelectStore(store.id)}
                    >
                        {store.name}
                    </button>
                ))}
            </div>

            {status === 'loading' && <p className={styles.reviews__message}>Cargando opiniones...</p>}

            {status === 'error' && (
                <div className={styles.reviews__message}>
                    <p>No pudimos cargar las opiniones.</p>
                    <button type="button" className={styles.reviews__more} onClick={() => setAttempt((n) => n + 1)}>
                        Reintentar
                    </button>
                </div>
            )}

            {status === 'empty' && (
                <p className={styles.reviews__message}>
                    {selectedStore
                        ? `Aún nadie opinó sobre este producto en ${selectedStore.name}.`
                        : 'Aún nadie opinó sobre este producto.'}
                    {' '}Usa Opinar en la tarjeta de una tienda para ser quien abra la conversación.
                </p>
            )}

            {status === 'success' && (
                <>
                    <ul className={styles.reviews__list}>
                        {items.map((review) => (
                            <li key={review.id} className={styles.review}>
                                <div className={styles.review__head}>
                                    <span className={styles.review__author}>{review.authorName}</span>
                                    <StarRating value={review.score} size="sm" label={`${review.score} de 5`} />
                                    {selectedStoreId === null && (
                                        <span className={styles.review__store}>{review.storeName}</span>
                                    )}
                                    <span className={styles.review__date}>{formatDate(review.registrationDate)}</span>
                                </div>
                                {/* React escapa el texto: un comentario con HTML se ve literal */}
                                <p className={styles.review__text}>{review.text}</p>
                                {canReport && (
                                    <button type="button" className={styles.review__report} onClick={() => setReporting(review)}>
                                        Reportar
                                    </button>
                                )}
                            </li>
                        ))}
                    </ul>

                    {moreError && (
                        <p className={styles.reviews__message}>No pudimos cargar más opiniones. Inténtalo de nuevo.</p>
                    )}
                    {hasMore && (
                        <button type="button" className={styles.reviews__more} onClick={handleLoadMore} disabled={loadingMore}>
                            {loadingMore ? 'Cargando...' : `Ver más opiniones (${total - items.length})`}
                        </button>
                    )}
                </>
            )}

            {reporting && <ReportDialog review={reporting} onClose={() => setReporting(null)} />}
        </section>
    );
}
