import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { resetPassword } from '@/api/auth';
import PasswordField, { CheckIcon, ShieldIcon } from '@/components/password/PasswordField';
import styles from '@/components/password/PasswordPages.module.css';

const MIN_LENGTH = 8;
const STRENGTH_LABELS = ['', 'Débil', 'Regular', 'Segura', 'Muy segura'];

// Solo orienta al usuario: el backend exige únicamente la longitud mínima
function getStrength(password) {
    if (!password) return 0;
    const passed = [
        password.length >= MIN_LENGTH,
        /[a-z]/.test(password) && /[A-Z]/.test(password),
        /\d/.test(password),
        /[^A-Za-z0-9]/.test(password),
    ].filter(Boolean).length;
    return Math.max(1, passed);
}

export default function ResetPassword() {
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const navigate = useNavigate();

    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState(null);
    const [successMessage, setSuccessMessage] = useState(null);
    const [isLoading, setIsLoading] = useState(false);

    const strength = getStrength(newPassword);
    const matches = confirmPassword !== '' && newPassword === confirmPassword;
    const canSubmit = newPassword.length >= MIN_LENGTH && matches && !isLoading && !successMessage;

    // El temporizador vive en un efecto para cancelarlo si el usuario sale antes
    useEffect(() => {
        if (!successMessage) return;
        const timer = setTimeout(() => navigate('/login', { replace: true }), 3000);
        return () => clearTimeout(timer);
    }, [successMessage, navigate]);

    const handleSubmit = async (event) => {
        event.preventDefault();
        if (!canSubmit) return;

        setError(null);
        setIsLoading(true);
        try {
            const data = await resetPassword(token, newPassword);
            setSuccessMessage(data.message || 'Contraseña actualizada. Ya puedes iniciar sesión.');
        } catch (err) {
            setError(err.message);
        } finally {
            setIsLoading(false);
        }
    };

    if (!token) {
        return (
            <div className={styles.page}>
                <div className={styles.card}>
                    <header className={styles.header}>
                        <h1 className={styles.title}>Enlace no válido</h1>
                        <p className={styles.subtitle}>El enlace de recuperación es inválido o ya venció.</p>
                    </header>
                    <Link to="/recuperar" className={styles.submit}>Solicitar un nuevo enlace</Link>
                    <Link to="/login" className={styles.backLink}>Volver al inicio de sesión</Link>
                </div>
            </div>
        );
    }

    return (
        <div className={styles.page}>
            <div className={styles.card}>
                <header className={styles.header}>
                    <h1 className={styles.title}>Crea una nueva contraseña</h1>
                    <p className={styles.subtitle}>Protege tu cuenta con una contraseña segura y fácil de recordar.</p>
                </header>

                {error && <div className={`${styles.alert} ${styles.alertError}`} role="alert">{error}</div>}
                {successMessage && (
                    <div className={`${styles.alert} ${styles.alertSuccess}`} role="status">
                        {successMessage} Te llevaremos al inicio de sesión.
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <PasswordField
                        id="newPassword"
                        label="Nueva contraseña"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        autoComplete="new-password"
                        disabled={isLoading || Boolean(successMessage)}
                    >
                        <span className={styles.hint}>Mínimo {MIN_LENGTH} caracteres</span>
                    </PasswordField>

                    {newPassword && (
                        <div className={styles.strength} data-level={strength} aria-live="polite">
                            <div className={styles.strengthHead}>
                                <span>Seguridad de la contraseña</span>
                                <span className={styles.strengthLabel}>{STRENGTH_LABELS[strength]}</span>
                            </div>
                            <div className={styles.bars} aria-hidden="true">
                                {[0, 1, 2, 3].map((i) => <span key={i} className={styles.bar} />)}
                            </div>
                        </div>
                    )}

                    <PasswordField
                        id="confirmPassword"
                        label="Confirmar contraseña"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        autoComplete="new-password"
                        disabled={isLoading || Boolean(successMessage)}
                    >
                        {confirmPassword && (
                            <p className={`${styles.match} ${matches ? styles.matchOk : styles.matchBad}`} aria-live="polite">
                                {matches && <CheckIcon />}
                                {matches ? 'Las contraseñas coinciden' : 'Las contraseñas no coinciden'}
                            </p>
                        )}
                    </PasswordField>

                    <button type="submit" className={styles.submit} disabled={!canSubmit}>
                        {isLoading ? 'Actualizando...' : 'Restablecer contraseña'}
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