import { useCallback, useEffect, useId, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { getNotifications, markNotificationRead } from '../api/notifications';
import { formatDateTime } from '../utils/format-helper';
import { formatPrice } from '../utils/follow-helper';
import styles from './NotificationBell.module.css';

// Cada cuánto se consulta el contador de no leídos con la página abierta
const POLL_INTERVAL_MS = 60_000;

// Campana de avisos del Header (solo se monta con sesión y rol USER)
function BellIcon() {
    return (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>
    );
}

/**
 * Campana de avisos: indicador de no leídos, lista desplegable y opción de marcar como leído.
 *
 * - Consulta el contador al montarse y cada minuto (sin consultar si la pestaña está oculta).
 * - Al abrir el panel refresca la lista.
 * - Se cierra con Esc (devuelve el foco a la campana), con clic fuera o al ir a un producto.
 * - Si el backend responde 403 (rol sin avisos) se oculta; ante un 401 deja de consultar.
 *
 * Debe montarse solo para usuarios con rol USER.
 */
export default function NotificationBell() {
    const panelId = useId();
    const containerRef = useRef(null);
    const buttonRef = useRef(null);
    const stoppedRef = useRef(false);   // true tras un 401 o 403: no se vuelve a consultar
    const lastCountRef = useRef(null);  // para anunciar solo cuando aparecen avisos nuevos

    const [data, setData] = useState(null);   // null = aún sin datos; luego {unreadCount, notifications}
    const [loadError, setLoadError] = useState(null);
    const [forbidden, setForbidden] = useState(false);
    const [open, setOpen] = useState(false);
    const [markingId, setMarkingId] = useState(null);
    const [actionError, setActionError] = useState(null);
    const [announcement, setAnnouncement] = useState('');

    const refresh = useCallback(async () => {
        if (stoppedRef.current) return;
        try {
            const next = await getNotifications();
            setData(next);
            setLoadError(null);
            const previous = lastCountRef.current;
            if (previous !== null && next.unreadCount > previous) {
                setAnnouncement(`Tienes ${next.unreadCount} avisos sin leer`);
            }
            lastCountRef.current = next.unreadCount;
        } catch (err) {
            console.error(err);
            if (err.status === 403) {
                stoppedRef.current = true;
                setForbidden(true);
            } else if (err.status === 401) {
                stoppedRef.current = true; // sesión vencida: dejar de insistir
            }
            setLoadError(err.message);
        }
    }, []);

    // Carga inicial y consulta periódica
    useEffect(() => {
        refresh();

        const timer = setInterval(() => {
            if (!document.hidden) refresh();
        }, POLL_INTERVAL_MS);
        const onVisible = () => {
            if (!document.hidden) refresh();
        };
        document.addEventListener('visibilitychange', onVisible);

        return () => {
            clearInterval(timer);
            document.removeEventListener('visibilitychange', onVisible);
        };
    }, [refresh]);

    // Clic fuera y Esc cierran el panel
    useEffect(() => {
        if (!open) return;

        const onClickOutside = (e) => {
            if (containerRef.current && !containerRef.current.contains(e.target)) {
                setOpen(false);
            }
        };
        const onEscape = (e) => {
            if (e.key === 'Escape') {
                setOpen(false);
                buttonRef.current?.focus();
            }
        };

        document.addEventListener('mousedown', onClickOutside);
        document.addEventListener('keydown', onEscape);
        return () => {
            document.removeEventListener('mousedown', onClickOutside);
            document.removeEventListener('keydown', onEscape);
        };
    }, [open]);

    const handleToggle = () => {
        if (!open) {
            setActionError(null);
            refresh();
        }
        setOpen(!open);
    };

    // Marca como leído y baja el contador solo si el backend lo confirmó
    const handleMarkRead = async (item) => {
        if (item.read) return;
        setActionError(null);
        setMarkingId(item.id);
        try {
            await markNotificationRead(item.id);
            setData((current) => {
                if (!current) return current;
                const wasUnread = current.notifications.some((n) => n.id === item.id && !n.read);
                return {
                    unreadCount: wasUnread ? Math.max(0, current.unreadCount - 1) : current.unreadCount,
                    notifications: current.notifications.map((n) => (n.id === item.id ? { ...n, read: true } : n)),
                };
            });
            lastCountRef.current = Math.max(0, (lastCountRef.current ?? 1) - 1);
        } catch (err) {
            setActionError(err.message);
        } finally {
            setMarkingId(null);
        }
    };

    // Ir al producto del aviso cuenta como leerlo
    const handleOpenProduct = (item) => {
        setOpen(false);
        handleMarkRead(item);
    };

    if (forbidden) return null;

    const unreadCount = data?.unreadCount ?? 0;
    const notifications = data?.notifications ?? [];
    const badgeText = unreadCount > 99 ? '99+' : String(unreadCount);

    return (
        <div className={styles.bell} ref={containerRef}>
            <button
                ref={buttonRef}
                type="button"
                className={styles.bell__button}
                aria-label={unreadCount > 0 ? `Avisos, ${unreadCount} sin leer` : 'Avisos'}
                aria-expanded={open}
                aria-controls={panelId}
                onClick={handleToggle}
            >
                <BellIcon />
                {unreadCount > 0 && (
                    <span className={styles.bell__badge} aria-hidden="true">{badgeText}</span>
                )}
            </button>

            {/* Región viva: anuncia avisos nuevos a lectores de pantalla sin mover el foco */}
            <span className={styles['visually-hidden']} role="status" aria-live="polite">{announcement}</span>

            {open && (
                <div id={panelId} className={styles.bell__panel} role="region" aria-label="Avisos de baja de precio">
                    <div className={styles['bell__panel-head']}>
                        <h2 className={styles['bell__panel-title']}>Avisos</h2>
                        <span className={styles['bell__panel-count']}>
                            {unreadCount === 0 ? 'Al día' : `${unreadCount} sin leer`}
                        </span>
                    </div>

                    {actionError && (
                        <p className={styles.bell__alert} role="alert">{actionError}</p>
                    )}

                    {data === null && !loadError && (
                        <p className={styles.bell__state} role="status">
                            <span className={styles.bell__spinner} aria-hidden="true" />
                            Cargando avisos...
                        </p>
                    )}

                    {data === null && loadError && (
                        <div className={`${styles.bell__state} ${styles['bell__state--error']}`} role="alert">
                            <p>{loadError}</p>
                            <button type="button" className={styles.bell__link} onClick={refresh}>Reintentar</button>
                        </div>
                    )}

                    {data && notifications.length === 0 && (
                        <p className={styles.bell__state}>
                            Aún no tienes avisos. Sigue un producto y te avisamos cuando baje de precio.
                        </p>
                    )}

                    {data && notifications.length > 0 && (
                        <ul className={styles.bell__list}>
                            {notifications.map((item) => (
                                <li
                                    key={item.id}
                                    className={`${styles.bell__item} ${item.read ? '' : styles['bell__item--unread']}`}
                                >
                                    <span className={item.read ? styles['bell__dot--read'] : styles.bell__dot} aria-hidden="true" />
                                    <div className={styles.bell__content}>
                                        <Link
                                            className={styles.bell__product}
                                            to={`/comparador?productId=${item.productId}`}
                                            onClick={() => handleOpenProduct(item)}
                                        >
                                            {!item.read && <span className={styles['visually-hidden']}>No leído: </span>}
                                            {item.productName}
                                        </Link>
                                        <p className={styles.bell__text}>
                                            {item.storeName}: bajó de {formatPrice(item.previousPrice)} a{' '}
                                            <strong>{formatPrice(item.newPrice)}</strong>
                                        </p>
                                        <p className={styles.bell__date}>{formatDateTime(item.createdAt)}</p>
                                        {!item.read && (
                                            <button
                                                type="button"
                                                className={styles.bell__link}
                                                onClick={() => handleMarkRead(item)}
                                                disabled={markingId === item.id}
                                            >
                                                {markingId === item.id ? 'Marcando...' : 'Marcar como leído'}
                                            </button>
                                        )}
                                    </div>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
            )}
        </div>
    );
}