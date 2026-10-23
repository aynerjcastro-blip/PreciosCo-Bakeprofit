import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { loginApi } from '../api/auth';
import { useAuth } from '../context/useAuth';

export default function Login() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const { login } = useAuth(); // Hook de autenticación
    const navigate = useNavigate();
    const location = useLocation();

    // Determina la ruta a la que debe volver el usuario después del login (por defecto '/')
    const from = location.state?.from?.pathname || '/';

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            // Petición al backend
            const data = await loginApi({ email, password });

            // Sincroniza la respuesta con AuthContext y localStorage
            login(data);

            // Redirige a la página previa o al inicio
            navigate(from, { replace: true });
        } catch (err) {
            setError(err.message || 'Credenciales inválidas. Intente nuevamente.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-container" style={{ maxWidth: '400px', margin: '3rem auto', padding: '1.5rem' }}>
            <h2>Iniciar Sesión</h2>

            {error && (
                <div className="error-message" style={{ color: 'red', marginBottom: '1rem' }}>
                    {error}
                </div>
            )}

            <form onSubmit={handleSubmit}>
                <div className="form-group" style={{ marginBottom: '1rem' }}>
                    <label htmlFor="email">Correo Electrónico</label>
                    <input
                        id="email"
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                        style={{ width: '100%', padding: '0.5rem', marginTop: '0.25rem' }}
                    />
                </div>

                <div className="form-group" style={{ marginBottom: '1.5rem' }}>
                    <label htmlFor="password">Contraseña</label>
                    <input
                        id="password"
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                        style={{ width: '100%', padding: '0.5rem', marginTop: '0.25rem' }}
                    />
                </div>

                <button
                    type="submit"
                    disabled={loading}
                    style={{ width: '100%', padding: '0.75rem', cursor: loading ? 'not-allowed' : 'pointer' }}
                >
                    {loading ? 'Iniciando sesión...' : 'Ingresar'}
                </button>
            </form>

            <p style={{ marginTop: '1rem', textAlign: 'center' }}>
                ¿No tienes cuenta? <Link to="/register">Regístrate aquí</Link>
            </p>
        </div>
    );
}