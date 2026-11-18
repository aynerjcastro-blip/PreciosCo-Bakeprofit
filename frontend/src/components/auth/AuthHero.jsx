import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { findActiveStores } from '@/api/stores';
import styles from '../../pages/AuthPage.module.css';

/*
 * Panel informativo (izquierda). Solo promete funciones que existen hoy; las tiendas que nombra
 * salen de las tiendas activas de la base de datos, no de una lista escrita a mano.
 */

// Intl.ListFormat arma "D1, Éxito, Jumbo y Olímpica" con la gramática del idioma
const listFormat = new Intl.ListFormat('es', { style: 'long', type: 'conjunction' });

function Svg({ children }) {
    return (
        <svg width={20} height={20} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}
            strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            {children}
        </svg>
    );
}

const ICONS = {
    store: (
        <Svg>
            <path d="m2 7 4.41-4.41A2 2 0 0 1 7.83 2h8.34a2 2 0 0 1 1.42.59L22 7" />
            <path d="M4 7v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7" />
            <path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8" />
            <path d="M2 7h20" />
        </Svg>
    ),
    bell: (
        <Svg>
            <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
            <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
        </Svg>
    ),
    calendar: (
        <Svg>
            <rect x="3" y="4" width="18" height="18" rx="2" />
            <path d="M16 2v4M8 2v4M3 10h18" />
            <path d="m9 16 2 2 4-4" />
        </Svg>
    ),
    star: (
        <Svg>
            <path d="m12 2 3.1 6.3 6.9 1-5 4.9 1.2 6.8L12 17.8 5.8 21l1.2-6.8-5-4.9 6.9-1z" />
        </Svg>
    ),
};

/** Nombres de las tiendas activas; si la petición falla se usa un texto general. */
function useStoreNames() {
    const [names, setNames] = useState([]);

    useEffect(() => {
        let cancelled = false; // evita actualizar el estado si la página ya se cerró
        findActiveStores()
            .then((stores) => {
                if (!cancelled) setNames(stores.map((store) => store.name));
            })
            .catch(() => { /* sin nombres el panel sigue funcionando con el texto general */ });
        return () => {
            cancelled = true;
        };
    }, []);

    return names;
}

/**
 * @param {{mode: 'login'|'register'}} props
 *
 * Los textos de los DOS modos están siempre en el DOM, apilados en la misma celda de una
 * cuadrícula; solo el activo se ve (opacidad y desplazamiento). Así el cambio se anima con
 * CSS y la tarjeta no cambia de altura. El inactivo queda con `visibility: hidden`: no se ve,
 * no recibe foco y los lectores de pantalla lo ignoran.
 */
export default function AuthHero({ mode }) {
    const storeNames = useStoreNames();
    const stores = storeNames.length > 0
        ? `${listFormat.format(storeNames)} en una sola búsqueda.`
        : 'Todas las tiendas en una sola búsqueda.';

    const copies = [
        {
            mode: 'login',
            title: 'Vuelve a encontrar los mejores precios',
            subtitle: 'Entra para seguir tus productos y revisar tus avisos.',
            features: [
                { icon: 'store', title: 'Varias tiendas a la vez', text: stores },
                { icon: 'bell', title: 'Tus avisos de baja de precio', text: 'Mira qué cambió desde tu última visita.' },
                { icon: 'star', title: 'Tus productos seguidos', text: 'Gestiona tus productos y tu precio objetivo en un solo lugar.' },
            ],
        },
        {
            mode: 'register',
            title: 'Compara mejor. Ahorra más.',
            subtitle: 'Crea tu cuenta para seguir productos y recibir avisos cuando bajen de precio.',
            features: [
                { icon: 'store', title: 'Varias tiendas a la vez', text: stores },
                { icon: 'calendar', title: 'El precio, con su fecha', text: 'Cada precio muestra cuándo lo confirmó la tienda y te lleva a su página.' },
                { icon: 'bell', title: 'Avisos cuando baja el precio', text: 'Sigue un producto y fija un precio objetivo.' },
            ],
        },
    ];

    return (
        <aside className={styles['auth-hero']}>
            <Link to="/" className={styles['auth-brand']} aria-label="PreciosCo, ir al inicio">
                <span className={styles['auth-brand__text']}>Precios</span>
                <img src="/assets/images/co-badge.png" alt="" className={styles['auth-brand__co']} />
            </Link>

            <div className={styles['hero-stack']}>
                {copies.map((copy) => (
                    <div key={copy.mode} className={styles['hero-copy']} data-active={mode === copy.mode}>
                        <p className={styles['hero-title']}>{copy.title}</p>
                        <p className={styles['hero-subtitle']}>{copy.subtitle}</p>
                        <ul className={styles['hero-features']}>
                            {copy.features.map((feature) => (
                                <li key={feature.title}>
                                    <span className={styles['hero-icon']}>{ICONS[feature.icon]}</span>
                                    <div>
                                        <strong>{feature.title}</strong>
                                        <p>{feature.text}</p>
                                    </div>
                                </li>
                            ))}
                        </ul>
                    </div>
                ))}
            </div>
        </aside>
    );
}