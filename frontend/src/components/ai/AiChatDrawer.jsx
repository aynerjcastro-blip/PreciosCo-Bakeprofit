import { useState, useRef, useEffect } from 'react';
import ReactMarkdown from 'react-markdown';
import { sendAiMessage } from '../../api/ai';
import s from '@/components/ai/AiChatDrawer.module.css';
import {
    SuperAvatar,
    DrogueriaAvatar,
    CuidadoAvatar,
} from '@/components/ui/icons/AiAvatars';

/* ───────────────────────────────────────────
   Configuración por sección
   ─────────────────────────────────────────── */

const SECTION_CONFIG = {
    supermercados: { label: 'CanastaBot', sub: 'Supermercados', Icon: SuperAvatar, color: '#E8740C' },
    droguerias: { label: 'FarmaBot', sub: 'Droguerías & Salud', Icon: DrogueriaAvatar, color: '#2E7D32' },
    'cuidado-facial': { label: 'GlowBot', sub: 'Facial & Belleza', Icon: CuidadoAvatar, color: '#7B1FA2' },
};

/** Chips de consultas sugeridas (welcome) */
const SUGGESTIONS = {
    supermercados: [
        'Arroz 1kg más barato',
        'Leche deslactosada',
        'Café 500g',
    ],
    droguerias: [
        'Acetaminofén genérico',
        'Protector solar',
        'Kit primeros auxilios',
    ],
    'cuidado-facial': [
        'Sérum vitamina C',
        'Hidratante piel grasa',
        'Protector solar facial',
    ],
};

/** Formatea segundos a "0.0s" */
function formatTime(ms) {
    return (ms / 1000).toFixed(1) + 's';
}

/* ───────────────────────────────────────────
   Componente
   ─────────────────────────────────────────── */

export default function AiChatDrawer({ section, onClose }) {

    // ── Config de la sección activa ──
    const cfg = SECTION_CONFIG[section] || SECTION_CONFIG.supermercados;

    // ── Key para sessionStorage (una por sección) ──
    const storageKey = `ai-chat-${section}`;

    // ── Estado: mensajes (se rehidratan de sessionStorage) ──
    const [messages, setMessages] = useState(() => {
        try {
            const saved = sessionStorage.getItem(storageKey);
            return saved ? JSON.parse(saved) : [];
        } catch {
            return [];
        }
    });

    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [elapsed, setElapsed] = useState(0);   // ms transcurridos
    const bottomRef = useRef(null);
    const timerRef = useRef(null);   // referencia al interval

    // ── Persistir mensajes en sessionStorage ──
    useEffect(() => {
        if (messages.length > 0) {
            sessionStorage.setItem(storageKey, JSON.stringify(messages));
        }
    }, [messages, storageKey]);

    // ── Auto-scroll al último mensaje ──
    useEffect(() => {
        bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages, loading]);

    // ── Timer: cuenta mientras loading === true ──
    // ── Timer: cuenta mientras loading === true ──
    useEffect(() => {
        if (!loading) return;

        const start = Date.now();
        timerRef.current = setInterval(() => {
            setElapsed(Date.now() - start);
        }, 100);

        return () => clearInterval(timerRef.current);
    }, [loading]);

    // ── Limpiar historial manualmente (botón 🗑) ──
    function handleClear() {
        setMessages([]);
        sessionStorage.removeItem(storageKey);
    }

    // ── Enviar mensaje ──
    async function handleSend(e) {
        e.preventDefault();
        const text = input.trim();
        if (!text || loading) return;

        const userMsg = { role: 'user', content: text };
        setMessages(prev => [...prev, userMsg]);
        setInput('');
        setError(null);
        setElapsed(0);
        setLoading(true);

        const start = Date.now();                        // ← captura el inicio

        try {
            const { reply } = await sendAiMessage({
                section,
                message: text,
                history: messages,
            });
            const duration = Date.now() - start;         // ← tiempo real
            setMessages(prev => [
                ...prev,
                { role: 'assistant', content: reply, time: duration },
            ]);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    // ── Render ──
    return (
        <div className={s.overlay} onClick={onClose}>
            <aside
                className={s.drawer}
                onClick={e => e.stopPropagation()}
                role="dialog"
                aria-label={`Chat IA — ${cfg.sub}`}
            >
                {/* ── Header ── */}
                <header className={s.header} style={{ background: cfg.color }}>
                    <cfg.Icon />
                    <div className={s.headerText}>
                        <span className={s.botName}>{cfg.label}</span>
                        <span className={s.botSub}>{cfg.sub}</span>
                    </div>
                    <div className={s.headerActions}>
                        <button className={s.iconBtn} onClick={handleClear}
                            title="Limpiar chat" aria-label="Limpiar chat">
                            🗑
                        </button>
                        <button className={s.iconBtn} onClick={onClose}
                            title="Cerrar" aria-label="Cerrar chat">
                            ✕
                        </button>
                    </div>
                </header>

                {/* ── Cuerpo ── */}
                <div className={s.body}>

                    {/* Welcome + chips */}
                    {messages.length === 0 && (
                        <div className={s.welcome}>
                            <cfg.Icon />
                            <p className={s.welcomeText}>
                                ¡Hola! Soy <strong>{cfg.label}</strong>, tu asistente
                                de {cfg.sub.toLowerCase()}.
                                ¿Qué producto necesitas cotizar?
                            </p>
                            <div className={s.chips}>
                                {(SUGGESTIONS[section] || []).map(text => (
                                    <button
                                        key={text}
                                        className={s.chip}
                                        onClick={() => setInput(text)}
                                    >
                                        {text}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* Mensajes */}
                    {messages.map((msg, i) => (
                        <div key={i} className={`${s.bubble} ${msg.role === 'user' ? s.user : s.assistant
                            }`}>
                            {msg.role === 'user'
                                ? msg.content
                                : <ReactMarkdown>{msg.content}</ReactMarkdown>
                            }
                            {/* Tiempo de respuesta (solo asistente) */}
                            {msg.role === 'assistant' && msg.time != null && (
                                <span className={s.responseTime}>
                                    ⚡ {formatTime(msg.time)}
                                </span>
                            )}
                        </div>
                    ))}

                    {/* Loading con timer en vivo */}
                    {loading && (
                        <div className={`${s.bubble} ${s.assistant}`}>
                            <span className={s.dots}>···</span>
                            <span className={s.liveTimer}>
                                {formatTime(elapsed)}
                            </span>
                        </div>
                    )}

                    {error && <p className={s.error}>⚠️ {error}</p>}
                    <div ref={bottomRef} />
                </div>

                {/* ── Input ── */}
                <form className={s.form} onSubmit={handleSend}>
                    <input
                        className={s.input}
                        type="text"
                        value={input}
                        onChange={e => setInput(e.target.value)}
                        placeholder={`Pregúntale a ${cfg.label}...`}
                        disabled={loading}
                        autoFocus
                    />
                    <button className={s.send} type="submit"
                        disabled={loading || !input.trim()}>
                        Enviar
                    </button>
                </form>
            </aside>
        </div>
    );
}