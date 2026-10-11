
import { Link } from 'react-router-dom';

export default function Footer() {
    return (
        <footer className="site-footer">
            <ul className="site-footer__links">
                <li>
                    <Link to="/faq">
                        <h5>Preguntas Y Respuestas</h5>
                    </Link>
                </li>
                <li>
                    <Link to="/contacto">
                        <h5>Contáctanos</h5>
                    </Link>
                </li>
            </ul>
            <h5 className="site-footer__copy">Todos los derechos reservados</h5>
        </footer>
    );
}