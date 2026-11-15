import { useState } from 'react';
import styles from './StoreLogo.module.css';

/*
 * Logo de una tienda. Los archivos viven en public/assets/stores/ y se descargan del kit de
 * prensa OFICIAL de cada marca. Son marcas registradas de sus titulares: solo se muestran las
 * de tiendas que están en la base de datos y se acompañan del aviso de no afiliación.
 *
 * La tienda se reconoce por un fragmento de su nombre ya normalizado (sin tildes, en minúsculas),
 * para que "Éxito", "EXITO" y "Almacenes Éxito" lleguen al mismo archivo.
 * Si no hay logo registrado o el archivo no carga, se muestran las iniciales: nunca un ícono roto.
 */
const LOGOS = [
    { match: 'd1', src: '/assets/stores/d1.png' },
    { match: 'exito', src: '/assets/stores/exito.png' },
    { match: 'jumbo', src: '/assets/stores/jumbo.png' },
    { match: 'olimpica', src: '/assets/stores/olimpica.png' },
    { match:'larebaja' , src: '/assets/stores/larebaja.png'}
];

function normalize(text) {
    return text.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
}

function findLogo(name) {
    const key = normalize(name);
    return LOGOS.find((logo) => key.includes(logo.match))?.src ?? null;
}

function initials(name) {
    return name.trim().slice(0, 2).toUpperCase();
}

/**
 * @param {{name: string, size?: 'md'|'lg', decorative?: boolean}} props
 * `decorative`: true cuando el nombre de la tienda ya está escrito al lado (el lector de pantalla
 * no debe repetirlo); false cuando el logo va solo y su `alt` es lo único que lo identifica.
 */
export default function StoreLogo({ name, size = 'md', decorative = true }) {
    const src = findLogo(name);
    // Se recuerda en qué logo falló la carga: así un cambio de tienda reintenta con el nuevo archivo
    const [failedSrc, setFailedSrc] = useState(null);
    const showImage = src && failedSrc !== src;

    return (
        <span className={`${styles.logo} ${styles[size]}`} {...(decorative ? { 'aria-hidden': 'true' } : {})}>
            {showImage ? (
                <img src={src} alt={decorative ? '' : name} loading="lazy" onError={() => setFailedSrc(src)} />
            ) : (
                <span className={styles.initials} {...(decorative ? {} : { role: 'img', 'aria-label': name })}>
                    {initials(name)}
                </span>
            )}
        </span>
    );
}