import AuthHero from '../components/auth/AuthHero';
import AuthForm from '../components/auth/AuthForm';
import styles from './AuthPage.module.css';

/**
 * Orquestador de la vista de Autenticación (login y registro).
 * Su única responsabilidad es decidir el modo y dar el marco de la página: el fondo cálido
 * que ocupa todo el ancho (shell) y la rejilla de dos columnas (hero + formulario).
 */
export default function AuthPage({ type = 'login' }) {
    const isLogin = type === 'login';

    return (
        <div className={styles['auth-shell']}>
            <div className={styles['auth-page']}>
                <AuthHero isLogin={isLogin} />
                <AuthForm isLogin={isLogin} />
            </div>
        </div>
    );
}
