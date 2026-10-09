import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import styles from './Forms.module.css';
import { login, register } from '../api/auth';
import { saveSession } from '../utils/auth-helper';

// Componente centralizado para Login y Registro
// Recibe la propiedad 'type' que determina que vista renderizar
export default function Forms({ type = 'login' }) {
    const isLogin = type === 'login';
    const navigate = useNavigate();

    // Estado unificado para todos los campos posibles
    const [formData, setFormData] = useState({
        name: '',
        email: '',
        phone: '',
        password: '',
        terms: false
    });

    // Estados para el manejo de la interfaz
    const [error, setError] = useState(null);
    const [isLoading, setIsLoading] = useState(false);

    // Manejador generico para actualizar el estado de los inputs
    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData((prev) => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    // Validacion basica en el lado del cliente
    const validateClientSide = () => {
        if (!isLogin && !formData.terms) {
            return "Debes aceptar los terminos y condiciones";
        }
        return null;
    };

    // Manejador del envio del formulario
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

            // Guardamos la sesion utilizando la utilidad existente
            saveSession(authResponse.token, {
                name: authResponse.name,
                email: authResponse.email,
                role: authResponse.role,
            });

            // Redireccion al inicio sin recargar la pagina
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

                {/* Renderizado condicional para campos exclusivos del Registro */}
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
                        <label className={styles['auth-form__label']} htmlFor="register-phone">Numero:</label>
                        <input
                            type="number"
                            id="register-phone"
                            name="phone"
                            value={formData.phone}
                            onChange={handleChange}
                        />
                    </div>
                )}

                <div className={styles['auth-form__field']}>
                    <label className={styles['auth-form__label']} htmlFor="password">Contrasena:</label>
                    <input
                        type="password"
                        id="password"
                        name="password"
                        value={formData.password}
                        onChange={handleChange}
                        required
                    />
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
                        <span>Aceptar los terminos y condiciones</span>
                    </div>
                )}

                {/* Muestra errores capturados durante el proceso */}
                {error && <p className={styles['auth-form__error']}>{error}</p>}

                <button
                    type="submit"
                    className={styles['auth-form__submit']}
                    disabled={isLoading}
                >
                    {isLoading
                        ? 'Procesando...'
                        : (isLogin ? 'Iniciar sesion' : 'Registrarse')}
                </button>

                {/* Enlaces de navegacion entre vistas */}
                {isLogin ? (
                    <p className={styles['auth-form__switch']}>
                        No tienes cuenta? <Link to="/register">Registrate</Link>
                    </p>
                ) : (
                    <p className={styles['auth-form__switch']}>
                        Ya tienes cuenta? <Link to="/login">Inicia sesion</Link>
                    </p>
                )}
            </form>
        </div>
    );
}