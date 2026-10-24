import { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import styles from './Forms.module.css';
import { login as loginRequest, register } from '../api/auth';
import { useAuth } from '../context/useAuth';

const API_BASE = 'http://localhost:8080/api';

function LiveSavingsCard({ searchTerm = 'Audifonos' }) {
    const [deal, setDeal] = useState(null);

    useEffect(() => {
        let isMounted = true;
        async function fetchRealDeal() {
            try {
                const resSearch = await fetch(`${API_BASE}/products/search?name=${searchTerm}`);
                const products = await resSearch.json();
                if (!products.length || !isMounted) return;

                const product = products[0];
                const resPrices = await fetch(`${API_BASE}/prices/compare?productId=${product.id}`);
                const prices = await resPrices.json();

                if (prices.length >= 2 && isMounted) {
                    const best = prices[0];
                    const worst = prices[prices.length - 1];
                    const savings = worst.value - best.value;

                    setDeal({
                        id: product.id,
                        name: product.name,
                        image: best.imageUrl || worst.imageUrl,
                        bestPrice: best.value,
                        bestStore: best.storeName,
                        worstPrice: worst.value,
                        worstStore: worst.storeName,
                        savings
                    });
                }
            } catch (error) {
                console.error('Error cargando ahorro en vivo:', error);
            }
        }
        fetchRealDeal();
        return () => { isMounted = false; };
    }, [searchTerm]);

    if (!deal || deal.savings <= 0) return null;

    return (
        <Link to={`/comparador?productId=${deal.id}`} className={styles['savings-card']}>
            <div className={styles['savings-card__badge']}>
                Ahorras
                <br />${deal.savings.toLocaleString('es-CO')}
            </div>
            <div className={styles['savings-card__product']}>
                {deal.image ? (
                    <img src={deal.image} alt={deal.name} className={styles['savings-card__img']} />
                ) : (
                    <div className={styles['savings-card__img-placeholder']} />
                )}
                <h4 className={styles['savings-card__name']}>{deal.name}</h4>
            </div>
            <div className={styles['savings-card__comparison']}>
                <div className={styles['savings-card__price-box']}>
                    <span className={styles['savings-card__price--best']}>
                        ${deal.bestPrice.toLocaleString('es-CO')}
                    </span>
                    <span className={styles['savings-card__store']}>en {deal.bestStore}</span>
                </div>
                <div className={styles['savings-card__line']}>
                    <svg width={40} height={12} viewBox="0 0 40 12" fill="none" aria-hidden="true">
                        <path d="M0 6H40M40 6L35 1M40 6L35 11" stroke="#FF9F1C" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                </div>
                <div className={styles['savings-card__price-box']}>
                    <span className={styles['savings-card__price--worst']}>
                        ${deal.worstPrice.toLocaleString('es-CO')}
                    </span>
                    <span className={styles['savings-card__store']}>en {deal.worstStore}</span>
                </div>
            </div>
        </Link>
    );
}

export default function Forms({ type = 'login' }) {
    const isLogin = type === 'login';
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
        <div className={styles['auth-page']}>
            {/* COLUMNA IZQUIERDA: Ahora siempre visible, pero condicional en textos */}
            <div className={styles['auth-hero']}>
                <h1 className={styles['auth-hero__title']}>
                    {isLogin ? (
                        <>
                            Vuelve a encontrar <br />
                            <span className={styles['auth-hero__highlight']}>los mejores precios</span>
                        </>
                    ) : (
                        <>
                            Compara mejor.<br />
                            <span className={styles['auth-hero__highlight']}>Ahorra mas.</span>
                        </>
                    )}
                </h1>

                <p className={styles['auth-hero__subtitle']}>
                    {isLogin
                        ? 'Inicia sesion para continuar comparando y ahorrar.'
                        : 'Encuentra las mejores ofertas y recibe alertas cuando bajen de precio.'}
                </p>

                <ul className={styles['auth-hero__features']}>
                    {isLogin ? (
                        /* Beneficios Login */
                        <>
                            <li>
                                <div className={styles['feature-icon']}>
                                    <svg width={22} height={22} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                                        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                                    </svg>
                                </div>
                                <div>
                                    <strong>Tus productos favoritos</strong>
                                    <p>Ten siempre a mano lo que mas te interesa.</p>
                                </div>
                            </li>
                            <li>
                                <div className={styles['feature-icon']}>
                                    <svg width={22} height={22} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                                        <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
                                        <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
                                    </svg>
                                </div>
                                <div>
                                    <strong>Tus alertas de precio</strong>
                                    <p>Recibe notificaciones cuando bajen de precio.</p>
                                </div>
                            </li>
                            <li>
                                <div className={styles['feature-icon']}>
                                    <svg width={22} height={22} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                                        <line x1="18" y1="20" x2="18" y2="10" />
                                        <line x1="12" y1="20" x2="12" y2="4" />
                                        <line x1="6" y1="20" x2="6" y2="14" />
                                    </svg>
                                </div>
                                <div>
                                    <strong>Tus comparaciones guardadas</strong>
                                    <p>Accede a tus historiales y continua donde quedaste.</p>
                                </div>
                            </li>
                        </>
                    ) : (
                        /* Beneficios Registro (Mantenemos los que ya estaban) */
                        <>
                            <li>
                                <div className={styles['feature-icon']}>
                                    <svg width={22} height={22} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                                        <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z" />
                                    </svg>
                                </div>
                                <div>
                                    <strong>Guarda tus productos favoritos</strong>
                                    <p>Ten todo lo que te interesa en un solo lugar.</p>
                                </div>
                            </li>
                            <li>
                                <div className={styles['feature-icon']}>
                                    <svg width={22} height={22} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                                        <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
                                        <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
                                    </svg>
                                </div>
                                <div>
                                    <strong>Recibe alertas de precio</strong>
                                    <p>Te avisamos cuando bajen de precio.</p>
                                </div>
                            </li>
                            <li>
                                <div className={styles['feature-icon']}>
                                    <svg width={22} height={22} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                                        <path d="m2 7 4.41-4.41A2 2 0 0 1 7.83 2h8.34a2 2 0 0 1 1.42.59L22 7" />
                                        <path d="M4 7v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7" />
                                        <path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8" />
                                        <path d="M2 7h20" />
                                    </svg>
                                </div>
                                <div>
                                    <strong>Compara tiendas en un solo lugar</strong>
                                    <p>Ahorra tiempo y encuentra siempre la mejor opcion.</p>
                                </div>
                            </li>
                        </>
                    )}
                </ul>
                <LiveSavingsCard searchTerm="Audifonos" />
            </div>

            {/* COLUMNA DERECHA: Formulario */}
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
                                    <svg width={18} height={18} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                                        <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
                                        <circle cx={12} cy={7} r={4} />
                                    </svg>
                                </span>
                                <input
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
                        <label className={styles['auth-form__label']} htmlFor="email">Correo electronico</label>
                        <div className={styles['input-with-icon']}>
                            <span className={styles['input-icon']}>
                                <svg width={18} height={18} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                                    <rect width={20} height={16} x={2} y={4} rx={2} />
                                    <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
                                </svg>
                            </span>
                            <input
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
                        <label className={styles['auth-form__label']} htmlFor="password">Contrasena</label>
                        <div className={styles['input-with-icon']}>
                            <span className={styles['input-icon']}>
                                <svg width={18} height={18} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                                    <rect width={18} height={11} x={3} y={11} rx={2} ry={2} />
                                    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                                </svg>
                            </span>
                            <input
                                type={showPassword ? 'text' : 'password'}
                                id="password"
                                name="password"
                                value={formData.password}
                                onChange={handleChange}
                                placeholder={isLogin ? 'Ingresa tu contrasena' : 'Crea una contrasena'}
                                autoComplete={isLogin ? 'current-password' : 'new-password'}
                                required
                            />
                            <button
                                type="button"
                                className={styles['password-toggle']}
                                onClick={() => setShowPassword(!showPassword)}
                                aria-label={showPassword ? 'Ocultar contrasena' : 'Mostrar contrasena'}
                            >
                                {showPassword ? (
                                    <svg width={18} height={18} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                                        <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" />
                                        <path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68" />
                                        <path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61" />
                                        <line x1={2} x2={22} y1={2} y2={22} />
                                    </svg>
                                ) : (
                                    <svg width={18} height={18} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                                        <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
                                        <circle cx={12} cy={12} r={3} />
                                    </svg>
                                )}
                            </button>
                        </div>

                        {/* Enlace recuperar contraseña (Solo en Login) */}
                        {isLogin ? (
                            <div className={styles['auth-form__forgot']}>
                                <Link to="/recuperar">¿Olvidaste tu contrasena?</Link>
                            </div>
                        ) : (
                            <span className={styles['auth-form__hint']}>Minimo 8 caracteres</span>
                        )}
                    </div>

                    {!isLogin && (
                        <div className={styles['auth-form__terms']}>
                            <input type="checkbox" id="terms" name="terms" checked={formData.terms} onChange={handleChange} required />
                            <label htmlFor="terms">
                                Acepto los <Link to="/terminos">Terminos y condiciones</Link> y la <Link to="/privacidad">Politica de privacidad</Link>
                            </label>
                        </div>
                    )}

                    {error && <p className={styles['auth-form__error']} role="alert">{error}</p>}

                    <button
                        type="submit"
                        className={styles['auth-form__submit']}
                        disabled={isLoading}
                    >
                        {isLoading ? 'Iniciando...' : isLogin ? 'Iniciar sesion' : 'Empezar a comparar'}
                    </button>

                    {/* Boton de Google (Solo en Login) */}
                    {isLogin && (
                        <>
                            <div className={styles['auth-divider']}>
                                <span>o continua con</span>
                            </div>
                            <button type="button" className={styles['auth-google-btn']} onClick={() => alert("Integracion de Google pendiente")}>
                                <svg width="20" height="20" viewBox="0 0 48 48" aria-hidden="true">
                                    <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z" />
                                    <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z" />
                                    <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z" />
                                    <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z" />
                                </svg>
                                Continuar con Google
                            </button>
                        </>
                    )}

                    <div className={styles['auth-form__switch']}>
                        {isLogin ? (
                            <>
                                <p>¿Aun no tienes una cuenta? <Link to="/register">Registrate gratis</Link></p>
                                <p className={styles['auth-form__trust']}>
                                    <svg width={16} height={16} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                                        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                                    </svg>
                                    Acceso seguro y gratuito
                                </p>
                            </>
                        ) : (
                            <>
                                <p>¿Ya tienes cuenta? <Link to="/login">Inicia sesion</Link></p>
                                <p className={styles['auth-form__trust']}>
                                    <svg width={16} height={16} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                                        <path d="M20 6 9 17l-5-5" />
                                    </svg>
                                    Registro gratuito - Sin tarjeta de credito
                                </p>
                            </>
                        )}
                    </div>
                </form>
            </div>
        </div>
    );
}