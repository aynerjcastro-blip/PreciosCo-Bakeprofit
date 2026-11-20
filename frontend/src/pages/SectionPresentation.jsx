import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useSectionBySlug } from '@/hooks/useSectionsBySlug';
import { useSectionDetail } from '@/hooks/useSectionDetail';
import { getStoreLogo } from '@/config/storeLogos';
import styles from '@/pages/SectionPresentation.module.css';
import ProductImage from './ProductImage';
import Icon from './Icon';

const numberFormat = new Intl.NumberFormat('es-CO');
const currencyFormat = new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
});
const dateFormat = new Intl.DateTimeFormat('es-CO', { dateStyle: 'long' });

/**
 * Página de sección rediseñada (/secciones/:slug).
 *
 * Tres bloques nuevos sobre el CTA existente:
 *  1. Stats + chips de tiendas (confianza: "¿compara donde yo compro?")
 *  2. Ahorro destacado (prueba de valor: diferencia de precio real)
 *  3. Productos populares (atajos: no obligan a pensar qué buscar)
 *
 * Los datos extra vienen de useSectionDetail, que reutiliza endpoints existentes
 * (findActiveStores + findBestSavings) filtrados por sectionId.
 */
export default function SectionPresentation() {
    const { slug } = useParams();
    const { status, section, stats, reload } = useSectionBySlug(slug);
    const { stores, topSaving, popular } = useSectionDetail(section?.id ?? null);

    useEffect(() => {
        if (section) document.title = `${section.title} | PreciosCo`;
    }, [section]);

    /* ---------- Lightbox para ver imagen en grande ---------- */
    const [lightbox, setLightbox] = useState(null); // { src, alt }
    const openLightbox = useCallback((src, alt) => {
        if (src) setLightbox({ src, alt });
    }, []);
    const closeLightbox = useCallback(() => setLightbox(null), []);

    // Cerrar con Escape
    useEffect(() => {
        if (!lightbox) return;
        const handleKey = (e) => { if (e.key === 'Escape') closeLightbox(); };
        document.addEventListener('keydown', handleKey);
        return () => document.removeEventListener('keydown', handleKey);
    }, [lightbox, closeLightbox]);

    /* ---------- Estados de carga y error ---------- */

    if (status === 'loading') {
        return (
            <section className={styles.page} aria-busy="true">
                <div className={styles.skeleton} aria-hidden="true" />
            </section>
        );
    }

    if (status === 'error') {
        return (
            <section className={styles.page}>
                <header className={styles.hero}>
                    <h1 className={styles.title}>No pudimos cargar la sección</h1>
                    <p className={styles.description}>Revisa tu conexión e inténtalo de nuevo.</p>
                </header>
                <div className={styles.actions}>
                    <button type="button" className={styles.button} onClick={reload}>
                        Reintentar
                    </button>
                </div>
            </section>
        );
    }

    if (!section) {
        return (
            <section className={styles.page}>
                <header className={styles.hero}>
                    <h1 className={styles.title}>Sección no encontrada</h1>
                    <p className={styles.description}>La sección que buscas no existe.</p>
                </header>
                <div className={styles.actions}>
                    <Link to="/" className={styles.button}>
                        Volver al inicio
                    </Link>
                </div>
            </section>
        );
    }

    /* ---------- Sección con datos ---------- */

    const saving = topSaving
        ? Number(topSaving.maxPrice) - Number(topSaving.minPrice)
        : 0;

    return (
        <section className={styles.page}>
            {/* ── Hero ── */}
            <header className={styles.hero}>
                <span className={`${styles.eyebrow} ${section.available ? '' : styles['eyebrow--soon']}`}>
                    {section.available ? 'Disponible' : 'Próximamente'}
                </span>
                <h1 className={styles.title}>{section.title}</h1>
                <p className={styles.description}>{section.description}</p>
            </header>

            {section.available ? (
                <>
                    {/* ── Bloque 1: Stats + tiendas ── */}
                    <div className={styles.infoCard}>
                        <dl className={styles.stats}>
                            <div className={styles.stat}>
                                <dt>Productos con precio</dt>
                                <dd>{numberFormat.format(section.productCount)}</dd>
                            </div>
                            <div className={styles.stat}>
                                <dt>{section.storeCount === 1 ? 'Tienda' : 'Tiendas'}</dt>
                                <dd>{section.storeCount}</dd>
                            </div>
                            {stats?.lastUpdate && (
                                <div className={`${styles.stat} ${styles['stat--date']}`}>
                                    <dt>Última actualización</dt>
                                    <dd>{dateFormat.format(new Date(stats.lastUpdate))}</dd>
                                </div>
                            )}
                        </dl>

                        {/* Chips de tiendas: solo si ya cargaron */}
                        {stores.length > 0 && (
                            <div className={styles.storesRow}>
                                <span className={styles.storesLabel}>Comparamos en:</span>
                                <div className={styles.storeChips}>
                                    {stores.map((store) => {
                                        const logo = getStoreLogo(store.name);
                                        return (
                                            <span key={store.id} className={styles.storeChip} title={store.name}>
                                                {logo ? (
                                                    <img
                                                        src={logo}
                                                        alt={store.name}
                                                        className={styles.storeLogo}
                                                        loading="lazy"
                                                    />
                                                ) : (
                                                    <span className={styles.storeInitial} aria-hidden="true">
                                                        {store.name.charAt(0)}
                                                    </span>
                                                )}
                                            </span>
                                        );
                                    })}
                                </div>
                            </div>
                        )}
                    </div>

                    {/* ── Bloque 2: Ahorro destacado ── */}
                    {topSaving && saving > 0 && (
                        <div className={styles.savingSection}>
                            <h2 className={styles.savingHeading}>
                                Mayor diferencia de precio ahora
                            </h2>
                            <div className={styles.savingCard}>
                                <button
                                    type="button"
                                    className={styles.imageButton}
                                    onClick={() => openLightbox(topSaving.imageUrl, topSaving.name)}
                                    aria-label={`Ver imagen de ${topSaving.name}`}
                                >
                                    <ProductImage
                                        src={topSaving.imageUrl}
                                        alt={topSaving.name}
                                        className={styles.savingImage}
                                    />
                                </button>
                                <div className={styles.savingInfo}>
                                    <p className={styles.savingName}>{topSaving.name}</p>
                                    <p className={styles.savingMeta}>
                                        {topSaving.storeCount}{' '}
                                        {topSaving.storeCount === 1 ? 'tienda' : 'tiendas'}
                                    </p>
                                </div>
                                <div className={styles.savingPrices}>
                                    <div className={`${styles.priceTag} ${styles['priceTag--low']}`}>
                                        <span className={styles.priceLabel}>Más barato</span>
                                        <span className={styles.priceValue}>
                                            {currencyFormat.format(topSaving.minPrice)}
                                        </span>
                                    </div>
                                    <div className={styles.priceDivider} aria-hidden="true" />
                                    <div className={styles.priceTag}>
                                        <span className={styles.priceLabel}>Más caro</span>
                                        <span className={styles.priceValue}>
                                            {currencyFormat.format(topSaving.maxPrice)}
                                        </span>
                                    </div>
                                </div>
                            </div>
                            <p className={styles.savingBadge}>
                                ↓ Ahorras hasta {currencyFormat.format(saving)}
                            </p>
                        </div>
                    )}

                    {/* ── Bloque 3: Productos populares ── */}
                    {popular.length > 0 && (
                        <div className={styles.popularSection}>
                            <h2 className={styles.popularHeading}>
                                Más buscados en {section.title.toLowerCase()}
                            </h2>
                            <div className={styles.popularGrid}>
                                {popular.map((product) => (
                                    <div key={product.id} className={styles.popularCard}>
                                        <button
                                            type="button"
                                            className={styles.imageButton}
                                            onClick={() => openLightbox(product.imageUrl, product.name)}
                                            aria-label={`Ver imagen de ${product.name}`}
                                        >
                                            <ProductImage
                                                src={product.imageUrl}
                                                alt={product.name}
                                                className={styles.popularCardImage}
                                            />
                                        </button>
                                        <Link
                                            to={`/buscar?sector=${section.slug}&name=${encodeURIComponent(product.name)}`}
                                            className={styles.popularCardLink}
                                        >
                                            <p className={styles.popularCardName}>{product.name}</p>
                                            <p className={styles.popularCardPrice}>
                                                Desde {currencyFormat.format(product.minPrice)}
                                            </p>
                                            <p className={styles.popularCardStores}>
                                                {product.storeCount}{' '}
                                                {product.storeCount === 1 ? 'tienda' : 'tiendas'}
                                            </p>
                                        </Link>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* ── CTA + Notice ── */}
                    <div className={styles.actions}>
                        <Link to={`/buscar?sector=${section.slug}`} className={styles.button}>
                            Buscar y comparar
                            <Icon name="arrow" size={18} />
                        </Link>
                        <p className={styles.notice}>
                            Los precios son informativos y pueden cambiar.
                            {section.notice && ` ${section.notice}`}
                        </p>
                    </div>
                </>
            ) : (
                <>
                    <p className={styles.soon}>Estamos incorporando tiendas a esta sección.</p>
                    <p className={styles.notice}>
                        Los precios son informativos y pueden cambiar.
                        {section.notice && ` ${section.notice}`}
                    </p>
                </>
            )}

            {/* ── Lightbox modal ── */}
            {lightbox && (
                <div
                    className={styles.lightboxOverlay}
                    onClick={closeLightbox}
                    role="dialog"
                    aria-modal="true"
                    aria-label={lightbox.alt}
                >
                    <button
                        type="button"
                        className={styles.lightboxClose}
                        onClick={closeLightbox}
                        aria-label="Cerrar"
                    >
                        ✕
                    </button>
                    <img
                        src={lightbox.src}
                        alt={lightbox.alt}
                        className={styles.lightboxImage}
                        onClick={(e) => e.stopPropagation()}
                        referrerPolicy="no-referrer"
                    />
                </div>
            )}
        </section>
    );
}