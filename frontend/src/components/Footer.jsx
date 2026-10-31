/**
 * @file Footer.jsx
 * @description Componente de pie de página (Footer) global de la aplicación PreciosCo.
 * Contiene enlaces de navegación interna, accesos a redes sociales, 
 * logotipo de la marca y enlaces legales.
 */

import { Link } from 'react-router-dom';

// Importación del módulo CSS para encapsular los estilos y evitar colisiones
import styles from './Footer.module.css';

/**
 * Componente Footer.
 * @returns {JSX.Element} El pie de página renderizado.
 */
export default function Footer() {
    // Obtenemos el año actual de forma dinámica para el copyright
    const currentYear = new Date().getFullYear();

    return (
        <footer className={styles['site-footer']}>
            <div className={styles['site-footer__container']}>

                {/* 1. Sección de Marca y Eslogan */}
                <div className={styles['site-footer__brand']}>
                    <Link to="/" className={styles['site-footer__logo']} aria-label="Ir al inicio">
                        <span className={styles['logo-text']}>Precios</span>
                        <img
                            src="/assets/images/co-badge.png"
                            alt="Badge Colombia"
                            className={styles['logo-co']}
                            loading="lazy"
                        />
                    </Link>
                    <p className={styles['site-footer__slogan']}>
                        Compras inteligentes para todos.
                    </p>
                </div>

                {/* 2. Sección de Navegación Interna */}
                <nav className={styles['site-footer__nav']} aria-label="Navegación del pie de página">
                    {/* Grupo 1: Enlaces principales de la plataforma */}
                    <div className={styles['site-footer__nav-group']}>
                        <Link to="/sobre-nosotros">Quiénes somos</Link>
                        <Link to="/como-funciona">Cómo funciona</Link>
                        <Link to="/contacto">Contáctanos</Link>
                    </div>
                    {/* Grupo 2: Accesos rápidos a categorías o funcionalidades */}
                    <div className={styles['site-footer__nav-group']}>
                        <Link to="/buscar?sector=supermercados">Supermercados</Link>
                        <Link to="/buscar?sector=droguerias">Farmacia</Link>
                        <Link to="/buscar?sector=repuestos">Repuestos</Link>
                    </div>
                </nav>

                {/* 3. Sección de Redes Sociales */}
                <div className={styles['site-footer__social']} aria-label="Redes sociales">
                    <a href="https://facebook.com" target="_blank" rel="noopener noreferrer" className={styles['social-link']} aria-label="Facebook">
                        {/* Se asume el uso de una clase de ícono global o puedes insertar un SVG directo */}
                        <span className="icon icon--facebook" aria-hidden="true">f</span>
                    </a>
                    <a href="https://instagram.com" target="_blank" rel="noopener noreferrer" className={styles['social-link']} aria-label="Instagram">
                        <span className="icon icon--instagram" aria-hidden="true">ig</span>
                    </a>
                    <a href="https://twitter.com" target="_blank" rel="noopener noreferrer" className={styles['social-link']} aria-label="Twitter">
                        <span className="icon icon--twitter" aria-hidden="true">x</span>
                    </a>
                </div>
            </div>

            {/* 4. Sección Inferior (Copyright y Legales) */}
            <div className={styles['site-footer__bottom']}>
                <p>&copy; {currentYear} PreciosCo. Todos los derechos reservados.</p>
                <div className={styles['site-footer__legal-links']}>
                    <Link to="/terminos">Términos y Condiciones</Link>
                    <Link to="/privacidad">Política de Privacidad</Link>
                </div>
            </div>
        </footer>
    );
}