import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import styles from './Forms.module.css';
import { login, register } from '../api/auth';
import { saveSession } from '../utils/auth-helper';

export default function Forms({ type = 'login' }) {
    const isLogin = type === 'login';
    const navigate = useNavigate();

    const [formData, setFormData] = useState({
        name: '',
        email: '',
        phone: '',
        password: '',
        terms: false
    });

    const [error, setError] = useState(null);
    const [isLoading, setIsLoading] = useState(false);

    // Nuevo estado para controlar la visibilidad de la contraseña
    const [showPassword, setShowPassword] = useState(false);

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData((prev) => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const validateClientSide = () => {
        if (!isLogin && !formData.terms) {
            return "Debes aceptar los términos y condiciones";
        }
        return null;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        const clientError = validateClientSide();
        if (clientError) {
            setError(clientError);
            return;
        }
        setIsLoading(true);
        try {
            let authResponse;
            if (isLogin) {
                authResponse = await login(formData.email, formData.password);
            } else {
                authResponse = await register({
                    name: formData.name,
                    email: formData.email,
                    password: formData.password
                });
            }
            saveSession(authResponse.token, {
                name: authResponse.name,
                email: authResponse.email,
                role: authResponse.role,
            });
            navigate('/');
        } catch (err) {
            setError(err.message);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className={styles['auth-form-wrapper']}>
            <form className={styles['auth-form']} onSubmit={handleSubmit}>

                {!isLogin && (
                    <div className={styles['auth-form__field']}>
                        <label className={styles['auth-form__label']} htmlFor="register-name">Nombre:</label>
                        <input
                            type="text"
                            id="register-name"
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            required
                        />
                    </div>
                )}

                <div className={styles['auth-form__field']}>
                    <label className={styles['auth-form__label']} htmlFor="email">Correo:</label>
                    <input
                        type="email"
                        id="email"
                        name="email"
                        value={formData.email}
                        onChange={handleChange}
                        required
                    />
                </div>

                {!isLogin && (
                    <div className={styles['auth-form__field']}>
                        <label className={styles['auth-form__label']} htmlFor="register-phone">Numero de telefono:</label>
                        {/* Cambio clave: type="tel" elimina las flechas de incremento */}
                        <input
                            type="tel"
                            id="register-phone"
                            name="phone"
                            value={formData.phone}
                            onChange={handleChange}
                            pattern="[0-9]*"
                        />
                    </div>
                )}

                <div className={styles['auth-form__field']}>
                    <label className={styles['auth-form__label']} htmlFor="password">Contraseña:</label>
                    <div style={{ display: 'flex', position: 'relative' }}>
                        <input
                            type={showPassword ? "text" : "password"}
                            id="password"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            required
                            style={{ width: '100%', paddingRight: '40px' }}
                        />
                        <button
                            type="button"
                            onClick={() => setShowPassword(!showPassword)}
                            aria-label={showPassword ? "Ocultar contraseña" : "Mostrar contraseña"}
                            style={{
                                position: 'absolute',
                                right: '10px',
                                top: '50%',
                                transform: 'translateY(-50%)',
                                background: 'none',
                                border: 'none',
                                cursor: 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                color: 'inherit' // Toma el color del texto actual (modo claro/oscuro)
                            }}
                        >
                            {showPassword ? (
                                // Ícono de ojo tachado (Ocultar)
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                                    <line x1="1" y1="1" x2="23" y2="23"></line>
                                </svg>
                            ) : (
                                // Ícono de ojo abierto (Mostrar)
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                    <circle cx="12" cy="12" r="3"></circle>
                                </svg>
                            )}
                        </button>
                    </div>
                </div>

                {!isLogin && (
                    <div className={styles['auth-form__terms']}>
                        <input
                            type="checkbox"
                            id="register-terms"
                            name="terms"
                            checked={formData.terms}
                            onChange={handleChange}
                            required
                        />
                        <span>Aceptar los términos y condiciones</span>
                    </div>
                )}

                {error && <p className={styles['auth-form__error']}>{error}</p>}

                <button
                    type="submit"
                    className={styles['auth-form__submit']}
                    disabled={isLoading}
                >
                    {isLoading ? 'Procesando...' : (isLogin ? 'Iniciar sesión' : 'Registrarse')}
                </button>

                {isLogin ? (
                    <p className={styles['auth-form__switch']}>
                        ¿No tienes cuenta? <Link to="/register">Regístrate</Link>
                    </p>
                ) : (
                    <p className={styles['auth-form__switch']}>
                        ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
                    </p>
                )}
            </form>
        </div>
    );
}