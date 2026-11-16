import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { googleLogin } from '@/api/auth';
import { useAuth } from '../context/useAuth';
import AuthHero from '../components/auth/AuthHero';
import AuthForm from '../components/auth/AuthForm';
import styles from './AuthPage.module.css';

/**
 * Selector Iniciar sesión | Registrarse.
 * Son enlaces (no botones) porque cambian la URL: /login y /register siguen siendo direcciones
 * reales que se pueden compartir, y los botones Atrás y Adelante funcionan.
 * `state` conserva la ruta protegida de la que venía el usuario: sin él, al pasar de login a
 * registro se perdería a dónde había que devolverlo después de entrar.
 */
function ModeSwitch({ mode, state }) {
    return (
        <nav className={styles['mode-switch']} data-mode={mode} aria-label="Acceso">
            {/* La píldora blanca se desliza bajo la opción activa (solo CSS, según data-mode) */}
            <span className={styles['mode-switch__pill']} aria-hidden="true" />
            <Link
                to="/login"
                state={state}
                className={styles['mode-switch__link']}
                aria-current={mode === 'login' ? 'page' : undefined}
            >
                Iniciar sesión
            </Link>
            <Link
                to="/register"
                state={state}
                className={styles['mode-switch__link']}
                aria-current={mode === 'register' ? 'page' : undefined}
            >
                Registrarse
            </Link>
        </nav>
    );
}

/**
 * Pantalla de acceso: tarjeta dividida en un panel informativo oscuro (izquierda) y el panel del
 * formulario (derecha). Login y registro viven en la MISMA página y se deslizan entre sí.
 *
 * Para que la animación funcione, esta página no debe desmontarse al pasar de /login a
 * /register. Con una ruta "de diseño" (layout route) React Router la mantiene montada:
 *
 *   <Route element={<AuthPage />}>
 *       <Route path="/login" element={null} />
 *       <Route path="/register" element={null} />
 *   </Route>
 *
 * El modo sale de la URL (única fuente de verdad); la prop `type` es opcional y solo existe
 * para no romper rutas escritas con <AuthPage type="login" />.
 */
export default function AuthPage({ type }) {
    const location = useLocation();
    const navigate = useNavigate();
    const { login } = useAuth();

    const mode = type ?? (location.pathname.startsWith('/register') ? 'register' : 'login');
    // Ruta protegida desde la que llegó el usuario; se le devuelve ahí al entrar
    const from = location.state?.from?.pathname || '/';

    // El inicio con Google vive aquí y no en cada formulario: hay DOS botones de Google a la vez
    // (uno por formulario) y Google Identity Services guarda un único callback. Con un solo
    // manejador, da igual cuál de los dos botones se pulse.
    const [googleError, setGoogleError] = useState(null);
    const [googleBusy, setGoogleBusy] = useState(false);

    // Al cambiar de modo se borra el error de Google; se ajusta durante el render (no en un efecto)
    const [prevMode, setPrevMode] = useState(mode);
    if (mode !== prevMode) {
        setPrevMode(mode);
        setGoogleError(null);
    }

    // replace evita que el botón "atrás" regrese a /login con la sesión ya iniciada
    const finishLogin = (authResponse) => {
        login(authResponse);
        navigate(from, { replace: true });
    };

    const handleGoogleCredential = async (credential) => {
        setGoogleError(null);
        setGoogleBusy(true);
        try {
            // El token de Google se valida en el backend; el front nunca lo da por bueno.
            // Si el correo no estaba registrado, el backend crea la cuenta (RF56): sirve para ambos modos.
            finishLogin(await googleLogin(credential));
        } catch (err) {
            // El backend explica el motivo (p. ej. cuenta desactivada); sin mostrarlo, el usuario no se entera
            setGoogleError(err.message || 'Error al iniciar sesión con Google');
        } finally {
            setGoogleBusy(false);
        }
    };

    const shared = {
        switchState: location.state,
        onAuthenticated: finishLogin,
        onGoogleCredential: handleGoogleCredential,
        onGoogleError: setGoogleError,
        googleError,
        googleBusy,
    };

    return (
        <div className={styles['auth-shell']}>
            <div className={styles['auth-card']}>
                <AuthHero mode={mode} />

                <div className={styles['auth-panel']}>
                    <ModeSwitch mode={mode} state={location.state} />

                    {/* Dos formularios lado a lado en una pista; se mueve la pista, no se desmonta nada */}
                    <div className={styles['form-viewport']}>
                        <div className={styles['form-track']} data-mode={mode}>
                            <div className={styles['form-pane']} data-active={mode === 'login'}>
                                <AuthForm isLogin {...shared} />
                            </div>
                            <div className={styles['form-pane']} data-active={mode === 'register'}>
                                <AuthForm isLogin={false} {...shared} />
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}