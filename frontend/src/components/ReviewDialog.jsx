import { useId, useState } from 'react';
import { Link } from 'react-router-dom';
import Dialog from './Dialog';
import StarRating from './StarRating';
import { createReview, updateReview } from '../api/reviews';
import { formatDateTime } from '../utils/format-helper';
import styles from './ReviewForm.module.css';

const MIN_LENGTH = 10;
const MAX_LENGTH = 500;

/**
 * Diálogo para calificar y comentar un producto en una tienda (RN-30: siempre juntos).
 * Sin `review` crea la opinión; con `review` la edita.
 *
 * @param {object} review - opinión propia a editar (la que devuelve GET /api/reviews/me)
 * @param {function} onSaved - recibe la opinión creada o actualizada
 * @param {function} onClose
 */
export default function ReviewDialog({ productId, productName, storeId, storeName, review = null, onClose, onSaved }) {
    const isEdit = Boolean(review);
    const textId = useId();

    const [score, setScore] = useState(review?.score ?? 0);
    const [text, setText] = useState(review?.text ?? '');
    const [error, setError] = useState(null);
    const [sessionExpired, setSessionExpired] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    const validateClientSide = () => {
        if (score < 1) {
            return 'Elige una calificación de 1 a 5 estrellas';
        }
        const length = text.trim().length;
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            return `El comentario debe tener entre ${MIN_LENGTH} y ${MAX_LENGTH} caracteres`;
        }
        return null;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setSessionExpired(false);

        const clientError = validateClientSide();
        if (clientError) {
            setError(clientError);
            return;
        }

        setIsLoading(true);
        try {
            const saved = isEdit
                ? await updateReview(review.id, { score, text: text.trim() })
                : await createReview({ productId, storeId, score, text: text.trim() });
            onSaved(saved);
        } catch (err) {
            setError(err.message);
            setSessionExpired(err.status === 401);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Dialog
            title={isEdit ? 'Editar tu opinión' : 'Tu opinión'}
            subtitle={`${productName} en ${storeName}`}
            onClose={onClose}
        >
            <form className={styles['review-form']} onSubmit={handleSubmit} noValidate>
                <StarRating value={score} onChange={setScore} label="Calificación" />

                <div className={styles['review-form__field']}>
                    <label className={styles['review-form__label']} htmlFor={textId}>Comentario</label>
                    <textarea
                        id={textId}
                        className={styles['review-form__textarea']}
                        value={text}
                        onChange={(e) => setText(e.target.value)}
                        maxLength={MAX_LENGTH}
                        rows={4}
                        placeholder="Cuenta qué te pareció"
                    />
                    <div className={styles['review-form__meta']}>
                        <span>Mínimo {MIN_LENGTH} caracteres</span>
                        <span>{text.length}/{MAX_LENGTH}</span>
                    </div>
                </div>

                <p className={styles['review-form__note']}>
                    {isEdit
                        ? (review.editableUntil ? `Puedes editarla hasta el ${formatDateTime(review.editableUntil)}.` : '')
                        : 'Solo puedes opinar una vez sobre este producto en esta tienda. Podrás editarla durante 24 horas.'}
                </p>

                {error && (
                    <p className={styles['review-form__error']} role="alert">
                        {error}{' '}
                        {sessionExpired && <Link to="/login">Iniciar sesión</Link>}
                    </p>
                )}

                <div className={styles['review-form__actions']}>
                    <button type="button" className={styles['review-form__cancel']} onClick={onClose}>
                        Cancelar
                    </button>
                    <button type="submit" className={styles['review-form__submit']} disabled={isLoading}>
                        {isLoading ? 'Guardando...' : (isEdit ? 'Guardar cambios' : 'Publicar opinión')}
                    </button>
                </div>
            </form>
        </Dialog>
    );
}
