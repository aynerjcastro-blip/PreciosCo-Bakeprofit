// src/components/Header.jsx
import { Link } from 'react-router-dom';
import { toggleTheme } from '../utils/theme'; // Asumiendo que migraremos theme.js

export default function Header() {
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
                    <li><Link to="/#sobre-nosotros">Sobre Nosotros</Link></li>
                    <li><Link to="/#como-funciona">Como funciona</Link></li>
                    <li><Link to="/#testimonios">Testimonios</Link></li>
                    <li><Link to="/register">Registrarse</Link></li>
                </ul>
            </nav>
        </>
    );
}