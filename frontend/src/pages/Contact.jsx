import { Link } from 'react-router-dom';
import styles from './Contact.module.css';
import { CONTACT_EMAIL } from '@/config/contact';

function MailIcon() {
    return (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
            strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
            <rect x="3" y="5" width="18" height="14" rx="2" />
            <path d="M3 7l9 6 9-6" />
        </svg>
    );
}

function QuestionIcon() {
    return (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
            strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
            <circle cx="12" cy="12" r="10" />
            <path d="M9.1 9a3 3 0 0 1 5.8 1c0 2-3 3-3 3" />
            <path d="M12 17h.01" />
        </svg>
    );
}

function ChatIcon() {
    return (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
            strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
            <path d="M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z" />
        </svg>
    );
}

/** Página de contacto: correo del equipo, preguntas frecuentes y asistentes. */
export default function Contact() {
    return (
        <div className={styles.page}>
            <header className={styles.hero}>
                <h1 className={styles.title}>Contáctanos</h1>
                <p className={styles.lead}>
                    ¿Encontraste un precio desactualizado, una tienda que falta o tienes una sugerencia?
                    Queremos saberlo.
                </p>
            </header>

            <div className={styles.cards}>
                {CONTACT_EMAIL && (
                    <a className={styles.card} href={`mailto:${CONTACT_EMAIL}`}>
                        <span className={styles.icon}><MailIcon /></span>
                        <span className={styles.cardTitle}>Escríbenos</span>
                        <span className={styles.cardText}>{CONTACT_EMAIL}</span>
                        <span className={styles.cardHint}>Te respondemos lo antes posible.</span>
                    </a>
                )}

                <Link className={styles.card} to="/#preguntas-frecuentes">
                    <span className={styles.icon}><QuestionIcon /></span>
                    <span className={styles.cardTitle}>Preguntas frecuentes</span>
                    <span className={styles.cardText}>Cómo comparamos, de dónde salen los precios y más.</span>
                </Link>

                <Link className={styles.card} to="/#como-funciona">
                    <span className={styles.icon}><ChatIcon /></span>
                    <span className={styles.cardTitle}>Cómo funciona</span>
                    <span className={styles.cardText}>
                        Busca, compara entre tiendas y sigue productos para recibir avisos de baja de precio.
                    </span>
                </Link>
            </div>

            <p className={styles.note}>
                Para reportar un precio incorrecto, incluye el nombre del producto, la tienda y, si puedes,
                el enlace o una captura.
            </p>
        </div>
    );
}
