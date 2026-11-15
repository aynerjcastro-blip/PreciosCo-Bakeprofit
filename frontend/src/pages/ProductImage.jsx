import { useState } from 'react';
import styles from './ProductImage.module.css';

/**
 * Foto del producto tal como la publica la tienda. Se ENLAZA, no se copia ni se guarda
 * (DECISIONS.md: los términos de las tiendas protegen sus imágenes). Si no hay URL o la imagen
 * no carga, se muestra un marcador: nunca un ícono de imagen rota.
 * `referrerPolicy="no-referrer"` evita que la tienda bloquee la imagen por venir de otro sitio.
 */
export default function ProductImage({ src, alt = '', className = '' }) {
    // Se recuerda cuál URL falló: si el producto cambia, la nueva imagen se vuelve a intentar
    const [failedSrc, setFailedSrc] = useState(null);
    const showImage = src && failedSrc !== src;

    return (
        <span className={`${styles.frame} ${className}`}>
            {showImage ? (
                <img src={src} alt={alt} loading="lazy" referrerPolicy="no-referrer" onError={() => setFailedSrc(src)} />
            ) : (
                <span className={styles.placeholder} aria-hidden="true">🛒</span>
            )}
        </span>
    );
}