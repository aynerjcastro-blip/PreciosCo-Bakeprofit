import { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useSectionBySlug } from '@/hooks/useSectionsBySlug';
import styles from '@/pages/SectionPresentation.module.css';
import Icon from './Icon';

const numberFormat = new Intl.NumberFormat('es-CO');
const dateFormat = new Intl.DateTimeFormat('es-CO', { dateStyle: 'long' });

/**
 * UN solo componente para las cuatro secciones: lo único que cambia es el slug de la URL
 * (/secciones/:slug); textos y avisos salen de config/sections.js y las cifras del backend.
 *
 * Se renderiza dentro del <main> de MainLayout, por eso aquí se usa <section> y no <main>:
 * una página solo puede tener un <main>.
 */
export default function SectionPresentation() {
    const { slug } = useParams();
    const { status, section, stats, reload } = useSectionBySlug(slug);

    // Efecto de sincronización con algo externo al render: el título de la pestaña
    useEffect(() => {
        if (section) document.title = `${section.title} | PreciosCo`;
    }, [section]);

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
                    <button type="button" className={styles.button} onClick={reload}>Reintentar</button>
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
                    <Link to="/" className={styles.button}>Volver al inicio</Link>
                </div>
            </section>
        );
    }

    return (
        <section className={styles.page}>
            <header className={styles.hero}>
                <span className={`${styles.eyebrow} ${section.available ? '' : styles['eyebrow--soon']}`}>
                    {section.available ? 'Disponible' : 'Próximamente'}
                </span>
                <h1 className={styles.title}>{section.title}</h1>
                <p className={styles.description}>{section.description}</p>
            </header>

            {section.available ? (
                <>
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

                    <div className={styles.actions}>
                        {/* El contrato con el buscador: la sección viaja como ?sector=<slug> en la URL */}
                        <Link to={`/buscar?sector=${section.slug}`} className={styles.button}>
                            Buscar y comparar
                            <Icon name="arrow" size={18} />
                        </Link>

                        <p className={styles.notice}>
                            Los precios son informativos y pueden cambiar: confirma en la tienda antes de comprar.
                            {section.notice && ` ${section.notice}`}
                        </p>
                    </div>
                </>
            ) : (
                <>
                    <p className={styles.soon}>Estamos incorporando tiendas a esta sección.</p>
                    <p className={styles.notice}>
                        Los precios son informativos y pueden cambiar: confirma en la tienda antes de comprar.
                        {section.notice && ` ${section.notice}`}
                    </p>
                </>
            )}
        </section>
    );
}