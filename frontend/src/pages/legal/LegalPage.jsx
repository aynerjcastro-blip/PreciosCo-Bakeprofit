import { Link } from 'react-router-dom';
import styles from './LegalPage.module.css';

/**
 * Plantilla de las páginas legales (términos y privacidad): título, fecha de vigencia,
 * índice con enlaces a cada sección y el contenido.
 *
 * @param {{ title: string, updated: string, intro: React.ReactNode,
 *           sections: Array<{ id: string, title: string, content: React.ReactNode }>,
 *           related: { to: string, label: string } }} props
 */
export default function LegalPage({ title, updated, intro, sections, related }) {
    return (
        <div className={styles.page}>
            <header className={styles.header}>
                <h1 className={styles.title}>{title}</h1>
                <p className={styles.updated}>Última actualización: {updated}</p>
                <div className={styles.intro}>{intro}</div>
            </header>

            <nav className={styles.toc} aria-label="Contenido">
                <p className={styles.tocTitle}>Contenido</p>
                <ol>
                    {sections.map(s => (
                        <li key={s.id}><a href={`#${s.id}`}>{s.title}</a></li>
                    ))}
                </ol>
            </nav>

            <div className={styles.body}>
                {sections.map((s, i) => (
                    <section key={s.id} id={s.id} className={styles.section} aria-labelledby={`${s.id}-t`}>
                        <h2 id={`${s.id}-t`}>{i + 1}. {s.title}</h2>
                        {s.content}
                    </section>
                ))}
            </div>

            {related && (
                <p className={styles.related}>
                    Consulta también: <Link to={related.to}>{related.label}</Link>
                </p>
            )}
        </div>
    );
}
