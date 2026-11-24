import { useState, useRef, useEffect } from 'react';
import ReactMarkdown from 'react-markdown';
import { sendAiMessage, fetchAiQuota } from '../../api/ai';
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

/** Formatea milisegundos a "0.0s" */
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
    const [elapsed, setElapsed] = useState(0);
    const bottomRef = useRef(null);
    const timerRef = useRef(null);

    // ── Estado de cuota ──
    const [quota, setQuota] = useState(null); // { queriesUsed, queriesLimit, plan }

    // ── Cargar cuota al montar el componente ──
    useEffect(() => {
        fetchAiQuota()
            .then(setQuota)
            .catch(() => { }); // silencioso: si falla, no bloqueamos el chat
    }, []);

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

    // ── Texto del contador de cuota ──
    function quotaText() {
        if (!quota) return null;
        if (quota.plan === 'PREMIUM') return '∞ Premium';
        const remaining = Math.max(0, quota.queriesLimit - quota.queriesUsed);
        return `${remaining}/${quota.queriesLimit}`;
    }

    // ── ¿Cuota agotada? ──
    function isQuotaExhausted() {
        if (!quota || quota.plan === 'PREMIUM') return false;
        return quota.queriesUsed >= quota.queriesLimit;
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

        const start = Date.now();

        try {
            const data = await sendAiMessage({
                section,
                message: text,
                history: messages,
            });
            const duration = Date.now() - start;
            setMessages(prev => [
                ...prev,
                { role: 'assistant', content: data.reply, time: duration },
            ]);

            // Actualizar cuota con los datos que devuelve el backend
            if (data.queriesUsed != null) {
                setQuota({
                    queriesUsed: data.queriesUsed,
                    queriesLimit: data.queriesLimit,
                    plan: data.plan,
                });
            }
        } catch (err) {
            // Error 429 = cuota excedida
            if (err.status === 429) {
                setError('Has alcanzado tu límite de consultas este mes. Actualiza a Premium para consultas ilimitadas.');
                // Forzar cuota agotada en el estado local
                if (quota) {
                    setQuota({ ...quota, queriesUsed: quota.queriesLimit });
                }
            } else {
                setError(err.message);
            }
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
                        {/* Contador de cuota */}
                        {quotaText() && (
                            <span
                                className={`${s.quotaBadge} ${isQuotaExhausted() ? s.quotaExhausted : ''}`}
                                title={quota?.plan === 'PREMIUM'
                                    ? 'Plan Premium — consultas ilimitadas'
                                    : `Consultas restantes este mes`}
                            >
                                {quotaText()}
                            </span>
                        )}
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
                        placeholder={isQuotaExhausted()
                            ? 'Límite de consultas alcanzado'
                            : `Pregúntale a ${cfg.label}...`}
                        disabled={loading || isQuotaExhausted()}
                        autoFocus
                    />
                    <button className={s.send} type="submit"
                        disabled={loading || !input.trim() || isQuotaExhausted()}>
                        Enviar
                    </button>
                </form>
            </aside>
        </div>
    );
}