import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import styles from './Forms.module.css';

// Funciones que hablan con el backend. Se renombra `login` a `loginRequest`
// porque el contexto tambien expone una funcion `login` con otro proposito.
import { login as loginRequest, register } from '../api/auth';

// Hook de sesion: expone login(), logout(), user, isAuthenticated, etc.
import { useAuth } from '../context/useAuth';

export default function Forms({ type = 'login' }) {
    const isLogin = type === 'login';
    const navigate = useNavigate();
    const location = useLocation();
// Una ruta protegida manda aquí al usuario; al entrar lo devolvemos a donde iba
const from = location.state?.from?.pathname || '/';

    /*
     * login() del CONTEXTO: recibe la respuesta (AuthResponse) que ya devolvio
     * el backend, la guarda en localStorage y actualiza el estado global.
     * NO hace ninguna peticion HTTP. Para eso esta loginRequest().
     */
    const { login } = useAuth();

    // Valores de los campos del formulario.
    const [formData, setFormData] = useState({
        name: '',
        email: '',
        password: '',
        terms: false
    });

    // Mensaje de error para mostrar al usuario (null = sin error).
    const [error, setError] = useState(null);

    // true mientras se espera la respuesta del backend (deshabilita el boton).
    const [isLoading, setIsLoading] = useState(false);

    // Controla si la contrasena se ve en texto plano o con puntos.
    const [showPassword, setShowPassword] = useState(false);

    /**
     * Actualiza un campo del formulario.
     * Funciona para todos los inputs usando el atributo `name` como clave.
     * Los checkbox guardan `checked` (boolean) en vez de `value` (string).
     */
    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData((prev) => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    /**
     * Validaciones que no necesitan al backend.
     * @returns {string|null} mensaje de error, o null si todo esta bien
     */
    const validateClientSide = () => {
        if (!isLogin && !formData.terms) {
            return 'Debes aceptar los términos y condiciones';
        }
        return null;
    };

    /**
     * Envia el formulario.
     * 1) Valida en el cliente.
     * 2) Pide al backend el login o el registro.
     * 3) Entrega la respuesta al contexto (guarda la sesion).
     * 4) Redirige al inicio.
     * Si el backend rechaza la peticion (credenciales invalidas, correo
     * duplicado, cuenta inactiva...), api/auth.js lanza un Error con el
     * mensaje del backend y aqui se muestra en pantalla.
     */
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
            // Paso 1: peticion al backend (devuelve token, name, email, role).
            const authResponse = isLogin
                ? await loginRequest(formData.email, formData.password)
                : await register({
                    name: formData.name,
                    email: formData.email,
                    password: formData.password
                });

            // Paso 2: el contexto guarda la sesion y notifica a la app.
            login(authResponse);

            // Paso 3: volver al inicio con la sesion ya activa.
            navigate(from, { replace: true });
        } catch (err) {
            setError(err.message);
        } finally {
            // Se ejecuta siempre, haya error o no: reactiva el boton.
            setIsLoading(false);
        }
    };

    return (
        <div className={styles['auth-form-wrapper']}>
            <form className={styles['auth-form']} onSubmit={handleSubmit}>

                {/* Nombre: solo se pide al registrarse */}
                {!isLogin && (
                    <div className={styles['auth-form__field']}>
                        <label className={styles['auth-form__label']} htmlFor="register-name">Nombre:</label>
                        <input
                            type="text"
                            id="register-name"
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            autoComplete="name"
                            required
                        />
                    </div>
                )}

                {/* Correo */}
                <div className={styles['auth-form__field']}>
                    <label className={styles['auth-form__label']} htmlFor="email">Correo:</label>
                    <input
                        type="email"
                        id="email"
                        name="email"
                        value={formData.email}
                        onChange={handleChange}
                        autoComplete="email"
                        required
                    />
                </div>

                {/* Contrasena con boton para mostrar u ocultar */}
                <div className={styles['auth-form__field']}>
                    <label className={styles['auth-form__label']} htmlFor="password">Contraseña:</label>
                    <div style={{ display: 'flex', position: 'relative' }}>
                        <input
                            type={showPassword ? 'text' : 'password'}
                            id="password"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            autoComplete={isLogin ? 'current-password' : 'new-password'}
                            required
                            style={{ width: '100%', paddingRight: '40px' }}
                        />
                        <button
                            type="button"
                            onClick={() => setShowPassword(!showPassword)}
                            aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
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
                                // Icono de ojo tachado (ocultar)
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                                    <line x1="1" y1="1" x2="23" y2="23"></line>
                                </svg>
                            ) : (
                                // Icono de ojo abierto (mostrar)
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                    <circle cx="12" cy="12" r="3"></circle>
                                </svg>
                            )}
                        </button>
                    </div>
                </div>

                {/* Terminos y condiciones: solo al registrarse */}
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

                {/* role="alert": los lectores de pantalla anuncian el error al aparecer */}
                {error && (
                    <p className={styles['auth-form__error']} role="alert">
                        {error}
                    </p>
                )}

                <button
                    type="submit"
                    className={styles['auth-form__submit']}
                    disabled={isLoading}
                >
                    {isLoading ? 'Procesando...' : (isLogin ? 'Iniciar sesión' : 'Registrarse')}
                </button>

                {/* Enlace para cambiar entre login y registro */}
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
