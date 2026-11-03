/**
 * @file Header.jsx
 * @description Componente principal de navegación superior para PreciosCo. 
 * Gestiona el menú responsivo, la barra de búsqueda global, la alternancia de tema 
 * y el menú de estado/perfil del usuario autenticado.
 */

import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { toggleTheme } from '../utils/theme';
import { useAuth } from '../context/useAuth';
import NotificationBell from './NotificationBell';

// Importación del módulo de CSS para garantizar el encapsulamiento de estilos
import styles from './Header.module.css';

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
    const [query, setQuery] = useState('');

    // Referencia al DOM para detectar interacciones fuera del menú desplegable del usuario
    const userMenuRef = useRef(null);

    // Permisos derivados: solo los usuarios estándar pueden interactuar con seguimientos
    const canFollow = isAuthenticated && user?.role === 'USER';

    // Condición de renderizado: oculta el buscador del header en las vistas '/' y '/buscar'
    // para evitar redundancia visual con los buscadores principales de esas páginas.
    const showSearch = location.pathname !== '/' && location.pathname !== '/buscar';

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
     * Intercepta el envío del formulario de búsqueda, formatea la consulta 
     * y redirige dinámicamente a la ruta de resultados.
     * @param {React.FormEvent} e - Evento de envío del formulario.
     */
    const handleSearch = (e) => {
        e.preventDefault();
        const term = query.trim();
        closeMenus();
        navigate(term ? `/buscar?q=${encodeURIComponent(term)}` : '/buscar');
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
                {/* Botón Hamburguesa (Móvil) */}
                <button
                    className={`${styles['icon-button']} ${styles['site-header__menu']}`}
                    type="button"
                    aria-label="Abrir menu"
                    aria-expanded={menuOpen}
                    onClick={() => setMenuOpen(!menuOpen)}
                >
                    <span className={`${styles.icon} ${styles['icon--menu']}`}></span>
                </button>

                {/* Logotipo de la plataforma */}
                <Link to="/" className={styles['site-header__title']}>
                    <span className={styles['logo-text']}>Precios</span>
                    <img src="/assets/images/co-badge.png" alt="Badge Colombia" className={styles['logo-co']} />
                </Link>

                {/* Buscador Condicional */}
                {showSearch && (
                    <form className={styles['header-search']} onSubmit={handleSearch} role="search">
                        <input
                            type="search"
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                            placeholder="Buscar productos, marcas y mas..."
                            aria-label="Buscar productos"
                        />
                        <button type="submit" aria-label="Buscar">
                            <span className={`${styles.icon} ${styles['icon--search']}`}></span>
                        </button>
                    </form>
                )}

                {/* Acciones de Cabecera (Tema, Notificaciones, Menú de Usuario) */}
                <div className={styles['header-actions']}>
                    <button id="theme-toggle" className={styles['theme-toggle']} onClick={toggleTheme}>
                        Modo oscuro
                    </button>

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
                                    </span>
                                    <span className={styles['user-menu__label']}>{firstName}</span>
                                </>
                            ) : (
                                <>
                                    <span className={`${styles.icon} ${styles['icon--user']}`} aria-hidden="true"></span>
                                    <span className={styles['user-menu__label']}>Ingresa</span>
                                </>
                            )}
                        </button>

                        {/* Panel Desplegable del Usuario */}
                        {userMenuOpen && (
                            <div className={styles['user-menu__panel']} role="menu">
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

            {/* Navegación por sectores comerciales (Se colapsa en móvil) */}
            <nav className={`${styles['site-nav']} ${menuOpen ? styles['site-nav--open'] : ''}`} aria-label="Principal">
                <ul className={styles['site-nav__list']}>
                    <li><Link to="/buscar?sector=supermercados" onClick={closeMenus}>Supermercados</Link></li>
                    <li><Link to="/buscar?sector=droguerias" onClick={closeMenus}>Farmacia</Link></li>
                    <li><Link to="/buscar?sector=repuestos" onClick={closeMenus}>Repuestos</Link></li>
                    <li><Link to="/buscar?sector=cuidado-facial" onClick={closeMenus}>Belleza</Link></li>
                    <li><Link to="/buscar" onClick={closeMenus}>Tecnologia</Link></li>
                </ul>
            </nav>
        </header>
    );
}