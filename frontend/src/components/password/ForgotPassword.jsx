import { useState } from 'react';
import { Link } from 'react-router-dom';
import { forgotPassword } from '@/api/auth';
import { CheckIcon, ShieldIcon } from '@/components/password/PasswordField';
import styles from '@/components/password/PasswordPages.module.css';

export default function ForgotPassword() {
    const [email, setEmail] = useState('');
    const [error, setError] = useState(null);
    const [sentTo, setSentTo] = useState(null);
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError(null);
        setIsLoading(true);
        try {
            await forgotPassword(email.trim());
            // Mensaje genérico a propósito: no revela si el correo existe
            setSentTo(email.trim());
        } catch (err) {
            setError(err.message);
        } finally {
            setIsLoading(false);
        }
    };

    if (sentTo) {
        return (
            <div className={styles.page}>
                <div className={styles.card}>
                    <div className={styles.successIcon}><CheckIcon size={56} /></div>
                    <header className={styles.header}>
                        <h1 className={styles.title}>Revisa tu correo</h1>
                        <p className={styles.subtitle}>
                            Si <strong>{sentTo}</strong> está registrado, te enviamos un enlace para crear una nueva
                            contraseña. Revisa también la carpeta de spam.
                        </p>
                    </header>
                    <Link to="/login" className={styles.submit}>Volver al inicio de sesión</Link>
                    <button type="button" className={styles.linkButton} onClick={() => setSentTo(null)}>
                        Usar otro correo
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className={styles.page}>
            <div className={styles.card}>
                <header className={styles.header}>
                    <h1 className={styles.title}>Recupera tu contraseña</h1>
                    <p className={styles.subtitle}>
                        Ingresa tu correo institucional o personal registrado y te enviaremos un enlace para restablecerla.
                    </p>
                </header>

                {error && <div className={`${styles.alert} ${styles.alertError}`} role="alert">{error}</div>}

                <form onSubmit={handleSubmit}>
                    <div className={styles.field}>
                        <label className={styles.label} htmlFor="email">Correo electrónico</label>
                        <div className={`${styles.control} ${styles.controlWithIcon}`}>
                            <span className={styles.icon}>
                                <svg width={18} height={18} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><rect width={20} height={16} x={2} y={4} rx={2} /><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" /></svg>
                            </span>
                            <input
                                id="email"
                                type="email"
                                className={styles.input}
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="tu@correo.com"
                                autoComplete="email"
                                disabled={isLoading}
                                required
                            />
                        </div>
                    </div>

                    <button type="submit" className={styles.submit} disabled={isLoading || !email.trim()}>
                        {isLoading ? 'Enviando...' : 'Enviar enlace'}
                    </button>
                </form>

                <Link to="/login" className={styles.backLink}>Volver al inicio de sesión</Link>

                <footer className={styles.trust}>
                    <ShieldIcon /> Tu información está protegida
                </footer>
            </div>
        </div>
    );
}