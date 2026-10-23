import { Link, useLocation, useNavigate } from 'react-router-dom';

export default function Footer() {
    const location = useLocation();
    const navigate = useNavigate();

    // Reutilizamos la lógica de scroll que quitamos del Header
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
            {/* Navegación Principal en el Footer */}
            <ul className="site-footer__links">
                <li><Link to="/"><h5>Inicio</h5></Link></li>
                <li><Link to="/buscar"><h5>Buscador</h5></Link></li>
                <li><a href="#sobre-nosotros" onClick={(e) => handleScrollTo(e, 'sobre-nosotros')}><h5>Sobre Nosotros</h5></a></li>
                <li><a href="#como-funciona" onClick={(e) => handleScrollTo(e, 'como-funciona')}><h5>Cómo Funciona</h5></a></li>
                <li><a href="#testimonios" onClick={(e) => handleScrollTo(e, 'testimonios')}><h5>Testimonios</h5></a></li>
            </ul>

            {/* Enlaces de Soporte */}
            <ul className="site-footer__links" style={{ marginTop: '16px', gap: '20px' }}>
                <li><Link to="/faq">Preguntas y Respuestas</Link></li>
                <li><Link to="/contacto">Contáctanos</Link></li>
            </ul>

            <div className="site-footer__copy">
                <p>Todos los derechos reservados © {new Date().getFullYear()} PreciosCo</p>
            </div>
        </footer>
    );
}