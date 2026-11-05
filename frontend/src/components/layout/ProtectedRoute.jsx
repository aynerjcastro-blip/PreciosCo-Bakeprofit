import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/useAuth';

/**
 * Encapsula rutas que requieren autenticación obligatoria.
 * Si el usuario no está autenticado, lo redirige a /login conservando la ubicación de origen.
 */
export default function ProtectedRoute() {
    const { isAuthenticated, loading } = useAuth();
    const location = useLocation();

    // Mientras se verifica la sesión inicial no se realiza ninguna redirección
    if (loading) {
        return <div className="loading-container">Verificando sesión...</div>;
    }

    // Si no hay sesión válida, redirige al login guardando la ruta intentada en state
    if (!isAuthenticated) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    // Renderiza las rutas hijas dentro del layout actual
    return <Outlet />;
}