import { useState, useContext } from 'react';
import { AuthContext } from '@/context/AuthContext';
import styles from '@/components/profile/Profile.module.css';

/**
 * Renderiza la interfaz para que el usuario logueado modifique su información personal.
 * Intercepta la respuesta de la API y sincroniza el estado global al instante para 
 * cumplir con la regla de negocio: actualizar el header sin necesidad de recargar o re-loguearse.
 */
export default function Profile() {
    // Extraemos el usuario actual para llenar el input, y la función para inyectar los cambios al Contexto.
    const { user, updateUserData } = useContext(AuthContext);

    // Estado para controlar lo que el usuario escribe. Se inicializa con el nombre actual (si existe).
    const [nombre, setNombre] = useState(user?.name || '');

    // Estado para bloquear la UI mientras la petición HTTP está en vuelo, evitando doble submit.
    const [isLoading, setIsLoading] = useState(false);

    // Estado para manejar el feedback visual (mensajes de éxito o error devueltos por el bloque try-catch).
    const [feedback, setFeedback] = useState({ type: '', message: '' });

    /**
     * Maneja el envío del formulario.
     * Previene el recargo por defecto, ejecuta la petición PUT y gestiona los estados de carga y error.
     * 
     * @param {Event} e - El evento del formulario (submit).
     */
    const handleSubmit = async (e) => {
        // Evitamos que el navegador recargue la página al hacer click en el botón tipo submit.
        e.preventDefault();

        // Iniciamos el estado de carga y limpiamos cualquier mensaje de error previo.
        setIsLoading(true);
        setFeedback({ type: '', message: '' });

        try {
            // Petición al backend protegido. Requerimos el token JWT guardado en localStorage.
            const response = await fetch('http://localhost:8080/api/users/me', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('token')}`
                },
                // Enviamos el nombre como JSON, tal cual lo espera el DTO de Spring Boot.
                body: JSON.stringify({ name: nombre, })
            });

            if (response.ok) {
                // Si el servidor responde HTTP 200, la base de datos se actualizó correctamente.
                // Actualizamos el Contexto de React y el localStorage para que el Header cambie ahora mismo.
                updateUserData({ name: nombre });

                // Mostramos mensaje verde al usuario.
                setFeedback({ type: 'success', message: '¡Nombre actualizado correctamente!' });
            } else {
                // Si la respuesta es HTTP 401, 403 o 400, mostramos el error.
                setFeedback({ type: 'error', message: 'Hubo un error al actualizar el perfil.' });
            }
        } catch {
            // Este catch atrapa errores de red (ej. servidor caído o sin internet).
            setFeedback({ type: 'error', message: 'Error de conexión con el servidor.' });
        } finally {
            // Sin importar si la petición fue un éxito o un error, desactivamos el estado de carga
            // para que el usuario recupere el control del botón.
            setIsLoading(false);
        }
    };

    return (
        <div className={styles.profileContainer}>
            <h2 className={styles.title}>Mi Perfil</h2>

            <form onSubmit={handleSubmit} className={styles.formContainer}>
                <div className={styles.inputGroup}>
                    <label htmlFor="nombre">Nombre a mostrar</label>
                    <input
                        id="nombre"
                        type="text"
                        value={nombre}
                        onChange={(e) => setNombre(e.target.value)}
                        placeholder="Tu nuevo nombre"
                        disabled={isLoading} // Bloquea el input si se está guardando para evitar incoherencias
                        className={styles.inputField}
                    />
                </div>

                <button
                    type="submit"
                    className={styles.submitButton}
                    /* 
                     * Lógica de accesibilidad y optimización:
                     * 1. isLoading: Bloqueado mientras guarda.
                     * 2. nombre.trim() === '': Evita peticiones con cadenas vacías o puros espacios.
                     * 3. nombre === user?.name: Ahorra peticiones si el usuario no modificó la información.
                     */
                    disabled={isLoading || nombre.trim() === '' || nombre === user?.name}
                >
                    {isLoading ? 'Guardando...' : 'Guardar Cambios'}
                </button>

                {/* Renderizado condicional del bloque de feedback visual */}
                {feedback.message && (
                    <div className={`${styles.feedbackMsg} ${styles[feedback.type]}`}>
                        {feedback.message}
                    </div>
                )}
            </form>
        </div>
    );
}