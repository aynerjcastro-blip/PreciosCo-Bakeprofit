import { useState, useCallback, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import styles from './Explore.module.css';
import { useExplore } from '@/hooks/useExplore';
import ProductImage from './ProductImage';
import StoreLogo from './StoreLogo';
import {
    CartIcon,
    PillIcon,
    FaceIcon,
    GearIcon,
} from '@/components/ui/icons/Sectoricons.jsx';

/*
 * Página "Explorar" — Fase B.
 *
 * Vista panorámica de todos los productos y secciones de PreciosCo.
 * Cuatro bloques:
 *   1. Hero Carousel — una diapositiva por sección
 *   2. Buscador + secciones como pills
 *   3. "En las tiendas hoy" — productos agrupados por tienda con tabs
 *   4. "Donde más ahorras hoy" — cross-sección con tabs de filtro
 *
 * Toda cifra sale del backend: si falla, el bloque se oculta.
 */

const money = (v) => `$${Number(v).toLocaleString('es-CO')}`;

const SECTION_ICONS = {
    supermercados: CartIcon,
    droguerias: PillIcon,
    'cuidado-facial': FaceIcon,
    repuestos: GearIcon,
};

/* ───── Datos de las diapositivas del Hero (uno por sección) ───── */
const HERO_SLIDES = [
    {
        slug: 'supermercados',
        badge: 'Mercado y hogar',
        titleLine1: 'Tu mercado,',
        titleAccent: 'al mejor precio',
        description: 'Arroz, aceite, leche, aseo — el mismo producto en D1, Éxito, Olímpica y Jumbo. Compara al instante.',
        cta: 'Comparar supermercados',
    },
    {
        slug: 'droguerias',
        badge: 'Salud y bienestar',
        titleLine1: 'Salud',
        titleAccent: 'sin pagar de más',
        description: 'Medicamentos y cuidado personal entre Farmatodo, La Rebaja y más. El mismo producto, distinto precio.',
        cta: 'Comparar droguerías',
    },
    {
        slug: 'cuidado-facial',
        badge: 'Cuidado personal',
        titleLine1: 'Tu piel,',
        titleAccent: 'mejor inversión',
        description: 'Cremas, sueros y protectores solares. Compara precios entre tiendas especializadas.',
        cta: 'Comparar cuidado facial',
    },
    {
        slug: 'repuestos',
        badge: 'Automotriz',
        titleLine1: 'Repuestos',
        titleAccent: 'al precio justo',
        description: 'Busca por referencia del fabricante y compara entre distribuidores autorizados.',
        cta: 'Comparar repuestos',
    },
];

/** Intervalo de auto-rotación del carrusel (ms) */
const CAROUSEL_INTERVAL = 6000;

/* ═══════════════════════ HERO CAROUSEL ═══════════════════════ */
function HeroCarousel({ sections, savingsBySection }) {
    const [current, setCurrent] = useState(0);
    const timerRef = useRef(null);

    // Solo mostrar slides de secciones que realmente existen y están activas
    const slides = HERO_SLIDES.filter((slide) =>
        sections.some((s) => s.slug === slide.slug),
    );

    const count = slides.length;

    const resetTimer = useCallback(() => {
        if (timerRef.current) clearInterval(timerRef.current);
        if (count <= 1) return;
        timerRef.current = setInterval(() => {
            setCurrent((prev) => (prev + 1) % count);
        }, CAROUSEL_INTERVAL);
    }, [count]);

    useEffect(() => {
        if (count <= 1) return;
        resetTimer();
        return () => clearInterval(timerRef.current);
    }, [count, resetTimer]);

    const goTo = (idx) => {
        setCurrent(idx);
        resetTimer();
    };
    const prev = () => goTo((current - 1 + count) % count);
    const next = () => goTo((current + 1) % count);

    if (count === 0) return null;

    const slide = slides[current];
    const Icon = SECTION_ICONS[slide.slug] ?? CartIcon;

    // Productos destacados de esta sección para el visual
    const matchSection = sections.find((s) => s.slug === slide.slug);
    const slideProducts = matchSection
        ? (savingsBySection.get(matchSection.id) ?? []).slice(0, 4)
        : [];

    return (
        <section
            className={styles.carousel}
            aria-roledescription="carousel"
            aria-label="Secciones destacadas"
        >
            <div className={styles.carouselSlide} key={slide.slug}>
                <div className={styles.carouselContent}>
                    <span className={styles.carouselBadge}>
                        <span className={styles.carouselBadgeIcon}><Icon /></span>
                        {slide.badge}
                    </span>

                    <h1 className={styles.carouselTitle}>
                        {slide.titleLine1}
                        <br />
                        <span className={styles.carouselAccent}>{slide.titleAccent}</span>
                    </h1>

                    <p className={styles.carouselDesc}>{slide.description}</p>

                    <Link
                        to={`/secciones/${slide.slug}`}
                        className={styles.carouselCta}
                    >
                        {slide.cta} →
                    </Link>
                </div>

                <div className={styles.carouselVisual}>
                    {slideProducts.length > 0 ? (
                        <div className={styles.carouselProducts}>
                            {slideProducts.map((p, i) => (
                                <div
                                    key={p.id}
                                    className={styles.carouselProductCard}
                                    style={{ '--card-i': i }}
                                >
                                    <ProductImage
                                        src={p.imageUrl}
                                        alt={p.name}
                                        className={styles.carouselProductImg}
                                    />
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className={styles.carouselIconLarge}>
                            <Icon />
                        </div>
                    )}
                </div>
            </div>

            {/* Flechas */}
            {count > 1 && (
                <>
                    <button className={`${styles.carouselArrow} ${styles.carouselArrowLeft}`}
                        onClick={prev} aria-label="Anterior">
                        ‹
                    </button>
                    <button className={`${styles.carouselArrow} ${styles.carouselArrowRight}`}
                        onClick={next} aria-label="Siguiente">
                        ›
                    </button>
                </>
            )}

            {/* Dots */}
            {count > 1 && (
                <div className={styles.carouselDots} role="tablist" aria-label="Diapositivas">
                    {slides.map((s, i) => (
                        <button
                            key={s.slug}
                            role="tab"
                            aria-selected={i === current}
                            aria-label={`Ir a ${s.badge}`}
                            className={`${styles.dot} ${i === current ? styles.dotActive : ''}`}
                            onClick={() => goTo(i)}
                        />
                    ))}
                </div>
            )}
        </section>
    );
}

/* ═══════════════ TARJETA DE PRODUCTO CON AHORRO ═══════════════ */
function SavingsCard({ product, onZoom }) {
    const saving = Number(product.maxPrice) - Number(product.minPrice);

    return (
        <div className={styles.savingsCard}>
            <button
                type="button"
                className={styles.imageButton}
                onClick={() => onZoom(product.imageUrl, product.name)}
                disabled={!product.imageUrl}
            >
                <ProductImage
                    src={product.imageUrl}
                    alt={product.name}
                    className={styles.savingsCardImage}
                />
            </button>

            <div className={styles.savingsCardBody}>
                <Link
                    to={`/comparador?productId=${product.id}`}
                    className={styles.savingsCardName}
                >
                    {product.name}
                </Link>

                <div className={styles.savingsCardPrices}>
                    <span className={styles.priceMin}>{money(product.minPrice)}</span>
                    {saving > 0 && (
                        <span className={styles.priceDiff}>
                            Ahorras {money(saving)} ({product.savingsPct}%)
                        </span>
                    )}
                </div>

                <p className={styles.savingsCardMeta}>
                    {product.storeCount} {product.storeCount === 1 ? 'tienda' : 'tiendas'}
                </p>
            </div>
        </div>
    );
}

/* ═══════════════ TARJETA DE PRODUCTO POR TIENDA ═══════════════ */
function StoreProductCard({ product, onZoom }) {
    return (
        <div className={styles.storeProduct}>
            <button
                type="button"
                className={styles.imageButton}
                onClick={() => onZoom(product.imageUrl, product.name)}
                disabled={!product.imageUrl}
            >
                <ProductImage
                    src={product.imageUrl}
                    alt={product.name}
                    className={styles.storeProductImage}
                />
            </button>

            <span className={styles.storeProductPrice}>{money(product.minPrice)}</span>

            <Link
                to={`/comparador?productId=${product.id}`}
                className={styles.storeProductName}
            >
                {product.name}
            </Link>

            <Link
                to={`/comparador?productId=${product.id}`}
                className={styles.storeProductCompare}
            >
                Comparar en todas las tiendas →
            </Link>
        </div>
    );
}

/* ═══════════════ FILA DE TIENDA ═══════════════ */
function StoreRow({ group, onZoom }) {
    const { store, products } = group;

    return (
        <div className={styles.storeRow}>
            <div className={styles.storeRowHead}>
                <div className={styles.storeRowInfo}>
                    <StoreLogo name={store.name} size="md" decorative={false} />
                    <div>
                        <h3 className={styles.storeRowName}>{store.name}</h3>
                        <span className={styles.storeRowCategory}>{store.category}</span>
                    </div>
                </div>
                <Link to="/buscar" className={styles.storeRowLink}>
                    Ver todo →
                </Link>
            </div>

            <div className={styles.storeProductGrid}>
                {products.map((p) => (
                    <StoreProductCard key={p.id} product={p} onZoom={onZoom} />
                ))}
            </div>
        </div>
    );
}

/* ═══════════════ FILA DE SECCIÓN CON SCROLL HORIZONTAL ═══════════════ */
function SectionRow({ section, products, onZoom }) {
    if (!products || products.length === 0) return null;

    const Icon = SECTION_ICONS[section.slug] ?? CartIcon;

    return (
        <div className={styles.sectionRow}>
            <div className={styles.sectionRowHead}>
                <div className={styles.sectionRowTitle}>
                    <span className={styles.sectionRowIcon}><Icon /></span>
                    <h3>{section.title}</h3>
                </div>
                <Link
                    to={`/secciones/${section.slug}`}
                    className={styles.sectionRowLink}
                >
                    Ver todo →
                </Link>
            </div>

            <div className={styles.scrollTrack}>
                {products.slice(0, 8).map((product) => (
                    <div key={product.id} className={styles.scrollCard}>
                        <button
                            type="button"
                            className={styles.imageButton}
                            onClick={() => onZoom(product.imageUrl, product.name)}
                            disabled={!product.imageUrl}
                        >
                            <ProductImage
                                src={product.imageUrl}
                                alt={product.name}
                                className={styles.scrollCardImage}
                            />
                        </button>

                        <Link
                            to={`/comparador?productId=${product.id}`}
                            className={styles.scrollCardName}
                        >
                            {product.name}
                        </Link>

                        <span className={styles.scrollCardPrice}>{money(product.minPrice)}</span>

                        {product.savingsPct > 0 && (
                            <span className={styles.scrollCardSaving}>
                                -{product.savingsPct}%
                            </span>
                        )}
                    </div>
                ))}
            </div>
        </div>
    );
}

/* ═══════════════════════════════════════════════════════════════ */
export default function Explore() {
    const navigate = useNavigate();
    const [query, setQuery] = useState('');
    const { status, sections, allSavings, savingsBySection, storeGroups } = useExplore();

    // Tab activo de ahorro: null = "Todo", o un sectionId
    const [activeTab, setActiveTab] = useState(null);

    // Tab activo de tiendas: null = "Todo", o un nombre de sección (category)
    const [storeTab, setStoreTab] = useState(null);

    // Lightbox
    const [lightbox, setLightbox] = useState(null);
    const openLightbox = useCallback((src, alt) => {
        if (src) setLightbox({ src, alt });
    }, []);
    const closeLightbox = useCallback(() => setLightbox(null), []);

    useEffect(() => {
        if (!lightbox) return;
        const handleKey = (e) => { if (e.key === 'Escape') closeLightbox(); };
        window.addEventListener('keydown', handleKey);
        return () => window.removeEventListener('keydown', handleKey);
    }, [lightbox, closeLightbox]);

    const handleSearch = (e) => {
        e.preventDefault();
        const term = query.trim();
        navigate(term ? `/buscar?q=${encodeURIComponent(term)}` : '/buscar');
    };

    // Productos filtrados por tab de ahorro
    const filteredSavings = activeTab === null
        ? allSavings
        : allSavings.filter((p) => p.sectionId === activeTab);

    // Secciones activas (con productos)
    const activeSections = sections.filter((s) => s.available);

    // Tiendas filtradas por tab de sección
    const filteredStoreGroups = storeTab === null
        ? storeGroups
        : storeGroups.filter((g) => g.store.category === storeTab);

    // Categorías únicas para los tabs de tienda
    const storeCategories = [...new Set(storeGroups.map((g) => g.store.category))];

    return (
        <div className={styles.page}>
            {/* ═══════════ HERO CAROUSEL ═══════════ */}
            <HeroCarousel sections={activeSections} savingsBySection={savingsBySection} />

            {/* ═══════════ BUSCADOR ═══════════ */}
            <section className={styles.searchSection}>
                <form className={styles.searchForm} onSubmit={handleSearch} role="search">
                    <span className={styles.searchIcon} aria-hidden="true">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
                            stroke="currentColor" strokeWidth="2.2" strokeLinecap="round">
                            <circle cx="11" cy="11" r="7" />
                            <path d="m20 20-3.5-3.5" />
                        </svg>
                    </span>
                    <input
                        type="search"
                        placeholder="Busca arroz, shampoo, crema facial..."
                        value={query}
                        onChange={(e) => setQuery(e.target.value)}
                        className={styles.searchInput}
                        aria-label="Buscar productos"
                    />
                    <button type="submit" className={styles.searchButton}>
                        Comparar →
                    </button>
                </form>
            </section>

            {/* ═══════════ SECCIONES COMO PILLS ═══════════ */}
            {activeSections.length > 0 && (
                <nav className={styles.sectionPills} aria-label="Secciones disponibles">
                    {activeSections.map((s) => {
                        const Icon = SECTION_ICONS[s.slug] ?? CartIcon;
                        return (
                            <Link
                                key={s.slug}
                                to={`/secciones/${s.slug}`}
                                className={styles.pill}
                            >
                                <span className={styles.pillIcon}><Icon /></span>
                                <span className={styles.pillLabel}>{s.title}</span>
                                <span className={styles.pillCount}>
                                    {s.productCount?.toLocaleString('es-CO')} productos
                                </span>
                            </Link>
                        );
                    })}
                </nav>
            )}

            {/* ═══════════ EN LAS TIENDAS HOY ═══════════ */}
            {storeGroups.length > 0 && (
                <section className={styles.storeSection} aria-labelledby="store-title">
                    <div className={styles.storeSectionHead}>
                        <div>
                            <span className={styles.storeSectionLabel}>POR TIENDA</span>
                            <h2 id="store-title" className={styles.sectionTitle}>
                                En las tiendas hoy
                            </h2>
                            <p className={styles.storeSectionHint}>
                                Productos y precios del momento. Toca uno para compararlo.
                            </p>
                        </div>
                    </div>

                    {/* Tabs de filtro por sección/categoría */}
                    {storeCategories.length > 1 && (
                        <div className={styles.tabs} role="tablist" aria-label="Filtrar por sección">
                            <button
                                role="tab"
                                aria-selected={storeTab === null}
                                className={`${styles.tab} ${storeTab === null ? styles.tabActive : ''}`}
                                onClick={() => setStoreTab(null)}
                            >
                                Todo
                            </button>
                            {storeCategories.map((cat) => (
                                <button
                                    key={cat}
                                    role="tab"
                                    aria-selected={storeTab === cat}
                                    className={`${styles.tab} ${storeTab === cat ? styles.tabActive : ''}`}
                                    onClick={() => setStoreTab(cat)}
                                >
                                    {cat}
                                </button>
                            ))}
                        </div>
                    )}

                    {/* Filas de tiendas */}
                    {filteredStoreGroups.map((group) => (
                        <StoreRow key={group.store.id} group={group} onZoom={openLightbox} />
                    ))}

                    {filteredStoreGroups.length === 0 && (
                        <p className={styles.empty}>No hay productos en esta sección.</p>
                    )}
                </section>
            )}

            {/* ═══════════ DONDE MÁS AHORRAS HOY ═══════════ */}
            {allSavings.length > 0 && (
                <section className={styles.savingsSection} aria-labelledby="savings-title">
                    <div className={styles.savingsHead}>
                        <h2 id="savings-title" className={styles.sectionTitle}>
                            Donde más ahorras hoy
                        </h2>
                        <p className={styles.savingsHint}>
                            El mismo producto en varias tiendas. El porcentaje es lo que pagas de menos frente a la tienda más cara.
                        </p>
                    </div>

                    <div className={styles.tabs} role="tablist" aria-label="Filtrar por sección">
                        <button
                            role="tab"
                            aria-selected={activeTab === null}
                            className={`${styles.tab} ${activeTab === null ? styles.tabActive : ''}`}
                            onClick={() => setActiveTab(null)}
                        >
                            Todo
                        </button>
                        {activeSections.map((s) => (
                            <button
                                key={s.id}
                                role="tab"
                                aria-selected={activeTab === s.id}
                                className={`${styles.tab} ${activeTab === s.id ? styles.tabActive : ''}`}
                                onClick={() => setActiveTab(s.id)}
                            >
                                {s.title}
                            </button>
                        ))}
                    </div>

                    <div className={styles.savingsGrid}>
                        {filteredSavings.slice(0, 12).map((product) => (
                            <SavingsCard key={product.id} product={product} onZoom={openLightbox} />
                        ))}
                    </div>

                    {filteredSavings.length === 0 && (
                        <p className={styles.empty}>No hay productos con ahorro en esta sección.</p>
                    )}
                </section>
            )}

            {/* ═══════════ POR SECCIÓN — CARRUSELES ═══════════ */}
            {activeSections.length > 0 && (
                <section className={styles.bySectionBlock} aria-labelledby="by-section-title">
                    <h2 id="by-section-title" className={styles.sectionTitle}>
                        Por sección
                    </h2>
                    <p className={styles.bySectionHint}>
                        Productos con mayor ahorro en cada sección.
                    </p>

                    {activeSections.map((s) => (
                        <SectionRow
                            key={s.slug}
                            section={s}
                            products={savingsBySection.get(s.id) ?? []}
                            onZoom={openLightbox}
                        />
                    ))}
                </section>
            )}

            {/* ═══════════ LOADING ═══════════ */}
            {status === 'loading' && (
                <div className={styles.loadingBlock}>
                    <div className={styles.spinner} />
                    <p>Cargando productos...</p>
                </div>
            )}

            {/* ═══════════ LIGHTBOX ═══════════ */}
            {lightbox && (
                <div className={styles.lightboxOverlay} onClick={closeLightbox}
                    role="dialog" aria-modal="true" aria-label={lightbox.alt}>
                    <button type="button" className={styles.lightboxClose}
                        onClick={closeLightbox} aria-label="Cerrar">
                        ✕
                    </button>
                    <img src={lightbox.src} alt={lightbox.alt}
                        className={styles.lightboxImage}
                        onClick={(e) => e.stopPropagation()} />
                </div>
            )}
        </div>
    );
}