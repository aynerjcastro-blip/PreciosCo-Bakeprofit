import { useState, useRef, useEffect, useCallback } from 'react';
import ReactMarkdown from 'react-markdown';
import { streamAiMessage, fetchAiQuota } from '../../api/ai';
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

/**
 * Extrae el bloque [SUGERENCIAS] del texto de respuesta de la IA.
 * Retorna { cleanText, suggestions: string[] }
 */
function parseSuggestions(text) {
    const marker = '[SUGERENCIAS]';
    const idx = text.indexOf(marker);

    if (idx === -1) {
        return { cleanText: text, suggestions: [] };
    }

    const cleanText = text.slice(0, idx).trimEnd();
    const suggestionsBlock = text.slice(idx + marker.length);

    const suggestions = suggestionsBlock
        .split('\n')
        .map(line => line.replace(/^[-•*]\s*/, '').trim())
        .filter(line => line.length > 0 && line.length <= 50);

    return { cleanText, suggestions: suggestions.slice(0, 3) };
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
    const [streamingText, setStreamingText] = useState('');
    const [dynamicSuggestions, setDynamicSuggestions] = useState([]);
    const bottomRef = useRef(null);
    const timerRef = useRef(null);

    // ── Estado de cuota ──
    const [quota, setQuota] = useState(null);

    // ── Cargar cuota al montar el componente ──
    useEffect(() => {
        fetchAiQuota()
            .then(setQuota)
            .catch(() => { });
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
    }, [messages, loading, streamingText]);

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
        setDynamicSuggestions([]);
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

    // ── Enviar sugerencia como mensaje ──
    const handleSuggestionClick = useCallback((text) => {
        setInput(text);
        // Disparar envío automáticamente en el siguiente tick
        setTimeout(() => {
            const form = document.querySelector(`.${s.form}`);
            if (form) form.requestSubmit();
        }, 0);
    }, []);

    // ── Enviar mensaje (con streaming) ──
    async function handleSend(e) {
        e.preventDefault();
        const text = input.trim();
        if (!text || loading) return;

        const userMsg = { role: 'user', content: text };
        setMessages(prev => [...prev, userMsg]);
        setInput('');
        setError(null);
        setElapsed(0);
        setStreamingText('');
        setDynamicSuggestions([]);
        setLoading(true);

        const start = Date.now();
        let fullText = '';

        try {
            const quotaData = await streamAiMessage(
                {
                    section,
                    message: text,
                    history: messages,
                },
                (chunk) => {
                    fullText += chunk;
                    setStreamingText(fullText);
                }
            );

            const duration = Date.now() - start;

            // Extraer sugerencias del texto completo
            const { cleanText, suggestions } = parseSuggestions(fullText);

            setMessages(prev => [
                ...prev,
                { role: 'assistant', content: cleanText, time: duration },
            ]);
            setStreamingText('');
            setDynamicSuggestions(suggestions);

            // Actualizar cuota
            if (quotaData?.queriesUsed != null) {
                setQuota({
                    queriesUsed: quotaData.queriesUsed,
                    queriesLimit: quotaData.queriesLimit,
                    plan: quotaData.plan,
                });
            }
        } catch (err) {
            if (err.status === 429) {
                setError('Has alcanzado tu límite de consultas este mes. Actualiza a Premium para consultas ilimitadas.');
                if (quota) {
                    setQuota({ ...quota, queriesUsed: quota.queriesLimit });
                }
            } else {
                setError(err.message);
            }
            setStreamingText('');
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
                    {messages.length === 0 && !loading && (
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

                    {/* Mensajes guardados */}
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

                    {/* Streaming: texto parcial en tiempo real */}
                    {loading && streamingText && (
                        <div className={`${s.bubble} ${s.assistant} ${s.streaming}`}>
                            <ReactMarkdown>{parseSuggestions(streamingText).cleanText}</ReactMarkdown>
                            <span className={s.liveTimer}>
                                ⚡ {formatTime(elapsed)}
                            </span>
                        </div>
                    )}

                    {/* Loading sin texto todavía (esperando primer chunk) */}
                    {loading && !streamingText && (
                        <div className={`${s.bubble} ${s.assistant}`}>
                            <span className={s.dots}>···</span>
                            <span className={s.liveTimer}>
                                {formatTime(elapsed)}
                            </span>
                        </div>
                    )}

                    {/* Chips de sugerencias dinámicas (post-respuesta) */}
                    {!loading && dynamicSuggestions.length > 0 && (
                        <div className={s.suggestionsRow}>
                            {dynamicSuggestions.map(text => (
                                <button
                                    key={text}
                                    className={s.suggestionChip}
                                    onClick={() => handleSuggestionClick(text)}
                                    disabled={isQuotaExhausted()}
                                >
                                    + {text}
                                </button>
                            ))}
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