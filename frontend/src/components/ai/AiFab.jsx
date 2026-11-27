import { useState } from 'react';
import s from './AiFab.module.css';
import {
    SuperAvatar,
    DrogueriaAvatar,
    CuidadoAvatar,
} from '../ui/icons/AiAvatars';
import { ChatIcon, CloseIcon } from '../ui/icons/ChatIcons';

const SECTIONS = [
    { slug: 'supermercados', label: 'Supermercados', Icon: SuperAvatar },
    { slug: 'droguerias', label: 'Droguerías', Icon: DrogueriaAvatar },
    { slug: 'cuidado-facial', label: 'Cuidado facial', Icon: CuidadoAvatar },
];

/**
 * Botón flotante visible solo en móvil (<768px).
 * Al tocar, despliega un mini-menú con los avatares de sección.
 * Al elegir una sección, abre el AiChatDrawer.
 */
export default function AiFab({ onOpenChat }) {
    const [open, setOpen] = useState(false);

    function handleSelect(slug) {
        setOpen(false);
        onOpenChat(slug);
    }

    return (
        <div className={s.wrapper}>
            {/* Mini-menú de secciones */}
            {open && (
                <>
                    <div className={s.backdrop} onClick={() => setOpen(false)} />
                    <div className={s.menu}>
                        {SECTIONS.map(({ slug, label, Icon }) => (
                            <button
                                key={slug}
                                className={s.menuItem}
                                onClick={() => handleSelect(slug)}
                            >
                                <Icon />
                                <span>{label}</span>
                            </button>
                        ))}
                    </div>
                </>
            )}

            {/* FAB */}
            <button
                className={s.fab}
                onClick={() => setOpen(prev => !prev)}
                aria-label="Abrir chat IA"
            >
                {open ? <CloseIcon size={24} /> : <ChatIcon size={26} />}
            </button>
        </div>
    );
}