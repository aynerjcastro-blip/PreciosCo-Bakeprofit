import SavingsCard from '../savings/SavingsCard';
import styles from '../../pages/AuthPage.module.css';

/**
 * Columna izquierda estática de la pantalla de autenticación.
 * ¿Por qué? Extraer el "Hero" evita que todo el bloque de texto y SVGs se vuelva a renderizar
 * en cascada cada vez que el usuario escribe un carácter en el formulario de la derecha.
 */
export default function AuthHero({ isLogin }) {
    return (
        <div className={styles['auth-hero']}>
            <h1 className={styles['auth-hero__title']}>
                {isLogin ? (
                    <>Vuelve a encontrar <br /><span className={styles['auth-hero__highlight']}>los mejores precios</span></>
                ) : (
                    <>Compara mejor.<br /><span className={styles['auth-hero__highlight']}>Ahorra mas.</span></>
                )}
            </h1>

            <p className={styles['auth-hero__subtitle']}>
                {isLogin
                    ? 'Inicia sesion para continuar comparando y ahorrar.'
                    : 'Encuentra las mejores ofertas y recibe alertas cuando bajen de precio.'}
            </p>

            <ul className={styles['auth-hero__features']}>
                {isLogin ? (
                    <>
                        <li>
                            <div className={styles['feature-icon']}>
                                <svg width={22} height={22} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" /></svg>
                            </div>
                            <div>
                                <strong>Tus productos favoritos</strong>
                                <p>Ten siempre a mano lo que mas te interesa.</p>
                            </div>
                        </li>
                        <li>
                            <div className={styles['feature-icon']}>
                                <svg width={22} height={22} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" /><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" /></svg>
                            </div>
                            <div>
                                <strong>Tus alertas de precio</strong>
                                <p>Recibe notificaciones cuando bajen de precio.</p>
                            </div>
                        </li>
                        <li>
                            <div className={styles['feature-icon']}>
                                <svg width={22} height={22} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><line x1="18" y1="20" x2="18" y2="10" /><line x1="12" y1="20" x2="12" y2="4" /><line x1="6" y1="20" x2="6" y2="14" /></svg>
                            </div>
                            <div>
                                <strong>Tus comparaciones guardadas</strong>
                                <p>Accede a tus historiales y continua donde quedaste.</p>
                            </div>
                        </li>
                    </>
                ) : (
                    <>
                        <li>
                            <div className={styles['feature-icon']}>
                                <svg width={22} height={22} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z" /></svg>
                            </div>
                            <div>
                                <strong>Guarda tus productos favoritos</strong>
                                <p>Ten todo lo que te interesa en un solo lugar.</p>
                            </div>
                        </li>
                        <li>
                            <div className={styles['feature-icon']}>
                                <svg width={22} height={22} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" /><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" /></svg>
                            </div>
                            <div>
                                <strong>Recibe alertas de precio</strong>
                                <p>Te avisamos cuando bajen de precio.</p>
                            </div>
                        </li>
                        <li>
                            <div className={styles['feature-icon']}>
                                <svg width={22} height={22} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="m2 7 4.41-4.41A2 2 0 0 1 7.83 2h8.34a2 2 0 0 1 1.42.59L22 7" /><path d="M4 7v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7" /><path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8" /><path d="M2 7h20" /></svg>
                            </div>
                            <div>
                                <strong>Compara tiendas en un solo lugar</strong>
                                <p>Ahorra tiempo y encuentra siempre la mejor opcion.</p>
                            </div>
                        </li>
                    </>
                )}
            </ul>
            <SavingsCard searchTerm="Audifonos" />
        </div>
    );
}
