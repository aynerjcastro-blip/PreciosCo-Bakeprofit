import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import styles from './Home.module.css';
import SectionsBlock from '@/components/sectionsBlock/SectionBlock';
import PriceShowcase from './PriceShowCase';
import ProductImage from './ProductImage';
import StoreLogo from './StoreLogo';
import Icon from './Icon';
import { useSections } from '@/hooks/useSection';
import { useHomeShowcase } from '@/hooks/useHomeShowCase';

/*
 * Página de inicio (RF50, HT-15).
 *
 * REGLA DE ESTA PÁGINA: toda cifra, tienda, producto, imagen o precio que se muestra sale del
 * backend. Si un dato falla o no existe, su bloque se oculta; nunca se rellena con un valor
 * inventado. Los textos fijos solo describen funciones que existen HOY (por eso no hay IA, que
 * es de la Fase 4, ni testimonios). Los textos que dependen de la cobertura (tiendas, secciones)
 * se arman con los datos, así no quedan desactualizados cuando entren nuevas tiendas.
 */

const money = (value) => `$${Number(value).toLocaleString('es-CO')}`;
const numberFormat = new Intl.NumberFormat('es-CO');
const longDate = new Intl.DateTimeFormat('es-CO', { dateStyle: 'long' });
// Intl.ListFormat arma "D1, Éxito, Jumbo y Olímpica" con la gramática del idioma
const listFormat = new Intl.ListFormat('es', { style: 'long', type: 'conjunction' });
const capitalize = (text) => text.charAt(0).toUpperCase() + text.slice(1);

const TRUST = [
    { icon: 'search', text: 'Buscar y comparar no requiere cuenta' },
    { icon: 'calendar', text: 'Cada precio muestra su fecha de confirmación' },
    { icon: 'link', text: 'Enlace directo a la tienda' },
];

// Solo funciones que existen hoy (Fases 1 y 2)
const FEATURES = [
    {
        icon: 'search', title: 'Un buscador para todas las tiendas', to: '/buscar', cta: 'Buscar productos',
        text: 'Busca por nombre, marca o categoría y filtra por tienda y rango de precio. Los resultados llegan ordenados por la mayor diferencia entre tiendas.'
    },
    {
        icon: 'bell', title: 'Avisos cuando baja el precio', to: '/seguidos', cta: 'Ver mis seguidos',
        text: 'Sigue un producto, fija un precio objetivo o pide aviso ante cualquier baja, y te lo mostramos en la plataforma.'
    },
    {
        icon: 'tag', title: 'Precio con fecha y enlace a la tienda', to: '/buscar', cta: 'Comparar precios',
        text: 'Cada precio indica cuándo lo confirmó la tienda y te lleva a su página, para que verifiques antes de comprar.'
    },
    {
        icon: 'star', title: 'Opiniones de otros compradores', to: '/buscar', cta: 'Explorar productos',
        text: 'Califica de 1 a 5 estrellas y comenta un producto en una tienda. Los promedios te ayudan a elegir dónde comprar.'
    },
];

const STEPS = [
    { title: 'Busca y compara', text: 'Escribe el producto que vas a comprar y mira su precio en cada tienda, ordenado de menor a mayor.' },
    { title: 'Sigue y recibe avisos', text: 'Sigue tus productos y fija un precio objetivo. Cuando baje, lo verás en tus avisos.' },
    { title: 'Compra donde más ahorras', text: 'Abre la página de la tienda más barata desde la comparación y confirma el precio antes de pagar.' },
];

/** Respuesta sobre la cobertura: se arma con las secciones reales, no se escribe a mano. */
function coverageAnswer(sections) {
    const live = sections.filter((s) => s.available).map((s) => s.title.toLowerCase());
    const soon = sections.filter((s) => !s.available).map((s) => s.title.toLowerCase());
    if (live.length === 0) return 'Estamos incorporando tiendas y categorías: revisa las secciones de esta página.';
    const now = `Hoy comparamos ${listFormat.format(live)}.`;
    return soon.length > 0 ? `${now} ${capitalize(listFormat.format(soon))} llegan próximamente.` : now;
}

/** Collage del hero: fotos reales de los productos con más diferencia y el precio más bajo y más alto. */
function HeroCollage({ products }) {
    const [top, second, third] = products;
    // El producto principal va al centro, grande; los demás, a los lados
    const photos = [second, top, third].filter(Boolean);
    const offers = [...top.offers].sort((a, b) => Number(a.value) - Number(b.value));
    const cheapest = offers[0];
    const priciest = offers[offers.length - 1];

    return (
        <aside className={styles.collage} aria-label="Ejemplo de diferencia de precio">
            <div className={styles.collagePhotos}>
                {photos.map((p) => (
                    <Link
                        key={p.id}
                        to={`/comparador?productId=${p.id}`}
                        aria-label={`Comparar ${p.name}`}
                        className={`${styles.photo} ${p === top ? styles.photoCenter : styles.photoSide}`}
                    >
                        <ProductImage src={p.imageUrl} alt={p.name} />
                    </Link>
                ))}
            </div>
            <p className={styles.collageCaption}>Ejemplo real: {top.name}</p>
            <div className={styles.collageTags}>
                <p className={`${styles.tag} ${styles.tagBest}`}>
                    <StoreLogo name={cheapest.storeName} />
                    <span><small>{cheapest.storeName}</small><strong>{money(cheapest.value)}</strong></span>
                </p>
                <p className={styles.tag}>
                    <StoreLogo name={priciest.storeName} />
                    <span><small>{priciest.storeName}</small><strong>{money(priciest.value)}</strong></span>
                </p>
            </div>
        </aside>
    );
}

export default function Home() {
    const navigate = useNavigate();
    const [query, setQuery] = useState('');

    // useSections comparte una sola petición con SectionsBlock (caché de módulo)
    const { stats, sections } = useSections();
    const { products, stores } = useHomeShowcase();

    const handleSearch = (e) => {
        e.preventDefault();
        const term = query.trim();
        navigate(term ? `/buscar?q=${encodeURIComponent(term)}` : '/buscar');
    };

    const storeNames = stores.map((s) => s.name);
    const hasStats = stats && stats.totalProducts > 0;

    const faq = [
        {
            q: '¿De dónde salen los precios?',
            a: 'Los carga automáticamente un proceso que consulta los catálogos públicos de las tiendas. Cada precio muestra la fecha en que la tienda lo confirmó por última vez.'
        },
        {
            q: '¿PreciosCo vende productos?',
            a: 'No. Comparamos precios y te enviamos a la página de la tienda. No hay compras, pagos ni domicilios en la plataforma.'
        },
        {
            q: '¿Tengo que registrarme?',
            a: 'Para buscar y comparar, no. Para seguir productos, recibir avisos, calificar y comentar necesitas una cuenta.'
        },
        {
            q: '¿Los precios son definitivos?',
            a: 'Son informativos y pueden cambiar: confirma siempre en la tienda. Verificamos el precio para Valledupar en las tiendas que lo permiten; D1 no vende en línea en Valledupar, así que usamos su precio nacional.'
        },
        { q: '¿Qué cobertura tiene hoy?', a: coverageAnswer(sections) },
    ];

    return (
        <div className={styles.page}>
            {/* ===================== HERO ===================== */}
            <section className={styles.hero}>
                <div className={styles.heroText}>
                    <h1 className={styles.title}>
                        Ahorra en cada compra antes de pagar
                    </h1>
                    <p className={styles.lead}>
                        {storeNames.length > 0
                            ? `Compara los precios de ${listFormat.format(storeNames)} y compra donde más ahorras.`
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
                        <button type="submit" className={styles.searchButton}>
                            Comparar <Icon name="arrow" size={16} />
                        </button>
                    </form>

                    <ul className={styles.trust}>
                        {TRUST.map((item) => (
                            <li key={item.text}><Icon name={item.icon} size={16} /> {item.text}</li>
                        ))}
                    </ul>
                </div>

                {products.length > 0 && <HeroCollage products={products} />}
            </section>

            {/* ===================== COMPARADOR REAL ===================== */}
            {products.length > 0 && (
                <section className={styles.section} aria-labelledby="diff-title">
                    <PriceShowcase products={products} />
                </section>
            )}

            {/* ===================== CIFRAS REALES + TIENDAS (sección oscura) ===================== */}
            {hasStats && (
                <section className={styles.sectionDark} aria-labelledby="coverage-title">
                    <div className={styles.inner}>
                        <h2 id="coverage-title" className={styles.darkTitle}>
                            {stats.totalStores} {stats.totalStores === 1 ? 'tienda' : 'tiendas'}, un buscador.
                        </h2>
                        <p className={styles.darkLead}>Los números de hoy, tomados directamente de la plataforma.</p>

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
                                            <StoreLogo name={store.name} size="lg" />
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
                <h2 id="features-title" className={styles.h2}>Todo para decidir dónde comprar.</h2>
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
                    <h2 id="how-title" className={styles.h2}>Así funciona.</h2>
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
                    <h2 id="about-title" className={styles.h2Left}>Quiénes somos.</h2>
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
            <section id="preguntas-frecuentes" className={styles.sectionAlt} aria-labelledby="faq-title">
                <div className={styles.innerNarrow}>
                    <h2 id="faq-title" className={styles.h2}>Preguntas, respondidas.</h2>
                    {/* <details> es un acordeón nativo: abre con teclado y lector de pantalla sin JavaScript */}
                    {faq.map((item) => (
                        <details key={item.q} className={styles.faq}>
                            <summary>{item.q}</summary>
                            <p>{item.a}</p>
                        </details>
                    ))}
                </div>
            </section>

            {/* ===================== LLAMADA A LA ACCIÓN ===================== */}
            <section className={styles.cta} aria-labelledby="cta-title">
                <h2 id="cta-title">Empieza a comparar hoy mismo.</h2>
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