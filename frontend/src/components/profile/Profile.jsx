import { useState } from 'react';
import { useAuth } from '@/context/useAuth';
import styles from '@/components/profile/Profile.module.css';

// Etiqueta legible del rol que guarda el backend
const ROLE_LABELS = {
    USER: 'Consumidor',
    ADMIN: 'Administrador',
    SUPPLIER: 'Proveedor',
};

/**
 * Pantalla "Mi Perfil" (ruta protegida): permite cambiar el nombre a mostrar.
 * Sincroniza el estado global apenas el servidor confirma, para que el Header cambie
 * sin recargar ni volver a iniciar sesión.
 */
export default function Profile() {
    // Usuario actual (para precargar el campo) y la función que inyecta los cambios al contexto
    const { user, updateUserData } = useAuth();

    // Lo que el usuario escribe; arranca con el nombre actual
    const [nombre, setNombre] = useState(user?.name || '');

    // Bloquea la UI mientras la petición está en vuelo, para evitar un doble envío
    const [isLoading, setIsLoading] = useState(false);

    // Mensaje visual de éxito o de error
    const [feedback, setFeedback] = useState({ type: '', message: '' });

    const initial = user?.name?.charAt(0).toUpperCase();
    const roleLabel = ROLE_LABELS[user?.role];

    /**
     * Envía el formulario: PUT al backend protegido con el JWT guardado en localStorage.
     * @param {Event} e - evento submit
     */
    const handleSubmit = async (e) => {
        e.preventDefault();

        setIsLoading(true);
        setFeedback({ type: '', message: '' });

        try {
            const response = await fetch('http://localhost:8080/api/users/me', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('token')}`
                },
                // Tal cual lo espera el DTO de Spring Boot
                body: JSON.stringify({ name: nombre, })
            });

            if (response.ok) {
                // La base de datos ya cambió: se actualiza el contexto y localStorage para que el Header cambie ahora mismo
                updateUserData({ name: nombre });
                setFeedback({ type: 'success', message: '¡Nombre actualizado correctamente!' });
            } else {
                // 400, 401 o 403
                setFeedback({ type: 'error', message: 'Hubo un error al actualizar el perfil.' });
            }
        } catch {
            // Error de red (servidor caído o sin internet)
            setFeedback({ type: 'error', message: 'Error de conexión con el servidor.' });
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className={styles.page}>
            <div className={styles.profileContainer}>
                <h1 className={styles.title}>Mi Perfil</h1>

                {/* Resumen de la cuenta: solo lectura */}
                <div className={styles.summary}>
                    <span className={styles.avatar} aria-hidden="true">{initial}</span>
                    <div className={styles.identity}>
                        <strong className={styles.name}>{user?.name}</strong>
                        {user?.email && <span className={styles.email}>{user.email}</span>}
                    </div>
                    {roleLabel && <span className={styles.role}>{roleLabel}</span>}
                </div>

                <form onSubmit={handleSubmit} className={styles.formContainer}>
                    <div className={styles.inputGroup}>
                        <label htmlFor="nombre">Nombre a mostrar</label>
                        <input
                            id="nombre"
                            type="text"
                            value={nombre}
                            onChange={(e) => setNombre(e.target.value)}
                            placeholder="Tu nuevo nombre"
                            autoComplete="name"
                            disabled={isLoading} // Bloquea el input mientras se guarda, para evitar incoherencias
                            className={styles.inputField}
                        />
                    </div>

                    <button
                        type="submit"
                        className={styles.submitButton}
                        /*
                         * Se deshabilita si:
                         * 1. isLoading: está guardando.
                         * 2. nombre.trim() === '': evita peticiones con cadenas vacías o solo espacios.
                         * 3. nombre === user?.name: evita peticiones si no cambió nada.
                         */
                        disabled={isLoading || nombre.trim() === '' || nombre === user?.name}
                    >
                        {isLoading ? 'Guardando...' : 'Guardar Cambios'}
                    </button>

                    {/* Éxito: región de estado; error: alerta (los lectores de pantalla lo anuncian sin mover el foco) */}
                    {feedback.message && (
                        <div
                            className={`${styles.feedbackMsg} ${styles[feedback.type]}`}
                            role={feedback.type === 'error' ? 'alert' : 'status'}
                        >
                            {feedback.message}
                        </div>
                    )}
                </form>
            </div>
        </div>
    );
}