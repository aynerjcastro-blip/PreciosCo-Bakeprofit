import { Link, useLocation, useNavigate } from 'react-router-dom';

export default function Footer() {
    const location = useLocation();
    const navigate = useNavigate();

    const handleScrollTo = (e, elementId) => {
        e.preventDefault();
        const scroll = () => document.getElementById(elementId)?.scrollIntoView({ behavior: 'smooth' });
        
        if (location.pathname !== '/') {
            navigate('/');
            setTimeout(scroll, 100);
        } else {
            scroll();
        }
    };

    return (
        <footer className="site-footer">
            <div className="site-footer__container">
                {/* Columna Izquierda: Brand */}
                <div className="site-footer__brand">
                    <Link to="/" className="site-footer__logo">
                        <span className="logo-text">Precios</span>
                        <img src="/assets/images/co-badge.png" alt="Badge Colombia" className="logo-co" />
                    </Link>
                    <p className="site-footer__slogan">Compras inteligentes para todos.</p>
                </div>

                {/* Centro: Columnas de Enlaces */}
                <div className="site-footer__nav">
                    <div className="site-footer__nav-group">
                        <Link to="/">Inicio</Link>
                        <Link to="/buscar">Buscador</Link>
                        <a href="#sobre-nosotros" onClick={(e) => handleScrollTo(e, 'sobre-nosotros')}>Sobre Nosotros</a>
                        <a href="#como-funciona" onClick={(e) => handleScrollTo(e, 'como-funciona')}>Como funciona</a>
                        <a href="#testimonios" onClick={(e) => handleScrollTo(e, 'testimonios')}>Testimonios</a>
                    </div>
                    
                    <div className="site-footer__nav-group">
                        <Link to="/faq">Preguntas y Respuestas</Link>
                        <Link to="/contacto">Contactanos</Link>
                        <Link to="/terminos">Terminos y condiciones</Link>
                        <Link to="/privacidad">Politica de privacidad</Link>
                    </div>
                </div>

                {/* Columna Derecha: Redes Sociales */}
                <div className="site-footer__social">
                    <a href="https://facebook.com" target="_blank" rel="noopener noreferrer" aria-label="Facebook">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z"></path>
                        </svg>
                    </a>
                    <a href="https://instagram.com" target="_blank" rel="noopener noreferrer" aria-label="Instagram">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <rect x="2" y="2" width="20" height="20" rx="5" ry="5"></rect>
                            <path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"></path>
                            <line x1="17.5" y1="6.5" x2="17.51" y2="6.5"></line>
                        </svg>
                    </a>
                    <a href="https://youtube.com" target="_blank" rel="noopener noreferrer" aria-label="YouTube">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M22.54 6.42a2.78 2.78 0 0 0-1.94-2C18.88 4 12 4 12 4s-6.88 0-8.6.46a2.78 2.78 0 0 0-1.94 2A29 29 0 0 0 1 11.75a29 29 0 0 0 .46 5.33A2.78 2.78 0 0 0 3.4 19c1.72.46 8.6.46 8.6.46s6.88 0 8.6-.46a2.78 2.78 0 0 0 1.94-2 29 29 0 0 0 .46-5.25 29 29 0 0 0-.46-5.33z"></path>
                            <polygon points="9.75 15.02 15.5 11.75 9.75 8.48 9.75 15.02"></polygon>
                        </svg>
                    </a>
                    <a href="https://linkedin.com" target="_blank" rel="noopener noreferrer" aria-label="LinkedIn">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M16 8a6 6 0 0 1 6 6v7h-4v-7a2 2 0 0 0-2-2 2 2 0 0 0-2 2v7h-4v-7a6 6 0 0 1 6-6z"></path>
                            <rect x="2" y="9" width="4" height="12"></rect>
                            <circle cx="4" cy="4" r="2"></circle>
                        </svg>
                    </a>
                </div>
            </div>

            <div className="site-footer__bottom">
                <p>Todos los derechos reservados © {new Date().getFullYear()} PreciosCo</p>
                <div className="site-footer__legal-links">
                    <Link to="/faq">Preguntas y Respuestas</Link>
                    <Link to="/contacto">Contactanos</Link>
                </div>
            </div>
        </footer>
    );
}