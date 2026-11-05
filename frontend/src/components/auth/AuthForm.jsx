import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { login as loginRequest, register } from '../../api/auth';
import { useAuth } from '../../context/useAuth';
import GoogleSignInButton from '../GoogleSignInButton';
import styles from '../../pages/AuthPage.module.css';

/**
 * Componente contenedor de la lógica del formulario interactivo.
 * ¿Por qué? Agrupa las variables de estado (input) y las peticiones de red (auth API) en un componente independiente.
 * Facilita el mantenimiento de validaciones complejas sin engordar la página principal.
 */
export default function AuthForm({ isLogin }) {
    const navigate = useNavigate();
    const location = useLocation();
    const from = location.state?.from?.pathname || '/';
    const { login } = useAuth();

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

        if (!isLogin && !formData.terms) {
            setError('Debes aceptar los terminos y condiciones');
            return;
        }
        setIsLoading(true);
        try {
            const authResponse = isLogin
                ? await loginRequest(formData.email, formData.password)
                : await register({ name: formData.name, email: formData.email, password: formData.password });

            login(authResponse);
            navigate(from, { replace: true });
        } catch (requestError) {
            setError(requestError.message);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className={styles['auth-form-wrapper']}>
            <form className={styles['auth-form']} onSubmit={handleSubmit}>
                <div className={styles['auth-form__header']}>
                    <h2>{isLogin ? 'Que bueno verte de nuevo' : 'Crea tu cuenta gratis'}</h2>
                    <p>{isLogin ? 'Accede a tu cuenta y sigue ahorrando.' : 'Empieza a comparar en menos de un minuto.'}</p>
                </div>

                {!isLogin && (
                    <div className={styles['auth-form__field']}>
                        <label className={styles['auth-form__label']} htmlFor="name">Nombre</label>
                        <div className={styles['input-with-icon']}>
                            <span className={styles['input-icon']}>
                                <svg width={18} height={18} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" /><circle cx={12} cy={7} r={4} /></svg>
                            </span>
                            <input type="text" id="name" name="name" value={formData.name} onChange={handleChange} placeholder="Tu nombre" autoComplete="name" required />
                        </div>
                    </div>
                )}

                <div className={styles['auth-form__field']}>
                    <label className={styles['auth-form__label']} htmlFor="email">Correo electronico</label>
                    <div className={styles['input-with-icon']}>
                        <span className={styles['input-icon']}>
                            <svg width={18} height={18} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><rect width={20} height={16} x={2} y={4} rx={2} /><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" /></svg>
                        </span>
                        <input type="email" id="email" name="email" value={formData.email} onChange={handleChange} placeholder="tu@correo.com" autoComplete="email" required />
                    </div>
                </div>

                <div className={styles['auth-form__field']}>
                    <label className={styles['auth-form__label']} htmlFor="password">Contrasena</label>
                    <div className={styles['input-with-icon']}>
                        <span className={styles['input-icon']}>
                            <svg width={18} height={18} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><rect width={18} height={11} x={3} y={11} rx={2} ry={2} /><path d="M7 11V7a5 5 0 0 1 10 0v4" /></svg>
                        </span>
                        <input type={showPassword ? 'text' : 'password'} id="password" name="password" value={formData.password} onChange={handleChange} placeholder={isLogin ? 'Ingresa tu contrasena' : 'Crea una contrasena'} autoComplete={isLogin ? 'current-password' : 'new-password'} required />
                        <button type="button" className={styles['password-toggle']} onClick={() => setShowPassword(!showPassword)} aria-label={showPassword ? 'Ocultar contrasena' : 'Mostrar contrasena'}>
                            {showPassword ? (
                                <svg width={18} height={18} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" /><path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68" /><path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61" /><line x1={2} x2={22} y1={2} y2={22} /></svg>
                            ) : (
                                <svg width={18} height={18} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" /><circle cx={12} cy={12} r={3} /></svg>
                            )}
                        </button>
                    </div>
                    {isLogin ? (
                        <div className={styles['auth-form__forgot']}><Link to="/recuperar">¿Olvidaste tu contrasena?</Link></div>
                    ) : (
                        <span className={styles['auth-form__hint']}>Minimo 8 caracteres</span>
                    )}
                </div>

                {!isLogin && (
                    <div className={styles['auth-form__terms']}>
                        <input type="checkbox" id="terms" name="terms" checked={formData.terms} onChange={handleChange} required />
                        <label htmlFor="terms">Acepto los <Link to="/terminos">Terminos y condiciones</Link> y la <Link to="/privacidad">Politica de privacidad</Link></label>
                    </div>
                )}

                {error && <p className={styles['auth-form__error']} role="alert">{error}</p>}

                <button type="submit" className={styles['auth-form__submit']} disabled={isLoading}>
                    {isLoading ? 'Iniciando...' : isLogin ? 'Iniciar sesion' : 'Empezar a comparar'}
                </button>

                {isLogin && (
                    <>
                        <div className={styles['auth-divider']}><span>o continua con</span></div>
                        <div style={{ display: 'flex', justifyContent: 'center' }}>
                            <GoogleSignInButton onCredential={(credential) => console.log('TOKEN DE GOOGLE:', credential)} onError={setError} />
                        </div>
                    </>
                )}

                <div className={styles['auth-form__switch']}>
                    {isLogin ? (
                        <>
                            <p>¿Aun no tienes una cuenta? <Link to="/register">Registrate gratis</Link></p>
                            <p className={styles['auth-form__trust']}><svg width={16} height={16} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /></svg> Acceso seguro y gratuito</p>
                        </>
                    ) : (
                        <>
                            <p>¿Ya tienes cuenta? <Link to="/login">Inicia sesion</Link></p>
                            <p className={styles['auth-form__trust']}><svg width={16} height={16} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M20 6 9 17l-5-5" /></svg> Registro gratuito - Sin tarjeta de credito</p>
                        </>
                    )}
                </div>
            </form>
        </div>
    );
}
