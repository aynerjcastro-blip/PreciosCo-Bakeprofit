import { useEffect, useState } from 'react';
import { changePassword, getMyProfile } from '@/api/users';
import PasswordField, { CheckIcon } from '@/components/password/PasswordField';
import passwordStyles from '@/components/password/PasswordPages.module.css';
import styles from '@/components/profile/ChangePasswordForm.module.css';

const MIN_LENGTH = 8;

/**
 * Sección «Cambiar contraseña» del perfil: pide la actual, la nueva y su confirmación.
 * Se encarga ella sola de decidir qué mostrar según el tipo de cuenta:
 * formulario para cuentas con contraseña propia, aviso para las de Google, nada mientras carga.
 */
export default function ChangePasswordForm() {
    // null = aún no se sabe; true = cuenta con contraseña propia; false = cuenta de Google
    const [hasOwnPassword, setHasOwnPassword] = useState(null);

    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        let cancelled = false;
        getMyProfile()
            // emailEditable es true solo en las cuentas locales, que son las que tienen contraseña propia
            .then((profile) => { if (!cancelled) setHasOwnPassword(Boolean(profile.emailEditable)); })
            .catch(() => { /* sin perfil no se muestra la sección; el resto del perfil sigue funcionando */ });
        return () => { cancelled = true; };
    }, []);

    const matches = confirmPassword !== '' && newPassword === confirmPassword;
    const sameAsCurrent = newPassword !== '' && newPassword === currentPassword;
    const canSubmit = currentPassword !== ''
        && newPassword.length >= MIN_LENGTH
        && !sameAsCurrent
        && matches
        && !isLoading;

    // Al volver a escribir se limpian los mensajes anteriores
    const handleChange = (setter) => (event) => {
        setter(event.target.value);
        setError(null);
        setSuccess(false);
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        if (!canSubmit) return;

        setError(null);
        setSuccess(false);
        setIsLoading(true);
        try {
            await changePassword(currentPassword, newPassword);
            setSuccess(true);
            setCurrentPassword('');
            setNewPassword('');
            setConfirmPassword('');
        } catch (err) {
            setError(err.status === 401 ? 'Tu sesión expiró. Inicia sesión de nuevo.' : err.message);
        } finally {
            setIsLoading(false);
        }
    };

    if (hasOwnPassword === null) return null;

    if (!hasOwnPassword) {
        return (
            <section className={styles.section} aria-labelledby="change-password-title">
                <h2 id="change-password-title" className={styles.title}>Contraseña</h2>
                <p className={styles.note}>
                    Iniciaste sesión con Google, por eso tu cuenta no tiene una contraseña que cambiar.
                </p>
            </section>
        );
    }

    return (
        <section className={styles.section} aria-labelledby="change-password-title">
            <h2 id="change-password-title" className={styles.title}>Cambiar contraseña</h2>
            <p className={styles.note}>Escribe tu contraseña actual y elige una nueva.</p>

            {error && <div className={`${styles.alert} ${styles.alertError}`} role="alert">{error}</div>}
            {success && (
                <div className={`${styles.alert} ${styles.alertSuccess}`} role="status">
                    Contraseña actualizada correctamente.
                </div>
            )}

            <form onSubmit={handleSubmit} className={styles.form}>
                <PasswordField
                    id="currentPassword"
                    label="Contraseña actual"
                    value={currentPassword}
                    onChange={handleChange(setCurrentPassword)}
                    autoComplete="current-password"
                    disabled={isLoading}
                />

                <PasswordField
                    id="newPassword"
                    label="Nueva contraseña"
                    value={newPassword}
                    onChange={handleChange(setNewPassword)}
                    autoComplete="new-password"
                    disabled={isLoading}
                >
                    {sameAsCurrent ? (
                        <p className={`${passwordStyles.match} ${passwordStyles.matchBad}`} aria-live="polite">
                            La nueva contraseña debe ser distinta de la actual
                        </p>
                    ) : (
                        <span className={passwordStyles.hint}>Mínimo {MIN_LENGTH} caracteres</span>
                    )}
                </PasswordField>

                <PasswordField
                    id="confirmNewPassword"
                    label="Confirmar nueva contraseña"
                    value={confirmPassword}
                    onChange={handleChange(setConfirmPassword)}
                    autoComplete="new-password"
                    disabled={isLoading}
                >
                    {confirmPassword && (
                        <p className={`${passwordStyles.match} ${matches ? passwordStyles.matchOk : passwordStyles.matchBad}`} aria-live="polite">
                            {matches && <CheckIcon />}
                            {matches ? 'Las contraseñas coinciden' : 'Las contraseñas no coinciden'}
                        </p>
                    )}
                </PasswordField>

                <button type="submit" className={styles.submit} disabled={!canSubmit}>
                    {isLoading ? 'Actualizando...' : 'Actualizar contraseña'}
                </button>
            </form>
        </section>
    );
}