import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Dialog from './Dialog';
import RatingSummary from './RatingSummary';
import ReviewDialog from './ReviewDialog';
import StarRating from './StarRating';
import { getSummary, getMyReview, deleteReviewComment } from '../api/reviews';
import { isLoggedIn, getUser } from '../utils/auth-helper';
import { formatDateTime } from '../utils/format-helper';
import formStyles from './ReviewForm.module.css';
import styles from './StoreReviewBar.module.css';

// Qué decirle al autor según el estado de su comentario y qué puede hacer
function describeOwnReview(review) {
    switch (review.commentStatus) {
        case 'REPORTADO':
            return { message: 'Tu comentario fue reportado y no se puede editar.', canDelete: true };
        case 'OCULTO':
            return { message: 'Un moderador ocultó tu comentario. Tu calificación se mantiene.', canDelete: false };
        case 'ELIMINADO':
            return {
                message: 'Eliminaste tu comentario. Tu calificación se mantiene y no puedes volver a opinar aquí.',
                canDelete: false,
            };
        default:
            return {
                message: review.editableUntil
                    ? `Puedes editarla hasta el ${formatDateTime(review.editableUntil)}.`
                    : 'Ya no se puede editar.',
                canDelete: true,
            };
    }
}

// Confirmación antes de eliminar: la acción no se puede deshacer
function DeleteCommentDialog({ reviewId, onClose, onDeleted }) {
    const [error, setError] = useState(null);
    const [isLoading, setIsLoading] = useState(false);

    const handleDelete = async () => {
        setError(null);
        setIsLoading(true);
        try {
            await deleteReviewComment(reviewId);
            onDeleted();
        } catch (err) {
            setError(err.message);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Dialog title="¿Eliminar tu comentario?" onClose={onClose}>
            <div className={formStyles['review-form']}>
                <p className={formStyles['review-form__text']}>
                    Tu calificación se conserva, pero no podrás volver a opinar sobre este producto en esta tienda.
                </p>

                {error && <p className={formStyles['review-form__error']} role="alert">{error}</p>}

                <div className={formStyles['review-form__actions']}>
                    <button type="button" className={formStyles['review-form__cancel']} onClick={onClose}>
                        Cancelar
                    </button>
                    <button
                        type="button"
                        className={`${formStyles['review-form__submit']} ${formStyles['review-form__submit--danger']}`}
                        onClick={handleDelete}
                        disabled={isLoading}
                    >
                        {isLoading ? 'Eliminando...' : 'Eliminar comentario'}
                    </button>
                </div>
            </div>
        </Dialog>
    );
}

/**
 * Franja de opiniones dentro de cada tarjeta de precio: promedio de la tienda,
 * botón para opinar y, si ya opinó, el estado de su opinión.
 *
 * @param {function} onShowReviews - recibe el storeId para filtrar la lista compartida
 * @param {function} onChanged - se llama tras crear, editar o eliminar (para refrescar lo demás)
 */
export default function StoreReviewBar({ productId, productName, storeId, storeName, onShowReviews, onChanged }) {
    // Solo el rol USER puede opinar (ADMIN y SUPPLIER no)
    const loggedIn = isLoggedIn();
    const canReview = loggedIn && getUser()?.role === 'USER';

    const [summary, setSummary] = useState(undefined);   // undefined = cargando, null = no disponible
    const [myReview, setMyReview] = useState(undefined); // undefined = cargando, null = aún no opina
    const [dialog, setDialog] = useState(null);          // null | 'review' | 'delete'
    const [reloadKey, setReloadKey] = useState(0);

    useEffect(() => {
        let cancelled = false;

        const loadSummary = async () => {
            try {
                const data = await getSummary({ productId, storeId });
                if (!cancelled) setSummary(data);
            } catch (err) {
                console.error(err);
                if (!cancelled) setSummary(null);
            }
        };

        loadSummary();
        return () => { cancelled = true; };
    }, [productId, storeId, reloadKey]);

    useEffect(() => {
        if (!canReview) return;
        let cancelled = false;

        const loadMyReview = async () => {
            try {
                const data = await getMyReview(productId, storeId);
                if (!cancelled) setMyReview(data);
            } catch (err) {
                console.error(err);
                if (!cancelled) setMyReview(null);
            }
        };

        loadMyReview();
        return () => { cancelled = true; };
    }, [canReview, productId, storeId, reloadKey]);

    const handleChanged = () => {
        setDialog(null);
        setReloadKey((key) => key + 1);
        onChanged();
    };

    const own = myReview ? describeOwnReview(myReview) : null;

    return (
        <div className={styles['store-reviews']}>
            <div className={styles['store-reviews__row']}>
                <RatingSummary
                    summary={summary}
                    size="sm"
                    onClick={() => onShowReviews(storeId)}
                    ariaLabel={`Ver las opiniones de ${storeName}`}
                />

                {!loggedIn && (
                    <Link className={styles['store-reviews__action']} to="/login">
                        Inicia sesión para opinar
                    </Link>
                )}
                {canReview && myReview === null && (
                    <button type="button" className={styles['store-reviews__action']} onClick={() => setDialog('review')}>
                        Opinar
                    </button>
                )}
            </div>

            {canReview && myReview && (
                <div className={styles['store-reviews__own']}>
                    <div className={styles['store-reviews__own-head']}>
                        <span className={styles['store-reviews__own-title']}>Tu opinión</span>
                        <StarRating value={myReview.score} size="sm" label={`Tu calificación: ${myReview.score} de 5`} />
                    </div>
                    <p className={styles['store-reviews__own-text']}>{own.message}</p>
                    <div className={styles['store-reviews__own-actions']}>
                        {myReview.editableUntil && (
                            <button type="button" className={styles['store-reviews__link']} onClick={() => setDialog('review')}>
                                Editar
                            </button>
                        )}
                        {own.canDelete && (
                            <button
                                type="button"
                                className={`${styles['store-reviews__link']} ${styles['store-reviews__link--danger']}`}
                                onClick={() => setDialog('delete')}
                            >
                                Eliminar comentario
                            </button>
                        )}
                    </div>
                </div>
            )}

            {dialog === 'review' && (
                <ReviewDialog
                    productId={productId}
                    productName={productName}
                    storeId={storeId}
                    storeName={storeName}
                    review={myReview}
                    onClose={() => setDialog(null)}
                    onSaved={(saved) => {
                        setMyReview(saved);
                        handleChanged();
                    }}
                />
            )}

            {dialog === 'delete' && (
                <DeleteCommentDialog
                    reviewId={myReview.id}
                    onClose={() => setDialog(null)}
                    onDeleted={handleChanged}
                />
            )}
        </div>
    );
}
