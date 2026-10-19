import { useId, useState } from 'react';
import { Link } from 'react-router-dom';
import Dialog from './Dialog';
import { reportReview } from '../api/reviews';
import styles from './ReviewForm.module.css';

const MAX_DETAILS = 200;

// Razones que acepta el backend (ReportReason)
const REASONS = [
    { value: 'SPAM', label: 'Spam' },
    { value: 'LENGUAJE_OFENSIVO', label: 'Lenguaje ofensivo' },
    { value: 'INFORMACION_FALSA', label: 'Información falsa' },
    { value: 'OTRO', label: 'Otro' },
];

/**
 * Diálogo para reportar el comentario de una opinión.
 * La razón es obligatoria y, si es OTRO, también el detalle.
 *
 * @param {object} review - opinión de la lista pública (usa `id` y `authorName`)
 * @param {function} onClose
 */
export default function ReportDialog({ review, onClose }) {
    const groupName = useId();
    const detailsId = useId();

    const [reason, setReason] = useState('');
    const [details, setDetails] = useState('');
    const [error, setError] = useState(null);
    const [sessionExpired, setSessionExpired] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [done, setDone] = useState(false);

    const isOther = reason === 'OTRO';

    const validateClientSide = () => {
        if (!reason) return 'Elige una razón';
        if (isOther && !details.trim()) return 'Describe la razón del reporte';
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
            await reportReview(review.id, { reason, details: details.trim() });
            setDone(true);
        } catch (err) {
            setError(err.message);
            setSessionExpired(err.status === 401);
        } finally {
            setIsLoading(false);
        }
    };

    if (done) {
        return (
            <Dialog title="Reporte enviado" onClose={onClose}>
                <div className={styles['review-form']}>
                    <p className={styles['review-form__text']}>Gracias por avisar.</p>
                    <div className={styles['review-form__actions']}>
                        <button type="button" className={styles['review-form__submit']} onClick={onClose}>
                            Cerrar
                        </button>
                    </div>
                </div>
            </Dialog>
        );
    }

    return (
        <Dialog title="Reportar comentario" subtitle={`Opinión de ${review.authorName}`} onClose={onClose}>
            <form className={styles['review-form']} onSubmit={handleSubmit} noValidate>
                <fieldset className={styles['review-form__options']}>
                    <legend className={styles['review-form__legend']}>Razón</legend>
                    {REASONS.map((item) => (
                        <label
                            key={item.value}
                            className={`${styles['review-form__option']} ${reason === item.value ? styles['review-form__option--selected'] : ''}`}
                        >
                            <input
                                type="radio"
                                name={groupName}
                                value={item.value}
                                checked={reason === item.value}
                                onChange={() => setReason(item.value)}
                            />
                            {item.label}
                        </label>
                    ))}
                </fieldset>

                <div className={styles['review-form__field']}>
                    <label className={styles['review-form__label']} htmlFor={detailsId}>
                        Cuéntanos más{isOther ? '' : ' (opcional)'}
                    </label>
                    <textarea
                        id={detailsId}
                        className={styles['review-form__textarea']}
                        value={details}
                        onChange={(e) => setDetails(e.target.value)}
                        maxLength={MAX_DETAILS}
                        rows={3}
                    />
                    <div className={styles['review-form__meta']}>
                        <span>{isOther ? 'Obligatorio con la razón "Otro"' : ''}</span>
                        <span>{details.length}/{MAX_DETAILS}</span>
                    </div>
                </div>

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
                        {isLoading ? 'Enviando...' : 'Enviar reporte'}
                    </button>
                </div>
            </form>
        </Dialog>
    );
}
