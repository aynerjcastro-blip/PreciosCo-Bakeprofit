/**
 * @description Componente principal de navegación superior para PreciosCo.
 * Una sola fila: logo, menú de secciones en píldora (centro) y acciones (tema, avisos, usuario).
 * Gestiona el menú responsivo, la alternancia de tema y el menú de estado/perfil del usuario.
 */

import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/useAuth';
import { usePresence } from '@/hooks/usePresence';
import NotificationBell from "@/components/follows/NotificationBell/NotificationBell";
import { SECTIONS } from '@/config/section';
import ThemeToggle from './ThemeToogle';

// Importación del módulo de CSS para garantizar el encapsulamiento de estilos
import styles from './Header.module.css';

// Las secciones del menú salen de config/sections.js, la misma fuente de las tarjetas de la
// portada y de las páginas /secciones/:slug: un slug nuevo o renombrado se cambia en UN lugar.
const NAV_ITEMS = SECTIONS.map((section) => ({ slug: section.slug, label: section.title }));

// Duración de la animación de salida del menú de usuario (debe coincidir con Header.module.css)
const PANEL_EXIT_MS = 140;

function MenuIcon() {
    return (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            strokeWidth="2.2" strokeLinecap="round" aria-hidden="true">
            <path className={styles['menu-line-top']} d="M4 7h16" />
            <path className={styles['menu-line-middle']} d="M4 12h16" />
            <path className={styles['menu-line-bottom']} d="M4 17h16" />
        </svg>
    );
}

function SearchIcon() {
    return (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
        </svg>
    );
}

function UserIcon() {
    return (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <circle cx="12" cy="8" r="4" />
            <path d="M4 21a8 8 0 0 1 16 0" />
        </svg>
    );
}

function ChevronIcon() {
    return (
        <svg className={styles['user-menu__chevron']} width="14" height="14" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="m6 9 6 6 6-6" />
        </svg>
    );
}

/**
 * Componente Header.
 * @returns {JSX.Element} El encabezado renderizado de la aplicación.
 */
export default function Header() {
    // Hooks de enrutamiento
    const location = useLocation();
    const navigate = useNavigate();

    // Contexto de autenticación
    const { user, isAuthenticated, logout } = useAuth();

    // Estados locales para el control de la interfaz (UI)
    const [menuOpen, setMenuOpen] = useState(false);
    const [userMenuOpen, setUserMenuOpen] = useState(false);

    // Referencia al DOM para detectar interacciones fuera del menú desplegable del usuario
    const userMenuRef = useRef(null);

    // Mantiene el panel montado mientras se ve su animación de salida
    const userPanel = usePresence(userMenuOpen, PANEL_EXIT_MS);

    // Permisos derivados: solo los usuarios estándar pueden interactuar con seguimientos
    const canFollow = isAuthenticated && user?.role === 'USER';

    // El atajo de búsqueda se oculta donde ya hay un buscador principal (inicio, /buscar) o
    // donde estorba (login y registro).
    // DECISIÓN: el buscador de texto del header se reemplazó por este atajo a /buscar. Con el menú
    // de secciones centrado en la misma fila, el campo de texto no cabe sin volver a abrir una
    // segunda fila.
    const showSearchLink = !['/', '/buscar', '/login', '/register'].includes(location.pathname);

    // Sección marcada en el menú: la página de la sección (/secciones/:slug) o /buscar?sector=
    const params = new URLSearchParams(location.search);
    const activeSlug = location.pathname === '/buscar'
        ? params.get('sector')
        : location.pathname.startsWith('/secciones/')
            ? location.pathname.split('/')[2]
            : null;

    // Derivación segura de datos del usuario para la interfaz
    const firstName = user?.name?.split(' ')[0];
    const initial = user?.name?.charAt(0).toUpperCase();

    /**
     * Efecto secundario: Cierra el menú de usuario si se hace clic fuera de su contenedor
     * o si se presiona la tecla "Escape", mejorando la accesibilidad y UX.
     */
    useEffect(() => {
        if (!userMenuOpen) return;

        const onClickOutside = (e) => {
            if (userMenuRef.current && !userMenuRef.current.contains(e.target)) {
                setUserMenuOpen(false);
            }
        };

        const onEscape = (e) => {
            if (e.key === 'Escape') setUserMenuOpen(false);
        };

        document.addEventListener('mousedown', onClickOutside);
        document.addEventListener('keydown', onEscape);

        // Limpieza de event listeners al desmontar o cuando el menú se cierra
        return () => {
            document.removeEventListener('mousedown', onClickOutside);
            document.removeEventListener('keydown', onEscape);
        };
    }, [userMenuOpen]);

    /**
     * Utilidad para cerrar todos los menús de navegación activos simultáneamente.
     */
    const closeMenus = () => {
        setMenuOpen(false);
        setUserMenuOpen(false);
    };

    /**
     * Finaliza la sesión del usuario actual y lo redirige a la página principal.
     */
    const handleLogout = () => {
        closeMenus();
        logout();
        navigate('/');
    };

    return (
        <header className={styles['site-header']}>
            <div className={styles['site-header__inner']}>
                {/* Izquierda: hamburguesa (solo móvil) y logotipo */}
                <div className={styles['site-header__brand']}>
                    <button
                        className={`${styles['icon-button']} ${styles['site-header__menu']} ${menuOpen ? styles['site-header__menu--open'] : ''}`}
                        type="button"
                        aria-label={menuOpen ? 'Cerrar menú' : 'Abrir menú'}
                        aria-expanded={menuOpen}
                        aria-controls="menu-secciones"
                        onClick={() => setMenuOpen(!menuOpen)}
                    >
                        <MenuIcon />
                    </button>

                    <Link to="/" className={styles['site-header__title']} aria-label="PreciosCo, ir al inicio">
                        <span className={styles['logo-text']}>Precios</span>
                        <img src="/assets/images/co-badge.png" alt="" className={styles['logo-co']} />
                    </Link>
                </div>

                {/* Centro: menú de secciones en píldora. En móvil se vuelve un desplegable bajo el header. */}
                <nav
                    id="menu-secciones"
                    className={`${styles['site-nav']} ${menuOpen ? styles['site-nav--open'] : ''}`}
                    aria-label="Secciones"
                >
                    <ul className={styles['site-nav__list']}>
                        {NAV_ITEMS.map((item) => {
                            const active = activeSlug === item.slug;
                            return (
                                <li key={item.slug}>
                                    <Link
                                        to={`/secciones/${item.slug}`}
                                        className={`${styles['site-nav__link']} ${active ? styles['site-nav__link--active'] : ''}`}
                                        aria-current={active ? 'page' : undefined}
                                        onClick={closeMenus}
                                    >
                                        {item.label}
                                    </Link>
                                </li>
                            );
                        })}
                    </ul>
                </nav>

                {/* Derecha: acciones (búsqueda, tema, notificaciones, usuario) */}
                <div className={styles['header-actions']}>
                    {showSearchLink && (
                        <Link to="/buscar" className={styles['icon-button']} aria-label="Buscar productos" onClick={closeMenus}>
                            <SearchIcon />
                        </Link>
                    )}

                    <ThemeToggle />

                    {/* Campana de avisos: exclusiva para roles con permiso (USER) */}
                    {canFollow && <NotificationBell />}

                    {/* Contenedor del Menú de Perfil/Autenticación */}
                    <div className={styles['user-menu']} ref={userMenuRef}>
                        <button
                            type="button"
                            className={styles['user-menu__button']}
                            aria-haspopup="menu"
                            aria-expanded={userMenuOpen}
                            onClick={() => setUserMenuOpen(!userMenuOpen)}
                        >
                            {isAuthenticated ? (
                                <>
                                    <span className={styles['user-menu__avatar']} aria-hidden="true">
                                        {initial}
                                        <span className={styles['user-menu__status']} />
                                    </span>
                                    <span className={styles['user-menu__text']}>
                                        <span className={styles['user-menu__label']}>{firstName}</span>
                                        <span className={styles['user-menu__hint']}>Mi cuenta</span>
                                    </span>
                                </>
                            ) : (
                                <>
                                    <span className={styles['user-menu__guest-icon']} aria-hidden="true">
                                        <UserIcon />
                                    </span>
                                    <span className={styles['user-menu__label']}>Ingresa</span>
                                </>
                            )}
                            <ChevronIcon />
                        </button>

                        {/* Panel Desplegable del Usuario */}
                        {userPanel.mounted && (
                            <div
                                className={styles['user-menu__panel']}
                                role="menu"
                                data-state={userPanel.closing ? 'closing' : 'open'}
                            >
                                {isAuthenticated ? (
                                    <>
                                        <div className={styles['user-menu__profile']}>
                                            <span className={`${styles['user-menu__avatar']} ${styles['user-menu__avatar--lg']}`} aria-hidden="true">
                                                {initial}
                                            </span>
                                            <div className={styles['user-menu__identity']}>
                                                <strong>{user?.name}</strong>
                                                <small>{user?.email}</small>
                                            </div>
                                        </div>

                                        <div className={styles['user-menu__divider']}></div>
                                        <Link to="/perfil" className={styles['user-menu__item']} role="menuitem" onClick={closeMenus}>
                                            Mi Perfil
                                        </Link>

                                        {canFollow && (
                                            <>
                                                <div className={styles['user-menu__divider']}></div>
                                                <Link to="/seguidos" className={styles['user-menu__item']} role="menuitem" onClick={closeMenus}>
                                                    Seguidos
                                                </Link>
                                            </>
                                        )}

                                        <div className={styles['user-menu__divider']}></div>
                                        <button type="button" className={styles['user-menu__item']} role="menuitem" onClick={handleLogout}>
                                            Salir
                                        </button>
                                    </>
                                ) : (
                                    <div className={styles['user-menu__guest']}>
                                        <Link to="/login" className={styles['user-menu__cta']} role="menuitem" onClick={closeMenus}>
                                            Ingresa
                                        </Link>
                                        <Link to="/register" className={styles['user-menu__item']} role="menuitem" onClick={closeMenus}>
                                            Crea tu cuenta
                                        </Link>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </header>
    );
}