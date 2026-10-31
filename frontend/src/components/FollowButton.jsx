import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import FollowDialog from './FollowDialog';
import { getMyFollow } from '../api/follows';
import { isLoggedIn, getUser } from '../utils/auth-helper';
import { describeTarget } from '../utils/follow-helper';
import styles from './FollowButton.module.css';

/**
 * Botón Seguir del comparador (un producto, no una tienda).
 * - Sin sesión: lleva a /login.
 * - Con rol distinto de USER: no se muestra (solo los consumidores siguen productos).
 * - Con rol USER: abre el diálogo para seguir o, si ya lo sigue, para ajustar el aviso.
 *
 * @param {number|string} productId
 * @param {string} productName
 */
export default function FollowButton({ productId, productName }) {
    const location = useLocation();
    const loggedIn = isLoggedIn();
    const canFollow = loggedIn && getUser()?.role === 'USER';

    // Se guarda junto al producto al que pertenece: al cambiar de producto no se ve el estado del anterior
    const [loaded, setLoaded] = useState({ productId: null, follow: null, failed: false });
    const [dialogOpen, setDialogOpen] = useState(false);
    const [notice, setNotice] = useState('');
    const [reloadKey, setReloadKey] = useState(0);

    const isCurrent = loaded.productId === String(productId);
    const follow = isCurrent ? loaded.follow : undefined; // undefined = cargando, null = no lo sigue
    const failed = isCurrent && loaded.failed;

    useEffect(() => {
        if (!canFollow) return;
        let cancelled = false;

        const loadFollow = async () => {
            try {
                const data = await getMyFollow(productId);
                if (!cancelled) setLoaded({ productId: String(productId), follow: data, failed: false });
            } catch (err) {
                console.error(err);
                if (!cancelled) setLoaded({ productId: String(productId), follow: null, failed: true });
            }
        };

        loadFollow();
        return () => { cancelled = true; };
    }, [canFollow, productId, reloadKey]);

    // El aviso de "listo" se oculta solo; sigue anunciándose a lectores de pantalla al aparecer
    useEffect(() => {
        if (!notice) return;
        const timer = setTimeout(() => setNotice(''), 4000);
        return () => clearTimeout(timer);
    }, [notice]);

    if (!loggedIn) {
        return (
            <div className={styles.follow}>
                <Link className={styles['follow-button']} to="/login" state={{ from: location }}>
                    Seguir
                </Link>
            </div>
        );
    }

    if (!canFollow) return null;

    const handleSaved = (saved) => {
        setNotice(follow ? 'Cambios guardados.' : 'Ahora sigues este producto.');
        setLoaded({ productId: String(productId), follow: saved, failed: false });
        setDialogOpen(false);
    };

    const handleUnfollowed = () => {
        setNotice('Dejaste de seguir este producto.');
        setLoaded({ productId: String(productId), follow: null, failed: false });
        setDialogOpen(false);
    };

    const handleConflict = () => {
        setDialogOpen(false);
        setReloadKey((key) => key + 1);
    };

    return (
        <div className={styles.follow}>
            {follow ? (
                <button
                    type="button"
                    className={`${styles['follow-button']} ${styles['follow-button--following']}`}
                    onClick={() => setDialogOpen(true)}
                    aria-haspopup="dialog"
                >
                    ✓ Siguiendo
                </button>
            ) : (
                <button
                    type="button"
                    className={styles['follow-button']}
                    onClick={() => setDialogOpen(true)}
                    disabled={follow === undefined || failed}
                    aria-haspopup="dialog"
                >
                    Seguir
                </button>
            )}

            {follow && (
                <p className={styles.follow__status}>
                    {follow.notificationActive ? describeTarget(follow.targetPrice) : 'Aviso pausado'}
                </p>
            )}

            {failed && (
                <p className={styles.follow__error} role="alert">
                    No se pudo consultar tu seguimiento.{' '}
                    <button type="button" className={styles.follow__retry} onClick={() => setReloadKey((key) => key + 1)}>
                        Reintentar
                    </button>
                </p>
            )}

            {/* Región viva: se anuncia el resultado sin mover el foco */}
            <div role="status" aria-live="polite">
                {notice && <p className={styles.follow__notice}>{notice}</p>}
            </div>

            {dialogOpen && (
                <FollowDialog
                    productId={productId}
                    productName={productName}
                    follow={follow}
                    onClose={() => setDialogOpen(false)}
                    onSaved={handleSaved}
                    onUnfollowed={handleUnfollowed}
                    onConflict={handleConflict}
                />
            )}
        </div>
    );
}