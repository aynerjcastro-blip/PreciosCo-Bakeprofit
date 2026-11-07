import AuthHero from '../components/auth/AuthHero';
import AuthForm from '../components/auth/AuthForm';
// Asegurate de renombrar Forms.module.css a AuthPage.module.css
import styles from './AuthPage.module.css';

/**
 * Orquestador de la vista de Autenticación.
 * ¿Por qué? Reemplaza al antiguo monolito Forms.jsx. Su única responsabilidad es 
 * determinar la vista (login o register) y proveer el Layout principal.
 * Cumple estrictamente con el patrón 'Container Component'.
 */
export default function AuthPage({ type = 'login' }) {
    const isLogin = type === 'login';

    return (
        <div className={styles['auth-page']}>
            <AuthHero isLogin={isLogin} />
            <AuthForm isLogin={isLogin} />
        </div>
    );
}
