import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import styles from './Home.module.css';
import SectionsBlock from '@/components/sectionsBlock/SectionBlock';
import PriceShowcase from './PriceShowCase';
import StoreLogo from './StoreLogo';
import Icon from './Icon';
import { useSections } from '@/hooks/useSection';
import { useHomeShowcase } from '@/hooks/useHomeShowCase';

/*
 * Página de inicio (RF50, HT-15).
 *
 * REGLA DE ESTA PÁGINA: toda cifra, tienda, producto o precio que se muestra sale del backend.
 * Si un dato falla o no existe, su bloque se oculta; nunca se rellena con un valor inventado.
 * Los textos fijos solo describen funciones que la plataforma tiene HOY (por eso no aparece la
 * IA, que es de la Fase 4, ni testimonios: no hay opiniones reales que mostrar en la portada).
 */

const money = (value) => `$${Number(value).toLocaleString('es-CO')}`;
const numberFormat = new Intl.NumberFormat('es-CO');
const longDate = new Intl.DateTimeFormat('es-CO', { dateStyle: 'long' });
// Intl.ListFormat arma "D1, Éxito, Jumbo y Olímpica" con la gramática del idioma
const listFormat = new Intl.ListFormat('es', { style: 'long', type: 'conjunction' });

const TRUST = [
    { icon: 'search', text: 'Buscar y comparar no requiere cuenta' },
    { icon: 'calendar', text: 'Cada precio muestra la fecha en que la tienda lo confirmó' },
    { icon: 'link', text: 'Enlace directo a la página del producto en la tienda' },
];

// Solo funciones que existen hoy (Fases 1 y 2)
const FEATURES = [
    {
        icon: 'search',
        title: 'Un buscador para todas las tiendas',
        text: 'Busca por nombre, marca o categoría y filtra por tienda y rango de precio. Los resultados llegan ordenados por la mayor diferencia entre tiendas.',
        to: '/buscar',
        cta: 'Buscar productos',
    },
    {
        icon: 'bell',
        title: 'Avisos cuando baja el precio',
        text: 'Sigue un producto, fija un precio objetivo o pide aviso ante cualquier baja, y te lo mostramos en la plataforma.',
        to: '/seguidos',
        cta: 'Ver mis seguidos',
    },
    {
        icon: 'tag',
        title: 'Precio con fecha y enlace a la tienda',
        text: 'Cada precio indica cuándo lo confirmó la tienda y te lleva a su página, para que verifiques antes de comprar.',
        to: '/buscar',
        cta: 'Comparar precios',
    },
    {
        icon: 'star',
        title: 'Opiniones de otros compradores',
        text: 'Califica de 1 a 5 estrellas y comenta un producto en una tienda. Los promedios te ayudan a elegir dónde comprar.',
        to: '/buscar',
        cta: 'Explorar productos',
    },
];

const STEPS = [
    { title: 'Busca y compara', text: 'Escribe el producto que vas a comprar y mira su precio en cada tienda, ordenado de menor a mayor.' },
    { title: 'Sigue y recibe avisos', text: 'Sigue tus productos y fija un precio objetivo. Cuando baje, lo verás en tus avisos.' },
    { title: 'Compra donde más ahorras', text: 'Abre la página de la tienda más barata desde la comparación y confirma el precio antes de pagar.' },
];

// Respuestas verificadas contra las decisiones del proyecto (RN-04, alcance, DECISIONS.md)
const FAQ = [
    {
        q: '¿De dónde salen los precios?',
        a: 'Los carga automáticamente un proceso que consulta los catálogos públicos de las tiendas. Cada precio muestra la fecha en que la tienda lo confirmó por última vez.',
    },
    {
        q: '¿PreciosCo vende productos?',
        a: 'No. Comparamos precios y te enviamos a la página de la tienda. No hay compras, pagos ni domicilios en la plataforma.',
    },
    {
        q: '¿Tengo que registrarme?',
        a: 'Para buscar y comparar, no. Para seguir productos, recibir avisos, calificar y comentar necesitas una cuenta.',
    },
    {
        q: '¿Los precios son definitivos?',
        a: 'Son informativos y pueden cambiar: confirma siempre en la tienda. Verificamos el precio para Valledupar en las tiendas que lo permiten; D1 no vende en línea en Valledupar, así que usamos su precio nacional.',
    },
    {
        q: '¿Qué cobertura tiene hoy?',
        a: 'Hoy comparamos supermercados. Droguerías, cuidado facial y repuestos para vehículos y motos están en camino.',
    },
];

export default function Home() {
    const navigate = useNavigate();
    const [query, setQuery] = useState('');

    // Mismo bloque de estadísticas que usa SectionsBlock: useSections comparte una sola petición
    const { stats } = useSections();
    const { products, stores } = useHomeShowcase();

    const handleSearch = (e) => {
        e.preventDefault();
        const term = query.trim();
        navigate(term ? `/buscar?q=${encodeURIComponent(term)}` : '/buscar');
    };

    const storeNames = stores.map((s) => s.name);
    const storesText = storeNames.length > 0 ? listFormat.format(storeNames) : null;
    const hasStats = stats && stats.totalProducts > 0;

    return (
        <div className={styles.page}>
            {/* ===================== HERO ===================== */}
            <section className={styles.hero}>
                <div className={styles.heroText}>
                    <p className={styles.eyebrow}>Comparador de precios de supermercado</p>
                    <h1 className={styles.title}>
                        Ahorra en cada compra <span className={styles.accent}>antes de pagar</span>
                    </h1>
                    <p className={styles.lead}>
                        {storesText
                            ? `Compara los precios de ${storesText} y compra donde más ahorras.`
                            : 'Compara los precios entre tiendas y compra donde más ahorras.'}
                    </p>

                    <form className={styles.search} onSubmit={handleSearch} role="search">
                        <input
                            type="search"
                            placeholder="Ej. Arroz, leche, aceite..."
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                            className={styles.searchInput}
                            aria-label="Buscar productos"
                        />
                        <button type="submit" className={styles.searchButton}>Comparar</button>
                    </form>

                    {products.length > 0 && (
                        <p className={styles.quick}>
                            <span>Mayor diferencia de precio:</span>
                            {products.map((p) => (
                                <Link key={p.id} to={`/comparador?productId=${p.id}`} className={styles.chip}>
                                    {p.name}
                                </Link>
                            ))}
                        </p>
                    )}

                    <ul className={styles.trust}>
                        {TRUST.map((item) => (
                            <li key={item.text}><Icon name={item.icon} size={16} /> {item.text}</li>
                        ))}
                    </ul>
                </div>

                {/* Tarjeta flotante: los dos productos con más diferencia hoy. Sin datos, no se dibuja. */}
                {products.length > 0 && (
                    <aside className={styles.floating} aria-label="Mayor diferencia de precio hoy">
                        <p className={styles.floatingTitle}>Mayor diferencia hoy</p>
                        <ul className={styles.floatingList}>
                            {products.slice(0, 2).map((p) => {
                                const cheapest = [...p.offers].sort((a, b) => Number(a.value) - Number(b.value))[0];
                                return (
                                    <li key={p.id}>
                                        <Link to={`/comparador?productId=${p.id}`} className={styles.floatingItem}>
                                            <StoreLogo name={cheapest.storeName} />
                                            <span className={styles.floatingInfo}>
                                                <strong>{p.name}</strong>
                                                <span>{money(cheapest.value)} en {cheapest.storeName}</span>
                                            </span>
                                            <span className={styles.floatingSaving}>
                                                −{money(Number(p.maxPrice) - Number(p.minPrice))}
                                            </span>
                                        </Link>
                                    </li>
                                );
                            })}
                        </ul>
                    </aside>
                )}
            </section>

            {/* ===================== COMPARADOR REAL ===================== */}
            {products.length > 0 && (
                <section className={styles.section} aria-labelledby="diff-title">
                    <p className={styles.kicker}>Comparación real</p>
                    <h2 id="diff-title" className={styles.h2}>La diferencia se nota en tu bolsillo</h2>
                    <p className={styles.sub}>
                        Estos son productos reales del catálogo, con el precio de cada tienda y la fecha en que fue confirmado.
                    </p>
                    <PriceShowcase products={products} />
                </section>
            )}

            {/* ===================== CIFRAS REALES + TIENDAS ===================== */}
            {hasStats && (
                <section className={styles.sectionAlt} aria-labelledby="coverage-title">
                    <div className={styles.inner}>
                        <p className={styles.kicker}>Cobertura actual</p>
                        <h2 id="coverage-title" className={styles.h2}>La plataforma hoy, en números reales</h2>

                        <dl className={styles.stats}>
                            <div>
                                <dt>Productos con precio</dt>
                                <dd>{numberFormat.format(stats.totalProducts)}</dd>
                            </div>
                            <div>
                                <dt>{stats.totalStores === 1 ? 'Tienda activa' : 'Tiendas activas'}</dt>
                                <dd>{stats.totalStores}</dd>
                            </div>
                            {stats.lastUpdate && (
                                <div>
                                    <dt>Último precio confirmado</dt>
                                    <dd className={styles.statDate}>{longDate.format(new Date(stats.lastUpdate))}</dd>
                                </div>
                            )}
                        </dl>

                        {stores.length > 0 && (
                            <>
                                <ul className={styles.storeStrip} aria-label="Tiendas que comparamos">
                                    {stores.map((store) => (
                                        <li key={store.id}>
                                            <StoreLogo name={store.name} size="lg" decorative />
                                            <span>{store.name}</span>
                                        </li>
                                    ))}
                                </ul>
                                <p className={styles.legal}>
                                    Los logos son marcas registradas de sus titulares. PreciosCo no está afiliada a estas tiendas.
                                </p>
                            </>
                        )}
                    </div>
                </section>
            )}

            {/* ===================== SECCIONES (HT-15) ===================== */}
            <SectionsBlock />

            {/* ===================== BENEFICIOS ===================== */}
            <section className={styles.section} aria-labelledby="features-title">
                <p className={styles.kicker}>Lo que ya puedes hacer</p>
                <h2 id="features-title" className={styles.h2}>Todo para decidir dónde comprar</h2>
                <ul className={styles.features}>
                    {FEATURES.map((f) => (
                        <li key={f.title} className={styles.feature}>
                            <span className={styles.featureIcon}><Icon name={f.icon} size={22} /></span>
                            <h3>{f.title}</h3>
                            <p>{f.text}</p>
                            <Link to={f.to} className={styles.featureLink}>{f.cta} <Icon name="arrow" size={16} /></Link>
                        </li>
                    ))}
                </ul>
            </section>

            {/* ===================== CÓMO FUNCIONA ===================== */}
            <section id="como-funciona" className={styles.sectionAlt} aria-labelledby="how-title">
                <div className={styles.inner}>
                    <h2 id="how-title" className={styles.h2}>¿Cómo funciona PreciosCo?</h2>
                    <p className={styles.sub}>Encontrar la mejor opción de compra en tres pasos.</p>
                    <ol className={styles.steps}>
                        {STEPS.map((step, index) => (
                            <li key={step.title} className={styles.step}>
                                <span className={styles.stepNumber}>{String(index + 1).padStart(2, '0')}</span>
                                <h3>{step.title}</h3>
                                <p>{step.text}</p>
                            </li>
                        ))}
                    </ol>
                </div>
            </section>

            {/* ===================== QUIÉNES SOMOS ===================== */}
            <section id="sobre-nosotros" className={styles.about} aria-labelledby="about-title">
                <img className={styles.aboutImage} src="/assets/images/LogoPreciosCo.png" alt="Logo PreciosCo" width="220" />
                <div>
                    <h2 id="about-title" className={styles.h2Left}>Quiénes somos</h2>
                    <p>
                        Somos PreciosCo, nacimos con una misión clara: ayudarte a tomar mejores decisiones de compra,
                        cuidando tu bolsillo y ahorrándote tiempo.
                    </p>
                    <p>
                        Evolucionamos desde una herramienta de consulta ágil en Telegram hacia una plataforma web
                        diseñada para centralizar, comparar y transparentar la información de precios de las tiendas
                        que comparamos.
                    </p>
                </div>
            </section>

            {/* ===================== PREGUNTAS FRECUENTES ===================== */}
            <section className={styles.sectionAlt} aria-labelledby="faq-title">
                <div className={styles.innerNarrow}>
                    <h2 id="faq-title" className={styles.h2}>Lo que necesitas saber</h2>
                    {/* <details> es un acordeón nativo: abre con teclado y lector de pantalla sin JavaScript */}
                    {FAQ.map((item) => (
                        <details key={item.q} className={styles.faq}>
                            <summary>{item.q}</summary>
                            <p>{item.a}</p>
                        </details>
                    ))}
                </div>
            </section>

            {/* ===================== LLAMADA A LA ACCIÓN ===================== */}
            <section className={styles.cta} aria-labelledby="cta-title">
                <h2 id="cta-title">Empieza a comparar hoy mismo</h2>
                <p>
                    {stats?.totalStores > 0
                        ? `Busca un producto y compara su precio en ${stats.totalStores} tiendas.`
                        : 'Busca un producto y compara su precio entre tiendas.'}
                </p>
                <div className={styles.ctaButtons}>
                    <Link to="/buscar" className={styles.ctaPrimary}>Comparar ahora</Link>
                    <a href="#como-funciona" className={styles.ctaSecondary}>Ver cómo funciona</a>
                </div>
            </section>
        </div>
    );
}