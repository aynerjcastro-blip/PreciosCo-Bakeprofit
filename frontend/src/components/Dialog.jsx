import { useEffect, useId, useRef } from 'react';
import styles from './Dialog.module.css';

/**
 * Diálogo modal basado en <dialog>: el navegador se encarga del foco,
 * de bloquear lo que hay detrás y de cerrar con Esc.
 *
 * Se usa montándolo y desmontándolo: {abierto && <Dialog ...>}.
 *
 * @param {string} title
 * @param {string} [subtitle]
 * @param {function} onClose - se llama con Esc, con la X o al hacer clic fuera
 */
export default function Dialog({ title, subtitle, onClose, children }) {
    const dialogRef = useRef(null);
    const titleId = useId();

    useEffect(() => {
        const dialog = dialogRef.current;
        if (dialog && !dialog.open) dialog.showModal();
        // Al cerrar, el navegador devuelve el foco al botón que abrió el diálogo
        return () => {
            if (dialog && dialog.open) dialog.close();
        };
    }, []);

    // Esc: se evita el cierre nativo y se deja que el componente padre lo desmonte
    const handleCancel = (e) => {
        e.preventDefault();
        onClose();
    };

    // El clic sobre el fondo oscuro llega con el propio <dialog> como destino
    const handleBackdropClick = (e) => {
        if (e.target === dialogRef.current) onClose();
    };

    return (
        <dialog
            ref={dialogRef}
            className={styles.dialog}
            aria-labelledby={titleId}
            onCancel={handleCancel}
            onClick={handleBackdropClick}
        >
            <div className={styles.dialog__panel}>
                <header className={styles.dialog__header}>
                    <div>
                        <h2 id={titleId} className={styles.dialog__title}>{title}</h2>
                        {subtitle && <p className={styles.dialog__subtitle}>{subtitle}</p>}
                    </div>
                    <button type="button" className={styles.dialog__close} onClick={onClose} aria-label="Cerrar">
                        ×
                    </button>
                </header>
                {children}
            </div>
        </dialog>
    );
}
