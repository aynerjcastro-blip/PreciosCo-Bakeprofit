import { useId, useState } from 'react';
import { Link } from 'react-router-dom';
import Dialog from './Dialog';
import { createFollow, updateFollow, deleteFollow } from '../api/follows';
import formStyles from './ReviewForm.module.css';
import styles from './FollowDialog.module.css';

/**
 * Diálogo para seguir un producto o ajustar su seguimiento.
 * Sin `follow` crea el seguimiento (precio objetivo opcional).
 * Con `follow` permite cambiar el objetivo, pausar o reactivar el aviso y dejar de seguir.
 *
 * @param {object} follow - seguimiento propio a editar (el que devuelve GET /api/follows)
 * @param {function} onSaved - recibe el seguimiento creado o actualizado
 * @param {function} onUnfollowed - se llama tras dejar de seguir
 * @param {function} [onConflict] - se llama si el backend dice que ya seguías el producto (409)
 */
export default function FollowDialog({ productId, productName, follow = null, onClose, onSaved, onUnfollowed, onConflict }) {
    const isEdit = Boolean(follow);
    const priceId = useId();
    const activeId = useId();
    const hintId = useId();

    const [targetPrice, setTargetPrice] = useState(follow?.targetPrice != null ? String(follow.targetPrice) : '');
    const [notificationActive, setNotificationActive] = useState(follow?.notificationActive ?? true);
    const [confirmingUnfollow, setConfirmingUnfollow] = useState(false);
    const [error, setError] = useState(null);
    const [sessionExpired, setSessionExpired] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    // Vacío = sin objetivo (null); si hay valor debe ser un número mayor que cero
    const parseTarget = () => {
        const text = targetPrice.trim();
        if (text === '') return { value: null };
        const number = Number(text);
        if (!Number.isFinite(number) || number <= 0) {
            return { error: 'El precio objetivo debe ser mayor que cero' };
        }
        return { value: number };
    };

    const showError = (err) => {
        setError(err.message);
        setSessionExpired(err.status === 401);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setSessionExpired(false);

        const target = parseTarget();
        if (target.error) {
            setError(target.error);
            return;
        }

        setIsLoading(true);
        try {
            const saved = isEdit
                ? await updateFollow(follow.id, { notificationActive, targetPrice: target.value })
                : await createFollow({ productId: Number(productId), targetPrice: target.value });
            onSaved(saved);
        } catch (err) {
            // 409 al crear: ya lo seguías (otra pestaña, por ejemplo); se refresca el estado del botón
            if (!isEdit && err.status === 409 && onConflict) {
                onConflict();
                return;
            }
            showError(err);
        } finally {
            setIsLoading(false);
        }
    };

    // Dejar de seguir pide una segunda pulsación: se pierde el precio objetivo
    const handleUnfollow = async () => {
        if (!confirmingUnfollow) {
            setConfirmingUnfollow(true);
            return;
        }
        setError(null);
        setSessionExpired(false);
        setIsLoading(true);
        try {
            await deleteFollow(follow.id);
            onUnfollowed();
        } catch (err) {
            showError(err);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Dialog
            title={isEdit ? 'Ajustar seguimiento' : 'Seguir producto'}
            subtitle={productName}
            onClose={onClose}
        >
            <form className={formStyles['review-form']} onSubmit={handleSubmit} noValidate>
                <div className={formStyles['review-form__field']}>
                    <label className={formStyles['review-form__label']} htmlFor={priceId}>
                        Precio objetivo (opcional)
                    </label>
                    <div className={styles['follow-form__price']}>
                        <span className={styles['follow-form__currency']} aria-hidden="true">$</span>
                        <input
                            id={priceId}
                            className={styles['follow-form__input']}
                            type="number"
                            inputMode="decimal"
                            min="1"
                            step="any"
                            value={targetPrice}
                            onChange={(e) => setTargetPrice(e.target.value)}
                            placeholder="Ej. 5000"
                            aria-describedby={hintId}
                        />
                    </div>
                    <p id={hintId} className={formStyles['review-form__note']}>
                        Si lo dejas vacío, te avisamos ante cualquier reducción de precio.
                    </p>
                </div>

                {isEdit && (
                    <div className={styles['follow-form__toggle']}>
                        <input
                            id={activeId}
                            type="checkbox"
                            checked={notificationActive}
                            onChange={(e) => setNotificationActive(e.target.checked)}
                        />
                        <label htmlFor={activeId}>Avisarme cuando baje el precio</label>
                    </div>
                )}

                <p className={formStyles['review-form__note']}>
                    {isEdit
                        ? 'Si pausas el aviso sigues el producto, pero no recibes avisos. Al reactivarlo solo cuentan las bajas que ocurran después.'
                        : 'Los avisos te llegan a la campana y, si está activado, a tu correo.'}
                </p>

                {error && (
                    <p className={formStyles['review-form__error']} role="alert">
                        {error}{' '}
                        {sessionExpired && <Link to="/login">Iniciar sesión</Link>}
                    </p>
                )}

                <div className={formStyles['review-form__actions']}>
                    {isEdit && (
                        <button
                            type="button"
                            className={`${formStyles['review-form__cancel']} ${styles['follow-form__unfollow']}`}
                            onClick={handleUnfollow}
                            disabled={isLoading}
                        >
                            {confirmingUnfollow ? 'Confirmar: dejar de seguir' : 'Dejar de seguir'}
                        </button>
                    )}
                    <button type="button" className={formStyles['review-form__cancel']} onClick={onClose}>
                        Cancelar
                    </button>
                    <button type="submit" className={formStyles['review-form__submit']} disabled={isLoading}>
                        {isLoading ? 'Guardando...' : (isEdit ? 'Guardar cambios' : 'Seguir producto')}
                    </button>
                </div>
            </form>
        </Dialog>
    );
}