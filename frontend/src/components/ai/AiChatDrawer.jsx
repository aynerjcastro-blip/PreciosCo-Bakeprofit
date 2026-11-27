import { useState, useRef, useEffect, useCallback } from 'react';
import ReactMarkdown from 'react-markdown';
import { streamAiMessage, streamAiRecipe, fetchAiQuota } from '../../api/ai';
import s from '@/components/ai/AiChatDrawer.module.css';
import {
    SuperAvatar,
    DrogueriaAvatar,
    CuidadoAvatar,
} from '@/components/ui/icons/AiAvatars';
import {
    AlertIcon,
    ChatIcon,
    CloseIcon,
    InfinityIcon,
    PlusIcon,
    RecipeIcon,
    ShieldIcon,
    TimerIcon,
    TrashIcon,
    TypingIndicator,
} from '@/components/ui/icons/ChatIcons';
import RecipeCard from '@/components/ai/RecipeCard';

/** Turnos de historial que se envían al backend (máx. permitido: 20). */
const MAX_HISTORY_TURNS = 10;

/** Duración de la animación de cierre (debe coincidir con el CSS). */
const CLOSE_ANIMATION_MS = 260;

/** Los enlaces de la IA (páginas del producto en cada tienda) abren en otra pestaña. */
const MD_COMPONENTS = {
    // eslint-disable-next-line no-unused-vars
    a: ({ node, ...props }) => <a {...props} target="_blank" rel="noopener noreferrer" />,
};

/* ───────────────────────────────────────────
   Configuración por sección
   ─────────────────────────────────────────── */

const SECTION_CONFIG = {
    supermercados: { label: 'CanastaBot', sub: 'Supermercados', Icon: SuperAvatar, color: '#E8740C', recipes: true },
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

/** Chips del modo receta (SCRUM-325) */
const RECIPE_SUGGESTIONS = [
    'Ajiaco para 4 personas',
    'Arroz con pollo',
    'Lentejas con chorizo',
];

/** Formatea milisegundos a "0.0s" */
function formatTime(ms) {
    return (ms / 1000).toFixed(1) + 's';
}

/** Quita emojis del texto de la IA: la interfaz usa solo iconos SVG. */
function stripEmojis(text) {
    return text.replace(/\p{Extended_Pictographic}️?/gu, '').replace(/[ \t]{2,}/g, ' ');
}

/**
 * Extrae el bloque [SUGERENCIAS] del texto de respuesta de la IA.
 * Retorna { cleanText, suggestions: string[] }
 */
function parseSuggestions(rawText) {
    const text = stripEmojis(rawText);
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

/** Quita del texto el aviso que agregó el backend (se muestra aparte como bloque fijo). */
function removeDisclaimer(text, disclaimer) {
    const idx = text.lastIndexOf(disclaimer);
    return idx === -1 ? text : (text.slice(0, idx) + text.slice(idx + disclaimer.length)).trimEnd();
}

/** ¿El usuario prefiere menos movimiento? (accesibilidad) */
function prefersReducedMotion() {
    return typeof window !== 'undefined'
        && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
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
    /** 'chat' = consulta de precios · 'recipe' = receta (solo supermercados) */
    const [mode, setMode] = useState('chat');
    const [closing, setClosing] = useState(false);
    const bottomRef = useRef(null);
    const timerRef = useRef(null);
    const formRef = useRef(null);
    const inputRef = useRef(null);
    const closeTimerRef = useRef(null);

    const recipeMode = cfg.recipes && mode === 'recipe';

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

    // ── Cierre animado: primero la animación de salida, luego se desmonta ──
    const requestClose = useCallback(() => {
        if (closeTimerRef.current) return;
        if (prefersReducedMotion()) {
            onClose();
            return;
        }
        setClosing(true);
        closeTimerRef.current = setTimeout(onClose, CLOSE_ANIMATION_MS);
    }, [onClose]);

    useEffect(() => () => clearTimeout(closeTimerRef.current), []);

    // ── Escape cierra el chat ──
    useEffect(() => {
        function onKey(e) {
            if (e.key === 'Escape') requestClose();
        }
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [requestClose]);

    // ── Limpiar historial manualmente ──
    function handleClear() {
        setMessages([]);
        setDynamicSuggestions([]);
        setError(null);
        sessionStorage.removeItem(storageKey);
    }

    // ── Cuota del modo activo ──
    function activeQuota() {
        if (!quota) return null;
        return recipeMode
            ? { used: quota.recipesUsed, limit: quota.recipesLimit, noun: 'recetas' }
            : { used: quota.queriesUsed, limit: quota.queriesLimit, noun: 'consultas' };
    }

    // ── ¿Cuota agotada (del modo activo)? ──
    function isQuotaExhausted() {
        const q = activeQuota();
        if (!q || quota.plan === 'PREMIUM' || q.limit == null) return false;
        return q.used >= q.limit;
    }

    function switchMode(next) {
        if (loading || next === mode) return;
        setMode(next);
        setError(null);
        setDynamicSuggestions([]);
        inputRef.current?.focus();
    }

    // ── Enviar sugerencia como mensaje ──
    const handleSuggestionClick = useCallback((text) => {
        setInput(text);
        // Disparar envío automáticamente en el siguiente tick
        setTimeout(() => formRef.current?.requestSubmit(), 0);
    }, []);

    // ── Enviar mensaje (con streaming) ──
    async function handleSend(e) {
        e.preventDefault();
        const text = input.trim();
        if (!text || loading) return;

        const asRecipe = recipeMode;
        const userMsg = { role: 'user', content: text, recipe: asRecipe || undefined };
        setMessages(prev => [...prev, userMsg]);
        setInput('');
        setError(null);
        setElapsed(0);
        setStreamingText('');
        setDynamicSuggestions([]);
        setLoading(true);

        const start = Date.now();
        let fullText = '';

        const payload = {
            section,
            message: text,
            // Solo los últimos turnos (el backend acepta máx. 20) y sin campos extra
            history: messages
                .filter(m => (m.role === 'user' || m.role === 'assistant') && m.content?.trim())
                .slice(-MAX_HISTORY_TURNS)
                .map(({ role, content }) => ({ role, content })),
        };
        const onChunk = (chunk) => {
            fullText += chunk;
            setStreamingText(fullText);
        };

        try {
            const done = asRecipe
                ? await streamAiRecipe(payload, onChunk)
                : await streamAiMessage(payload, onChunk);

            const duration = Date.now() - start;

            // Droguería (RN-33): el backend agrega el aviso al final del texto; se saca del
            // texto y se muestra como bloque fijo para que no se pierda entre las sugerencias.
            const disclaimer = done?.disclaimer || undefined;
            const text = disclaimer ? removeDisclaimer(fullText, disclaimer) : fullText;

            // Extraer sugerencias del texto completo
            const { cleanText, suggestions } = parseSuggestions(text);

            setMessages(prev => [
                ...prev,
                {
                    role: 'assistant',
                    content: cleanText,
                    time: duration,
                    // Disponibilidad de ingredientes resuelta contra el catálogo
                    recipe: asRecipe && done?.ingredients ? done : undefined,
                    disclaimer,
                },
            ]);
            setStreamingText('');
            setDynamicSuggestions(suggestions);

            // Actualizar cuota (se conservan los campos del otro modo)
            if (asRecipe && done?.recipesUsed != null) {
                setQuota(prev => ({ ...prev, recipesUsed: done.recipesUsed, recipesLimit: done.recipesLimit, plan: done.plan }));
            } else if (!asRecipe && done?.queriesUsed != null) {
                setQuota(prev => ({ ...prev, queriesUsed: done.queriesUsed, queriesLimit: done.queriesLimit, plan: done.plan }));
            }
        } catch (err) {
            if (err.status === 429) {
                setError(err.message || (asRecipe
                    ? 'Has alcanzado tu límite de recetas este mes.'
                    : 'Has alcanzado tu límite de consultas este mes. Actualiza a Premium para consultas ilimitadas.'));
                setQuota(prev => prev && (asRecipe
                    ? { ...prev, recipesUsed: prev.recipesLimit }
                    : { ...prev, queriesUsed: prev.queriesLimit }));
            } else {
                setError(err.message);
            }
            setStreamingText('');
        } finally {
            setLoading(false);
        }
    }

    // ── Badge de cuota del modo activo ──
    function renderQuotaBadge() {
        const q = activeQuota();
        if (!q) return null;
        if (quota.plan === 'PREMIUM') {
            return (
                <span className={s.quotaBadge} title="Plan Premium: ilimitado">
                    <InfinityIcon /> Premium
                </span>
            );
        }
        if (q.limit == null) return null;
        const remaining = Math.max(0, q.limit - q.used);
        return (
            <span
                className={`${s.quotaBadge} ${isQuotaExhausted() ? s.quotaExhausted : ''}`}
                title={`${q.noun[0].toUpperCase()}${q.noun.slice(1)} restantes este mes`}
            >
                {remaining}/{q.limit} {q.noun}
            </span>
        );
    }

    const welcomeChips = recipeMode ? RECIPE_SUGGESTIONS : (SUGGESTIONS[section] || []);

    // ── Render ──
    return (
        <div
            className={`${s.overlay} ${closing ? s.overlayClosing : ''}`}
            onClick={requestClose}
        >
            <aside
                className={`${s.drawer} ${closing ? s.drawerClosing : ''}`}
                onClick={e => e.stopPropagation()}
                role="dialog"
                aria-modal="true"
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
                        {renderQuotaBadge()}
                        <button className={s.iconBtn} onClick={handleClear}
                            title="Limpiar chat" aria-label="Limpiar chat">
                            <TrashIcon size={16} />
                        </button>
                        <button className={s.iconBtn} onClick={requestClose}
                            title="Cerrar" aria-label="Cerrar chat">
                            <CloseIcon size={16} />
                        </button>
                    </div>
                </header>

                {/* ── Selector de modo (solo supermercados) ── */}
                {cfg.recipes && (
                    <div className={s.modeBar} role="tablist" aria-label="Tipo de consulta">
                        <button
                            role="tab"
                            aria-selected={!recipeMode}
                            className={`${s.modeTab} ${!recipeMode ? s.modeTabActive : ''}`}
                            onClick={() => switchMode('chat')}
                            disabled={loading}
                        >
                            <ChatIcon size={16} /> Precios
                        </button>
                        <button
                            role="tab"
                            aria-selected={recipeMode}
                            className={`${s.modeTab} ${recipeMode ? s.modeTabActive : ''}`}
                            onClick={() => switchMode('recipe')}
                            disabled={loading}
                        >
                            <RecipeIcon size={16} /> Receta
                        </button>
                        <span
                            className={`${s.modeIndicator} ${recipeMode ? s.modeIndicatorRight : ''}`}
                            aria-hidden="true"
                        />
                    </div>
                )}

                {/* ── Cuerpo ── */}
                <div className={s.body}>

                    {/* Welcome + chips */}
                    {messages.length === 0 && !loading && (
                        <div className={s.welcome} key={mode}>
                            {recipeMode ? <span className={s.welcomeIcon}><RecipeIcon size={40} /></span> : <cfg.Icon />}
                            <p className={s.welcomeText}>
                                {recipeMode ? (
                                    <>Dime qué plato quieres preparar y te doy la receta con los
                                        ingredientes que encuentras en el catálogo.</>
                                ) : (
                                    <>¡Hola! Soy <strong>{cfg.label}</strong>, tu asistente
                                        de {cfg.sub.toLowerCase()}. ¿Qué producto necesitas cotizar?</>
                                )}
                            </p>
                            <div className={s.chips}>
                                {welcomeChips.map(text => (
                                    <button
                                        key={text}
                                        className={s.chip}
                                        onClick={() => setInput(text)}
                                    >
                                        {text}
                                    </button>
                                ))}
                            </div>
                            {cfg.recipes && !recipeMode && (
                                <button className={s.recipeCta} onClick={() => switchMode('recipe')}>
                                    <RecipeIcon size={16} /> Pedir una receta
                                </button>
                            )}
                        </div>
                    )}

                    {/* Mensajes guardados */}
                    {messages.map((msg, i) => (
                        <div key={i} className={`${s.bubble} ${msg.role === 'user' ? s.user : s.assistant}`}>
                            {msg.role === 'user' ? (
                                <>
                                    {msg.recipe && (
                                        <span className={s.userTag}><RecipeIcon size={12} /> Receta</span>
                                    )}
                                    {msg.content}
                                </>
                            ) : (
                                <>
                                    <ReactMarkdown components={MD_COMPONENTS}>{msg.content}</ReactMarkdown>
                                    {msg.recipe && <RecipeCard recipe={msg.recipe} />}
                                    {msg.disclaimer && (
                                        <p className={s.disclaimer} role="note">
                                            <ShieldIcon /> {msg.disclaimer}
                                        </p>
                                    )}
                                </>
                            )}
                            {msg.role === 'assistant' && msg.time != null && (
                                <span className={s.responseTime}>
                                    <TimerIcon /> {formatTime(msg.time)}
                                </span>
                            )}
                        </div>
                    ))}

                    {/* Streaming: texto parcial en tiempo real */}
                    {loading && streamingText && (
                        <div className={`${s.bubble} ${s.assistant} ${s.streaming}`}>
                            <ReactMarkdown components={MD_COMPONENTS}>{parseSuggestions(streamingText).cleanText}</ReactMarkdown>
                            <span className={s.liveTimer}>
                                <TimerIcon /> {formatTime(elapsed)}
                            </span>
                        </div>
                    )}

                    {/* Loading sin texto todavía (esperando primer chunk) */}
                    {loading && !streamingText && (
                        <div className={`${s.bubble} ${s.assistant} ${s.loadingBubble}`} aria-live="polite">
                            <TypingIndicator className={s.typing} />
                            <span className={s.loadingLabel}>
                                {recipeMode ? 'Buscando ingredientes en el catálogo' : 'Pensando'}
                            </span>
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
                                    <PlusIcon /> {text}
                                </button>
                            ))}
                        </div>
                    )}

                    {error && (
                        <p className={s.error} role="alert">
                            <AlertIcon /> {error}
                        </p>
                    )}
                    <div ref={bottomRef} />
                </div>

                {/* ── Input ── */}
                <form className={s.form} onSubmit={handleSend} ref={formRef}>
                    <input
                        ref={inputRef}
                        className={s.input}
                        type="text"
                        value={input}
                        onChange={e => setInput(e.target.value)}
                        placeholder={isQuotaExhausted()
                            ? `Límite de ${activeQuota()?.noun ?? 'consultas'} alcanzado`
                            : recipeMode
                                ? '¿Qué plato quieres preparar?'
                                : `Pregúntale a ${cfg.label}...`}
                        disabled={loading || isQuotaExhausted()}
                        maxLength={500}
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
