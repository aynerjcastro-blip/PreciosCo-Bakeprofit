import { Link, useLocation, useNavigate } from 'react-router-dom';
import { toggleTheme } from '../utils/theme';
import { useAuth } from '../context/useAuth'; // Importamos el contexto de autenticación

export default function Header() {
    const location = useLocation();
    const navigate = useNavigate();

    // Consumimos el estado reactivo de sesión desde AuthContext
    const { user, isAuthenticated, logout } = useAuth();

    /**
     * Maneja el desplazamiento suave (smooth scroll) hacia secciones de la página de inicio.
     * Si el usuario se encuentra en otra ruta, navega a '/' y luego realiza el desplazamiento.
     */
    const handleScrollTo = (e, elementId) => {
        e.preventDefault();

        if (location.pathname !== '/') {
            navigate('/');
            setTimeout(() => {
                const element = document.getElementById(elementId);
                if (element) {
                    element.scrollIntoView({ behavior: 'smooth' });
                }
            }, 100);
        } else {
            const element = document.getElementById(elementId);
            if (element) {
                element.scrollIntoView({ behavior: 'smooth' });
            }
        }
    };

    /**
     * Maneja el cierre de sesión del usuario.
     * Limpia los datos de autenticación y redirige a la página principal o login.
     */
    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    return (
        <>
            <header className="site-header">
                <div className="site-header__inner">
                    {/* Botón para la apertura del menú móvil */}
                    <button className="icon-button" type="button" aria-label="Abrir menú">
                        <span className="icon icon--menu"></span>
                    </button>

                    {/* Marca / Logo principal */}
                    <Link to="/" className="site-header__title">
                        <span className="logo-text">Precios</span>
                        <img src="/assets/images/co-badge.png" alt="Badge Colombia" className="logo-co" />
                    </Link>

                    {/* Acciones de la barra superior */}
                    <div className="header-actions">
                        {/* Interruptor de modo oscuro */}
                        <button id="theme-toggle" className="theme-toggle" onClick={toggleTheme}>
                            Modo oscuro
                        </button>

                        {/* Enlace de búsqueda */}
                        <Link to="/buscar" className="icon-button" aria-label="Buscar">
                            <span className="icon icon--search"></span>
                        </Link>

                        {/* Control de usuario: cambia dinámicamente si hay sesión activa */}
                        {isAuthenticated ? (
                            <button
                                type="button"
                                className="icon-button"
                                onClick={handleLogout}
                                aria-label="Cerrar sesión"
                                title={`Cerrar sesión de ${user?.name || user?.email}`}
                            >
                                <span className="icon icon--user" style={{ color: 'var(--primary-color, #4f46e5)' }}></span>
                            </button>
                        ) : (
                            <Link to="/login" className="icon-button" aria-label="Iniciar sesión">
                                <span className="icon icon--user"></span>
                            </Link>
                        )}
                    </div>
                </div>
            </header>

            {/* Barra de navegación de la aplicación */}
            <nav className="site-nav">
                <ul className="site-nav__list">
                    <li><a href="#sobre-nosotros" onClick={(e) => handleScrollTo(e, 'sobre-nosotros')}>Sobre Nosotros</a></li>
                    <li><a href="#como-funciona" onClick={(e) => handleScrollTo(e, 'como-funciona')}>Como funciona</a></li>
                    <li><a href="#testimonios" onClick={(e) => handleScrollTo(e, 'testimonios')}>Testimonios</a></li>

                    {/* Enlaces dinámicos según el estado de autenticación */}
                    {isAuthenticated ? (
                        <>
                            {/* Ruta protegida disponible en sesión */}
                            <li><Link to="/seguidos">Seguidos</Link></li>
                            <li>
                                <button
                                    type="button"
                                    onClick={handleLogout}
                                    style={{ background: 'none', border: 'none', cursor: 'pointer', font: 'inherit', color: 'inherit' }}
                                >
                                    Cerrar sesión ({user?.name ? user.name.split(' ')[0] : 'Usuario'})
                                </button>
                            </li>
                        </>
                    ) : (
                        <li><Link to="/register">Registrarse</Link></li>
                    )}
                </ul>
            </nav>
        </>
    );
}