import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { toggleTheme } from '../utils/theme';
import { useAuth } from '../context/useAuth';

export default function Header() {
    const location = useLocation();
    const navigate = useNavigate();
    const { user, isAuthenticated, logout } = useAuth();
    const [menuOpen, setMenuOpen] = useState(false);
    const [userMenuOpen, setUserMenuOpen] = useState(false);
    const [query, setQuery] = useState('');
    const userMenuRef = useRef(null);

    const canFollow = isAuthenticated && user?.role === 'USER';
    // La página de búsqueda ya trae su propio campo; dos juntos confunden
    const showSearch = location.pathname !== '/buscar';
    const firstName = user?.name?.split(' ')[0];
    const initial = user?.name?.charAt(0).toUpperCase();

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
        return () => {
            document.removeEventListener('mousedown', onClickOutside);
            document.removeEventListener('keydown', onEscape);
        };
    }, [userMenuOpen]);

    const closeMenus = () => {
        setMenuOpen(false);
        setUserMenuOpen(false);
    };

    const handleScrollTo = (e, elementId) => {
        e.preventDefault();
        closeMenus();

        const scroll = () =>
            document.getElementById(elementId)?.scrollIntoView({ behavior: 'smooth' });

        if (location.pathname !== '/') {
            navigate('/');
            // Home todavía no se ha montado cuando navigate() retorna
            setTimeout(scroll, 100);
        } else {
            scroll();
        }
    };

    const handleSearch = (e) => {
        e.preventDefault();
        const term = query.trim();
        closeMenus();
        navigate(term ? `/buscar?q=${encodeURIComponent(term)}` : '/buscar');
    };

    const handleLogout = () => {
        closeMenus();
        logout();
        navigate('/');
    };

    return (
        <header className="site-header">
            <div className="site-header__inner">
                <button
                    className="icon-button site-header__menu"
                    type="button"
                    aria-label="Abrir menú"
                    aria-expanded={menuOpen}
                    onClick={() => setMenuOpen(!menuOpen)}
                >
                    <span className="icon icon--menu"></span>
                </button>

                <Link to="/" className="site-header__title">
                    <span className="logo-text">Precios</span>
                    <img src="/assets/images/co-badge.png" alt="Badge Colombia" className="logo-co" />
                </Link>

                {showSearch && (
                    <form className="header-search" onSubmit={handleSearch} role="search">
                        <input
                            type="search"
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                            placeholder="Buscar productos, marcas y más..."
                            aria-label="Buscar productos"
                        />
                        <button type="submit" aria-label="Buscar">
                            <span className="icon icon--search"></span>
                        </button>
                    </form>
                )}

                <div className="header-actions">
                    <button id="theme-toggle" className="theme-toggle" onClick={toggleTheme}>
                        Modo oscuro
                    </button>

                    {/* Darwin: la campana de avisos va aquí, antes del menú de usuario */}

                    <div className="user-menu" ref={userMenuRef}>
                        <button
                            type="button"
                            className="user-menu__button"
                            aria-haspopup="menu"
                            aria-expanded={userMenuOpen}
                            onClick={() => setUserMenuOpen(!userMenuOpen)}
                        >
                            {isAuthenticated ? (
                                <>
                                    <span className="user-menu__avatar" aria-hidden="true">{initial}</span>
                                    <span className="user-menu__label">{firstName}</span>
                                </>
                            ) : (
                                <>
                                    <span className="icon icon--user" aria-hidden="true"></span>
                                    <span className="user-menu__label">Ingresa</span>
                                </>
                            )}
                            <span className="user-menu__chevron" aria-hidden="true">▾</span>
                        </button>

                        {userMenuOpen && (
                            <div className="user-menu__panel" role="menu">
                                {isAuthenticated ? (
                                    <>
                                        <div className="user-menu__profile">
                                            <span className="user-menu__avatar user-menu__avatar--lg" aria-hidden="true">{initial}</span>
                                            <div className="user-menu__identity">
                                                <strong>{user?.name}</strong>
                                                <small>{user?.email}</small>
                                            </div>
                                        </div>

                                        {canFollow && (
                                            <>
                                                <div className="user-menu__divider"></div>
                                                <Link to="/seguidos" className="user-menu__item" role="menuitem" onClick={closeMenus}>
                                                    Seguidos
                                                </Link>
                                            </>
                                        )}

                                        <div className="user-menu__divider"></div>
                                        <button type="button" className="user-menu__item" role="menuitem" onClick={handleLogout}>
                                            Salir
                                        </button>
                                    </>
                                ) : (
                                    <div className="user-menu__guest">
                                        <Link to="/login" className="user-menu__cta" role="menuitem" onClick={closeMenus}>
                                            Ingresa
                                        </Link>
                                        <Link to="/register" className="user-menu__item" role="menuitem" onClick={closeMenus}>
                                            Crea tu cuenta
                                        </Link>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            </div>

            <nav className={`site-nav ${menuOpen ? 'site-nav--open' : ''}`} aria-label="Principal">
                <ul className="site-nav__list">
                    <li><Link to="/" onClick={closeMenus}>Inicio</Link></li>
                    <li><Link to="/buscar" onClick={closeMenus}>Buscar</Link></li>
                    <li><a href="#sobre-nosotros" onClick={(e) => handleScrollTo(e, 'sobre-nosotros')}>Sobre Nosotros</a></li>
                    <li><a href="#como-funciona" onClick={(e) => handleScrollTo(e, 'como-funciona')}>Como funciona</a></li>
                    <li><a href="#testimonios" onClick={(e) => handleScrollTo(e, 'testimonios')}>Testimonios</a></li>
                </ul>
            </nav>
        </header>
    );
}