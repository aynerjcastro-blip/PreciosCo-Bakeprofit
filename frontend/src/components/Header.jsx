import { Link, useLocation, useNavigate } from 'react-router-dom';
import { toggleTheme } from '../utils/theme';

export default function Header() {
    const location = useLocation();
    const navigate = useNavigate();

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

    return (
        <>
            <header className="site-header">
                <div className="site-header__inner">
                    <button className="icon-button" type="button" aria-label="Abrir menú">
                        <span className="icon icon--menu"></span>
                    </button>

                    <Link to="/" className="site-header__title">
                        <span className="logo-text">Precios</span>
                        <img src="/assets/images/co-badge.png" alt="Badge Colombia" className="logo-co" />
                    </Link>

                    <div className="header-actions">
                        <button id="theme-toggle" className="theme-toggle" onClick={toggleTheme}>
                            Modo oscuro
                        </button>
                        <Link to="/buscar" className="icon-button" aria-label="Buscar">
                            <span className="icon icon--search"></span>
                        </Link>
                        <Link to="/login" className="icon-button" aria-label="Iniciar sesión">
                            <span className="icon icon--user"></span>
                        </Link>
                    </div>
                </div>
            </header>

            <nav className="site-nav">
                <ul className="site-nav__list">
                    <li><a href="#sobre-nosotros" onClick={(e) => handleScrollTo(e, 'sobre-nosotros')}>Sobre Nosotros</a></li>
                    <li><a href="#como-funciona" onClick={(e) => handleScrollTo(e, 'como-funciona')}>Como funciona</a></li>
                    <li><a href="#testimonios" onClick={(e) => handleScrollTo(e, 'testimonios')}>Testimonios</a></li>
                    <li><Link to="/register">Registrarse</Link></li>
                </ul>
            </nav>
        </>
    );
}