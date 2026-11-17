import { login as loginRequest, register } from '@/api/auth';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import GoogleSignInButton from './GoogleSignInButton';
import styles from '../../pages/AuthPage.module.css';

/* Íconos del formulario. Son decorativos (aria-hidden): el significado lo da la etiqueta. */
function Icon({ children }) {
    return (
        <svg width={18} height={18} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}
            strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            {children}
        </svg>
    );
}

const UserIcon = () => (
    <Icon>
        <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
        <circle cx={12} cy={7} r={4} />
    </Icon>
);

const MailIcon = () => (
    <Icon>
        <rect width={20} height={16} x={2} y={4} rx={2} />
        <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
    </Icon>
);

const LockIcon = () => (
    <Icon>
        <rect width={18} height={11} x={3} y={11} rx={2} ry={2} />
        <path d="M7 11V7a5 5 0 0 1 10 0v4" />
    </Icon>
);

const EyeIcon = () => (
    <Icon>
        <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
        <circle cx={12} cy={12} r={3} />
    </Icon>
);

const EyeOffIcon = () => (
    <Icon>
        <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" />
        <path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68" />
        <path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61" />
        <line x1={2} x2={22} y1={2} y2={22} />
    </Icon>
);

/**
 * Formulario de inicio de sesión o de registro.
 * Se muestran los DOS a la vez (uno por cada lado de la pista deslizante), así que cada uno
 * guarda su propio estado y prefija sus `id`: dos campos con el mismo id romperían las etiquetas.
 * La navegación posterior al ingreso y el inicio con Google los resuelve la página (AuthPage).
 *
 * @param {boolean} isLogin true = inicio de sesión, false = registro
 * @param {object} switchState estado de la ruta (a dónde volver); se conserva al cambiar de modo
 * @param {(authResponse: object) => void} onAuthenticated se llama con la respuesta del backend
 * @param {(credential: string) => void} onGoogleCredential credencial entregada por Google
 * @param {(message: string|null) => void} onGoogleError error del botón de Google
 * @param {string|null} googleError mensaje de error de Google (lo guarda la página)
 * @param {boolean} googleBusy true mientras el backend valida la cuenta de Google
 */
export default function AuthForm({
    isLogin,
    switchState,
    onAuthenticated,
    onGoogleCredential,
    onGoogleError,
    googleError,
    googleBusy,
}) {
    const prefix = isLogin ? 'login' : 'register';

    const [formData, setFormData] = useState({ name: '', email: '', password: '', terms: false });
    const [error, setError] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [showPassword, setShowPassword] = useState(false);

    const handleChange = (event) => {
        const { name, value, type, checked } = event.target;
        setFormData((prev) => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError(null);
        onGoogleError(null);

        if (!isLogin && !formData.terms) {
            setError('Debes aceptar los términos y condiciones');
            return;
        }
        setIsLoading(true);
        try {
            const authResponse = isLogin
                ? await loginRequest(formData.email, formData.password)
                : await register({ name: formData.name, email: formData.email, password: formData.password });

            onAuthenticated(authResponse);
        } catch (requestError) {
            setError(requestError.message);
        } finally {
            setIsLoading(false);
        }
    };

    const submitLabel = isLogin ? 'Iniciar sesión' : 'Crear cuenta';
    const loadingLabel = isLogin ? 'Iniciando...' : 'Creando cuenta...';

    return (
        <form className={styles['auth-form']} onSubmit={handleSubmit}>
            <div className={styles['auth-form__header']}>
                {/* Solo hay un <h1> visible a la vez: el panel inactivo queda oculto para lectores de pantalla */}
                <h1>{isLogin ? 'Bienvenido de nuevo' : 'Crea tu cuenta'}</h1>
                <p>{isLogin ? 'Entra y compara precios en las tiendas conectadas.' : 'Regístrate para comparar precios y seguir tus productos.'}</p>
            </div>

            {/* Google va primero, como en el diseño de referencia. El botón es el mismo componente de siempre. */}
            <div className={styles['auth-form__google']}>
                <GoogleSignInButton onCredential={onGoogleCredential} onError={onGoogleError} />
            </div>
            {googleError && <p className={styles['auth-form__error']} role="alert">{googleError}</p>}

            <div className={styles['auth-divider']}>
                <span>o con tu correo</span>
            </div>

            {!isLogin && (
                <div className={styles['auth-form__field']}>
                    <label className={styles['auth-form__label']} htmlFor={`${prefix}-name`}>Nombre</label>
                    <div className={styles['input-with-icon']}>
                        <span className={styles['input-icon']}><UserIcon /></span>
                        <input
                            className={styles['auth-form__input']}
                            type="text"
                            id={`${prefix}-name`}
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            placeholder="Tu nombre"
                            autoComplete="name"
                            required
                        />
                    </div>
                </div>
            )}

            <div className={styles['auth-form__field']}>
                <label className={styles['auth-form__label']} htmlFor={`${prefix}-email`}>Correo</label>
                <div className={styles['input-with-icon']}>
                    <span className={styles['input-icon']}><MailIcon /></span>
                    <input
                        className={styles['auth-form__input']}
                        type="email"
                        id={`${prefix}-email`}
                        name="email"
                        value={formData.email}
                        onChange={handleChange}
                        placeholder="tucorreo@email.com"
                        autoComplete="email"
                        required
                    />
                </div>
            </div>

            <div className={styles['auth-form__field']}>
                <label className={styles['auth-form__label']} htmlFor={`${prefix}-password`}>Contraseña</label>
                <div className={`${styles['input-with-icon']} ${styles['input-with-icon--password']}`}>
                    <span className={styles['input-icon']}><LockIcon /></span>
                    <input
                        className={styles['auth-form__input']}
                        type={showPassword ? 'text' : 'password'}
                        id={`${prefix}-password`}
                        name="password"
                        value={formData.password}
                        onChange={handleChange}
                        placeholder={isLogin ? 'Tu contraseña' : 'Mínimo 8 caracteres'}
                        autoComplete={isLogin ? 'current-password' : 'new-password'}
                        required
                    />
                    <button
                        type="button"
                        className={styles['password-toggle']}
                        onClick={() => setShowPassword(!showPassword)}
                        aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                    >
                        {showPassword ? <EyeOffIcon /> : <EyeIcon />}
                    </button>
                </div>
                {isLogin ? (
                    <div className={styles['auth-form__forgot']}>
                        <Link to="/forgot-password">¿Olvidaste tu contraseña?</Link>
                    </div>
                ) : (
                    <span className={styles['auth-form__hint']}>Mínimo 8 caracteres</span>
                )}
            </div>

            {!isLogin && (
                <div className={styles['auth-form__terms']}>
                    <input type="checkbox" id={`${prefix}-terms`} name="terms" checked={formData.terms} onChange={handleChange} required />
                    <label htmlFor={`${prefix}-terms`}>
                        Acepto los <Link to="/terminos">Términos y condiciones</Link> y la <Link to="/privacidad">Política de privacidad</Link>
                    </label>
                </div>
            )}

            {error && <p className={styles['auth-form__error']} role="alert">{error}</p>}

            <button type="submit" className={styles['auth-form__submit']} disabled={isLoading || googleBusy}>
                {isLoading ? loadingLabel : submitLabel}
            </button>

            <p className={styles['auth-form__switch']}>
                {isLogin ? (
                    <>¿No tienes cuenta? <Link to="/register" state={switchState}>Crear cuenta</Link></>
                ) : (
                    <>¿Ya tienes cuenta? <Link to="/login" state={switchState}>Inicia sesión</Link></>
                )}
            </p>
        </form>
    );
}