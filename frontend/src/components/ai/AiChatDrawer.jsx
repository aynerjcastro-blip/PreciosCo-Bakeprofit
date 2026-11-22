import { useState, useRef, useEffect } from 'react';
import { sendAiMessage } from '../../api/ai';
import styles from '@/components/ai/AiChatDrawer.module.css';
import { SuperAvatar, DrogueriaAvatar, CuidadoAvatar } from '@/components/ui/icons/AiAvatars';
const SECTION_LABELS = {
    'supermercados': 'Supermercados',
    'droguerias': 'Droguerías',
    'cuidado-facial': 'Cuidado facial',
};

const SECTION_CONFIG = {
    'supermercados': { label: SECTION_LABELS.supermercados, Avatar: SuperAvatar },
    'droguerias': { label: SECTION_LABELS.droguerias, Avatar: DrogueriaAvatar },
    'cuidado-facial': { label: SECTION_LABELS['cuidado-facial'], Avatar: CuidadoAvatar },
};

export default function AiChatDrawer({ section, onClose }) {
    const [messages, setMessages] = useState([]);   // {role, content}
    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const bottomRef = useRef(null);
    const { label, Avatar } = SECTION_CONFIG[section];
    // Auto-scroll al último mensaje
    useEffect(() => {
        bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    async function handleSend(e) {
        e.preventDefault();
        const text = input.trim();
        if (!text || loading) return;

        const userMsg = { role: 'user', content: text };
        setMessages(prev => [...prev, userMsg]);
        setInput('');
        setError(null);
        setLoading(true);

        try {
            const { reply } = await sendAiMessage({
                section,
                message: text,
                history: messages,  // todo el historial anterior
            });
            setMessages(prev => [...prev, { role: 'assistant', content: reply }]);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className={styles.overlay} onClick={onClose}>
            <aside
                className={styles.drawer}
                onClick={e => e.stopPropagation()}
                role="dialog"
                aria-label={`Chat IA — ${SECTION_LABELS[section]}`}
            >
                {/* Header */}
                <header className={styles.header}>
                    <div className={styles.headerInfo}>
                        <Avatar />
                        <h3 className={styles.title}>{label}</h3>
                    </div>
                    <button className={styles.close} onClick={onClose} aria-label="Cerrar chat">
                        ✕
                    </button>
                </header>

                {/* Mensajes */}
                <div className={styles.body}>
                    {messages.length === 0 && (
                        <p className={styles.empty}>
                            Pregúntame sobre productos de {SECTION_LABELS[section].toLowerCase()}
                        </p>
                    )}
                    {messages.map((msg, i) => (
                        <div
                            key={i}
                            className={`${styles.bubble} ${msg.role === 'user' ? styles.user : styles.assistant
                                }`}
                        >
                            {msg.content}
                        </div>
                    ))}
                    {loading && (
                        <div className={`${styles.bubble} ${styles.assistant}`}>
                            <span className={styles.dots}>···</span>
                        </div>
                    )}
                    {error && (
                        <p className={styles.error}>⚠️ {error}</p>
                    )}
                    <div ref={bottomRef} />
                </div>

                {/* Input */}
                <form className={styles.form} onSubmit={handleSend}>
                    <input
                        className={styles.input}
                        type="text"
                        value={input}
                        onChange={e => setInput(e.target.value)}
                        placeholder="Escribe tu pregunta..."
                        disabled={loading}
                        autoFocus
                    />
                    <button
                        className={styles.send}
                        type="submit"
                        disabled={loading || !input.trim()}
                    >
                        Enviar
                    </button>
                </form>
            </aside>
        </div>
    );
}