import { SuperAvatar, DrogueriaAvatar, CuidadoAvatar } from '../ui/icons/AiAvatars';
import styles from './AiDock.module.css';

const AI_SECTIONS = [
    { slug: 'supermercados', label: 'Supermercados', Icon: SuperAvatar },
    { slug: 'droguerias', label: 'Droguerías', Icon: DrogueriaAvatar },
    { slug: 'cuidado-facial', label: 'Cuidado facial', Icon: CuidadoAvatar },
];

export default function AiDock({ onOpenChat }) {
    return (
        <aside className={styles.dock} aria-label="Asistentes de IA">
            <ul className={styles.list}>
                {AI_SECTIONS.map(({ slug, label, Icon }) => (
                    <li key={slug}>
                        <button
                            className={styles.avatar}
                            onClick={() => onOpenChat(slug)}
                            title={`Chat IA — ${label}`}
                            aria-label={`Abrir chat de IA para ${label}`}
                        >
                            <Icon />
                        </button>
                    </li>
                ))}
            </ul>
        </aside>
    );
}