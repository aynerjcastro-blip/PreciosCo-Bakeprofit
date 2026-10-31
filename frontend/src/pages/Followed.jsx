import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import FollowDialog from '../components/FollowDialog';
import { getFollows, updateFollow } from '../api/follows';
import { formatDate } from '../utils/format-helper';
import { formatPrice, describeTarget } from '../utils/follow-helper';
import styles from './Followed.module.css';

/**
 * Página /seguidos (ruta protegida): lista de productos que sigo, con su precio más bajo,
 * precio objetivo y estado del aviso. Desde aquí se pausa o reactiva el aviso, se edita
 * el objetivo o se deja de seguir.
 */
export default function Followed() {
    const [status, setStatus] = useState('loading'); // loading | success | error
    const [follows, setFollows] = useState([]);
    const [loadError, setLoadError] = useState(null); // { message, status } si falló la carga
    const [reloadKey, setReloadKey] = useState(0);

    const [editing, setEditing] = useState(null);     // seguimiento abierto en el diálogo
    const [pendingId, setPendingId] = useState(null); // seguimiento cuyo aviso se está cambiando
    const [actionError, setActionError] = useState(null);

    useEffect(() => {
        let cancelled = false;

        const loadFollows = async () => {
            try {
                const data = await getFollows();
                if (cancelled) return;
                setFollows(data);
                setStatus('success');
            } catch (err) {
                console.error(err);
                if (cancelled) return;
                setLoadError({ message: err.message, status: err.status });
                setStatus('error');
            }
        };

        loadFollows();
        return () => { cancelled = true; };
    }, [reloadKey]);

    const handleRetry = () => {
        setStatus('loading');
        setLoadError(null);
        setReloadKey((key) => key + 1);
    };

    const replaceFollow = (updated) => {
        setFollows((current) => current.map((item) => (item.id === updated.id ? updated : item)));
    };

    // Pausar o reactivar el aviso sin abrir el diálogo; conserva el precio objetivo
    const handleToggle = async (follow) => {
        setActionError(null);
        setPendingId(follow.id);
        try {
            const updated = await updateFollow(follow.id, {
                notificationActive: !follow.notificationActive,
                targetPrice: follow.targetPrice,
            });
            replaceFollow(updated);
        } catch (err) {
            setActionError(err.message);
        } finally {
            setPendingId(null);
        }
    };

    return (
        <section className={styles.followed}>
            <header className={styles.followed__header}>
                <h1 className={styles.followed__title}>Productos seguidos</h1>
                <p className={styles.followed__subtitle}>
                    Te avisamos cuando baje el precio de lo que sigues.
                </p>
            </header>

            {actionError && (
                <p className={styles.followed__alert} role="alert">{actionError}</p>
            )}

            {status === 'loading' && (
                <p className={styles.followed__state} role="status">
                    <span className={styles.followed__spinner} aria-hidden="true" />
                    Cargando tus seguimientos...
                </p>
            )}

            {status === 'error' && (
                <div className={`${styles.followed__state} ${styles['followed__state--error']}`} role="alert">
                    {loadError?.status === 403 && <p>Solo las cuentas de consumidor pueden seguir productos.</p>}
                    {loadError?.status === 401 && (
                        <p>Tu sesión expiró. <Link to="/login">Iniciar sesión</Link></p>
                    )}
                    {loadError?.status !== 403 && loadError?.status !== 401 && (
                        <>
                            <p>{loadError?.message || 'No se pudieron cargar tus seguimientos.'}</p>
                            <button type="button" className={styles.followed__button} onClick={handleRetry}>
                                Reintentar
                            </button>
                        </>
                    )}
                </div>
            )}

            {status === 'success' && follows.length === 0 && (
                <div className={styles.followed__state}>
                    <p>Aún no sigues ningún producto.</p>
                    <p>Entra a la comparación de un producto y pulsa «Seguir».</p>
                    <Link className={styles.followed__button} to="/buscar">Buscar productos</Link>
                </div>
            )}

            {status === 'success' && follows.length > 0 && (
                <ul className={styles['follow-list']}>
                    {follows.map((follow) => (
                        <li key={follow.id} className={styles['follow-card']}>
                            <div className={styles['follow-card__head']}>
                                <h2 className={styles['follow-card__name']}>
                                    <Link to={`/comparador?productId=${follow.productId}`}>{follow.productName}</Link>
                                </h2>
                                <span
                                    className={`${styles.badge} ${follow.notificationActive ? styles['badge--active'] : styles['badge--paused']}`}
                                >
                                    {follow.notificationActive ? 'Aviso activo' : 'Aviso pausado'}
                                </span>
                            </div>

                            <dl className={styles['follow-card__data']}>
                                <div>
                                    <dt>Precio más bajo</dt>
                                    <dd className={styles['follow-card__price']}>
                                        {follow.lowestPrice != null ? formatPrice(follow.lowestPrice) : 'Sin precios'}
                                    </dd>
                                </div>
                                <div>
                                    <dt>Aviso</dt>
                                    <dd>{describeTarget(follow.targetPrice)}</dd>
                                </div>
                                <div>
                                    <dt>Siguiendo desde</dt>
                                    <dd>{formatDate(follow.registrationDate)}</dd>
                                </div>
                            </dl>

                            <div className={styles['follow-card__actions']}>
                                <button
                                    type="button"
                                    role="switch"
                                    aria-checked={follow.notificationActive}
                                    aria-label={`Avisarme de ${follow.productName}`}
                                    className={styles.switch}
                                    onClick={() => handleToggle(follow)}
                                    disabled={pendingId === follow.id}
                                >
                                    <span className={styles.switch__track} aria-hidden="true">
                                        <span className={styles.switch__thumb} />
                                    </span>
                                    <span>Avisarme</span>
                                </button>
                                <button
                                    type="button"
                                    className={styles['follow-card__edit']}
                                    onClick={() => setEditing(follow)}
                                    aria-haspopup="dialog"
                                >
                                    Ajustar
                                </button>
                            </div>
                        </li>
                    ))}
                </ul>
            )}

            {editing && (
                <FollowDialog
                    productId={editing.productId}
                    productName={editing.productName}
                    follow={editing}
                    onClose={() => setEditing(null)}
                    onSaved={(saved) => {
                        replaceFollow(saved);
                        setEditing(null);
                    }}
                    onUnfollowed={() => {
                        setFollows((current) => current.filter((item) => item.id !== editing.id));
                        setEditing(null);
                    }}
                />
            )}
        </section>
    );
}