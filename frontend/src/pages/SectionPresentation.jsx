import { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useSectionBySlug } from '@/hooks/useSectionsBySlug';
import styles from '@/pages/SectionPresentation.module.css';

const numberFormat = new Intl.NumberFormat('es-CO');
const dateFormat = new Intl.DateTimeFormat('es-CO', { dateStyle: 'long' });

/**
 * UN solo componente para las cuatro secciones: lo único que cambia es el slug de la URL
 * (/secciones/:slug); textos y avisos salen de config/sections.js y las cifras del backend.
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
            <main className={styles.page} aria-busy="true">
                <div className={styles.skeleton} aria-hidden="true" />
            </main>
        );
    }

    if (status === 'error') {
        return (
            <main className={styles.page}>
                <h1 className={styles.title}>No pudimos cargar la sección</h1>
                <p className={styles.description}>Revisa tu conexión e inténtalo de nuevo.</p>
                <button type="button" className={styles.button} onClick={reload}>Reintentar</button>
            </main>
        );
    }

    if (!section) {
        return (
            <main className={styles.page}>
                <h1 className={styles.title}>Sección no encontrada</h1>
                <p className={styles.description}>La sección que buscas no existe.</p>
                <Link to="/" className={styles.button}>Volver al inicio</Link>
            </main>
        );
    }

    return (
        <main className={styles.page}>
            <h1 className={styles.title}>{section.title}</h1>
            <p className={styles.description}>{section.description}</p>

            {section.available ? (
                <>
                    <dl className={styles.stats}>
                        <div>
                            <dt>Productos con precio</dt>
                            <dd>{numberFormat.format(section.productCount)}</dd>
                        </div>
                        <div>
                            <dt>{section.storeCount === 1 ? 'Tienda' : 'Tiendas'}</dt>
                            <dd>{section.storeCount}</dd>
                        </div>
                        {stats?.lastUpdate && (
                            <div>
                                <dt>Última actualización</dt>
                                <dd>{dateFormat.format(new Date(stats.lastUpdate))}</dd>
                            </div>
                        )}
                    </dl>

                    {/* El contrato con el buscador: la sección viaja como ?sector=<slug> en la URL */}
                    <Link to={`/buscar?sector=${section.slug}`} className={styles.button}>
                        Buscar y comparar
                    </Link>
                </>
            ) : (
                <p className={styles.soon}>Próximamente: estamos incorporando tiendas a esta sección.</p>
            )}

            <p className={styles.notice}>
                Los precios son informativos y pueden cambiar: confirma en la tienda antes de comprar.
                {section.notice && ` ${section.notice}`}
            </p>
        </main>
    );
}