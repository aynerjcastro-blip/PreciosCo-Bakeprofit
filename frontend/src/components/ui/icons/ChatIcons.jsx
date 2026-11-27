/**
 * Iconos SVG del asistente de IA (reemplazan los emojis de la interfaz).
 * Usan currentColor: toman el color del texto donde se ponen.
 */

const base = {
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 2,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
    'aria-hidden': true,
    focusable: false,
};

function Svg({ size = 18, children, ...rest }) {
    return (
        <svg width={size} height={size} {...base} {...rest}>
            {children}
        </svg>
    );
}

export function TrashIcon(props) {
    return (
        <Svg {...props}>
            <path d="M3 6h18" />
            <path d="M8 6V4h8v2" />
            <path d="M19 6l-1 14H6L5 6" />
            <path d="M10 11v6M14 11v6" />
        </Svg>
    );
}

export function CloseIcon(props) {
    return (
        <Svg {...props}>
            <path d="M18 6L6 18M6 6l12 12" />
        </Svg>
    );
}

/** Cronómetro: tiempo de respuesta. */
export function TimerIcon(props) {
    return (
        <Svg size={12} {...props}>
            <circle cx="12" cy="13" r="8" />
            <path d="M12 9v4l2 2" />
            <path d="M10 2h4" />
        </Svg>
    );
}

export function AlertIcon(props) {
    return (
        <Svg size={16} {...props}>
            <circle cx="12" cy="12" r="10" />
            <path d="M12 8v4M12 16h.01" />
        </Svg>
    );
}

/** Olla: modo receta. */
export function RecipeIcon(props) {
    return (
        <Svg {...props}>
            <path d="M4 11h16v3a6 6 0 0 1-6 6h-4a6 6 0 0 1-6-6z" />
            <path d="M2 11h2M20 11h2" />
            <path d="M9 7c0-1 1-1.5 1-2.5M13 7c0-1 1-1.5 1-2.5" />
        </Svg>
    );
}

/** Burbuja de chat: modo conversación y botón flotante. */
export function ChatIcon(props) {
    return (
        <Svg {...props}>
            <path d="M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z" />
            <path d="M8.5 12h.01M12 12h.01M15.5 12h.01" strokeWidth={2.6} />
        </Svg>
    );
}

export function CheckIcon(props) {
    return (
        <Svg size={16} {...props}>
            <circle cx="12" cy="12" r="10" />
            <path d="M8 12.5l2.5 2.5L16 9.5" />
        </Svg>
    );
}

export function MissingIcon(props) {
    return (
        <Svg size={16} {...props}>
            <circle cx="12" cy="12" r="10" />
            <path d="M15 9l-6 6M9 9l6 6" />
        </Svg>
    );
}

export function ExternalIcon(props) {
    return (
        <Svg size={12} {...props}>
            <path d="M14 4h6v6" />
            <path d="M20 4l-9 9" />
            <path d="M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5" />
        </Svg>
    );
}

/** Escudo con cruz: aviso de salud (droguería, RN-33). */
export function ShieldIcon(props) {
    return (
        <Svg size={16} {...props}>
            <path d="M12 3l7 3v5c0 4.5-3 8.3-7 10-4-1.7-7-5.5-7-10V6z" />
            <path d="M12 9v6M9 12h6" />
        </Svg>
    );
}

export function InfinityIcon(props) {
    return (
        <Svg size={14} {...props}>
            <path d="M18.2 8.5c-2 0-3.5 1.6-6.2 3.5-2.7 1.9-4.2 3.5-6.2 3.5a3.5 3.5 0 0 1 0-7c2 0 3.5 1.6 6.2 3.5 2.7 1.9 4.2 3.5 6.2 3.5a3.5 3.5 0 0 0 0-7z" />
        </Svg>
    );
}

export function PlusIcon(props) {
    return (
        <Svg size={12} {...props}>
            <path d="M12 5v14M5 12h14" />
        </Svg>
    );
}

/**
 * Indicador "escribiendo": tres puntos que rebotan (la animación está en
 * el CSS del chat, clase .typing).
 */
export function TypingIndicator({ className }) {
    return (
        <svg className={className} width="34" height="12" viewBox="0 0 34 12" aria-hidden="true" focusable="false">
            <circle cx="5" cy="6" r="4" fill="currentColor" />
            <circle cx="17" cy="6" r="4" fill="currentColor" />
            <circle cx="29" cy="6" r="4" fill="currentColor" />
        </svg>
    );
}
