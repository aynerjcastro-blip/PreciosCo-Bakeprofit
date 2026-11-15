import { Link } from 'react-router';
import { useSections } from '@/hooks/useSection';
import styles from '@/components/sectionsBlock/SectionBlock.module.css';

// Intl formatea según el idioma: 45000 -> "45.000" con es-CO
const numberFormat = new Intl.NumberFormat('es-CO');
const SKELETONS = [1, 2, 3, 4];

export default function SectionsBlock() {
    const { status, sections } = useSections();

    // Degradación elegante: si el backend falla, el bloque desaparece. Mostrar cifras de
    // respaldo o "Próximamente" en las cuatro sería mentirle al visitante.
    if (status === 'error') return null;

    return (
        <section className={styles.block} aria-labelledby="sections-title" aria-busy={status === 'loading'}>
            <h2 id="sections-title" className={styles.title}>Compara por sección</h2>

            <ul className={styles.grid}>
                {status === 'loading'
                    ? SKELETONS.map((n) => (
                        <li key={n} className={`${styles.card} ${styles.skeleton}`} aria-hidden="true" />
                    ))
                    : sections.map((s) => (
                        <li key={s.slug} className={styles.card}>
                            <h3 className={styles.cardTitle}>{s.title}</h3>
                            <p className={styles.description}>{s.description}</p>

                            {s.available ? (
                                <>
                                    <p className={styles.meta}>
                                        {numberFormat.format(s.productCount)}{' '}
                                        {s.productCount === 1 ? 'producto' : 'productos'} · {s.storeCount}{' '}
                                        {s.storeCount === 1 ? 'tienda' : 'tiendas'}
                                    </p>
                                    <Link to={`/secciones/${s.slug}`} className={styles.link}>
                                        Ver sección
                                    </Link>
                                </>
                            ) : (
                                <span className={styles.soon}>Próximamente</span>
                            )}
                        </li>
                    ))}
            </ul>
        </section>
    );
}