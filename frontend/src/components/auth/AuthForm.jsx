import { login as loginRequest, register, googleLogin } from '@/api/auth';
import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../../context/useAuth';
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

const ShieldIcon = () => (
    <svg width={16} height={16} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}
        strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
    </svg>
);

const CheckIcon = () => (
    <svg width={16} height={16} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}
        strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M20 6 9 17l-5-5" />
    </svg>
);

/**
 * Formulario de inicio de sesión y registro.
 * Concentra el estado de los campos y las peticiones a la API en un solo componente,
 * para que la página solo decida cuál de los dos modos mostrar.
 *
 * @param {boolean} isLogin true = inicio de sesión, false = registro
 */
export default function AuthForm({ isLogin }) {
    const navigate = useNavigate();
    const location = useLocation();
    // Ruta protegida desde la que llegó el usuario; se le devuelve ahí al entrar
    const from = location.state?.from?.pathname || '/';
    const { login } = useAuth();

    const [formData, setFormData] = useState({ name: '', email: '', password: '', terms: false });
    // Errores separados para mostrar cada uno junto al botón que el usuario pulsó
    const [error, setError] = useState(null);
    const [googleError, setGoogleError] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [showPassword, setShowPassword] = useState(false);

    const handleChange = (event) => {
        const { name, value, type, checked } = event.target;
        setFormData((prev) => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    // replace evita que el botón "atrás" regrese a /login con la sesión ya iniciada
    const finishLogin = (authResponse) => {
        login(authResponse);
        navigate(from, { replace: true });
    };

    const handleGoogleCredential = async (credential) => {
        setError(null);
        setGoogleError(null);
        setIsLoading(true);
        try {
            // El token de Google se valida en el backend; el front nunca lo da por bueno
            const authResponse = await googleLogin(credential);
            finishLogin(authResponse);
        } catch (err) {
            // El backend explica el motivo (p. ej. cuenta desactivada); sin mostrarlo, el usuario no se entera
            setGoogleError(err.message || 'Error al iniciar sesión con Google');
        } finally {
            setIsLoading(false);
        }
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError(null);
        setGoogleError(null);

        if (!isLogin && !formData.terms) {
            setError('Debes aceptar los términos y condiciones');
            return;
        }
        setIsLoading(true);
        try {
            const authResponse = isLogin
                ? await loginRequest(formData.email, formData.password)
                : await register({ name: formData.name, email: formData.email, password: formData.password });

            finishLogin(authResponse);
        } catch (requestError) {
            setError(requestError.message);
        } finally {
            setIsLoading(false);
        }
    };

    const submitLabel = isLogin ? 'Iniciar sesión' : 'Empezar a comparar';
    const loadingLabel = isLogin ? 'Iniciando...' : 'Creando cuenta...';

    return (
        <div className={styles['auth-form-wrapper']}>
            <form className={styles['auth-form']} onSubmit={handleSubmit}>
                <div className={styles['auth-form__header']}>
                    <h2>{isLogin ? 'Qué bueno verte de nuevo' : 'Crea tu cuenta gratis'}</h2>
                    <p>{isLogin ? 'Accede a tu cuenta y sigue ahorrando.' : 'Empieza a comparar en menos de un minuto.'}</p>
                </div>

                {!isLogin && (
                    <div className={styles['auth-form__field']}>
                        <label className={styles['auth-form__label']} htmlFor="name">Nombre</label>
                        <div className={styles['input-with-icon']}>
                            <span className={styles['input-icon']}><UserIcon /></span>
                            <input
                                className={styles['auth-form__input']}
                                type="text"
                                id="name"
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
                    <label className={styles['auth-form__label']} htmlFor="email">Correo electrónico</label>
                    <div className={styles['input-with-icon']}>
                        <span className={styles['input-icon']}><MailIcon /></span>
                        <input
                            className={styles['auth-form__input']}
                            type="email"
                            id="email"
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            placeholder="tu@correo.com"
                            autoComplete="email"
                            required
                        />
                    </div>
                </div>

                <div className={styles['auth-form__field']}>
                    <label className={styles['auth-form__label']} htmlFor="password">Contraseña</label>
                    <div className={`${styles['input-with-icon']} ${styles['input-with-icon--password']}`}>
                        <span className={styles['input-icon']}><LockIcon /></span>
                        <input
                            className={styles['auth-form__input']}
                            type={showPassword ? 'text' : 'password'}
                            id="password"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            placeholder={isLogin ? 'Ingresa tu contraseña' : 'Crea una contraseña'}
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
                        <input type="checkbox" id="terms" name="terms" checked={formData.terms} onChange={handleChange} required />
                        <label htmlFor="terms">
                            Acepto los <Link to="/terminos">Términos y condiciones</Link> y la <Link to="/privacidad">Política de privacidad</Link>
                        </label>
                    </div>
                )}

                {error && <p className={styles['auth-form__error']} role="alert">{error}</p>}

                <button type="submit" className={styles['auth-form__submit']} disabled={isLoading}>
                    {isLoading ? loadingLabel : submitLabel}
                </button>

                {/* Solo en login: Google crea la cuenta en el primer ingreso, no hace falta flujo de registro aparte */}
                {isLogin && (
                    <>
                        <div className={styles['auth-divider']}>
                            <span>o continúa con</span>
                        </div>

                        <div className={styles['auth-form__google']}>
                            <GoogleSignInButton
                                onCredential={handleGoogleCredential}
                                onError={setGoogleError}
                            />
                        </div>

                        {googleError && <p className={styles['auth-form__error']} role="alert">{googleError}</p>}
                    </>
                )}

                <div className={styles['auth-form__switch']}>
                    {isLogin ? (
                        <>
                            <p>¿Aún no tienes una cuenta? <Link to="/register">Regístrate gratis</Link></p>
                            <p className={styles['auth-form__trust']}><ShieldIcon /> Acceso seguro y gratuito</p>
                        </>
                    ) : (
                        <>
                            <p>¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link></p>
                            <p className={styles['auth-form__trust']}><CheckIcon /> Registro gratuito · Sin tarjeta de crédito</p>
                        </>
                    )}
                </div>
            </form>
        </div>
    );
}