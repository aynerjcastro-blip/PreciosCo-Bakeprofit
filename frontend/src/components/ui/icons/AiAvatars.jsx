const common = {
    width: 40,
    height: 40,
    viewBox: '0 0 64 64',
    fill: 'none',
    'aria-hidden': true,
    focusable: false,
};

const stroke = {
    stroke: 'currentColor',
    strokeWidth: 3.5,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
};

// Mismo carrito de Sectoricons pero como avatar circular
export function SuperAvatar() {
    return (
        <svg {...common}>
            <circle cx="32" cy="32" r="30" fill="var(--color-primary)" opacity="0.12" />
            <path d="M18 20h5l3 6" {...stroke} stroke="var(--color-primary)" />
            <path d="M24 26h22l-3 12H28z" {...stroke} />
            <circle cx="30" cy="46" r="3" fill="var(--color-primary)" />
            <circle cx="40" cy="46" r="3" fill="var(--color-primary)" />
        </svg>
    );
}

export function DrogueriaAvatar() {
    return (
        <svg {...common}>
            <circle cx="32" cy="32" r="30" fill="var(--color-primary)" opacity="0.12" />
            <g transform="translate(8,8) scale(0.75)">
                <g transform="rotate(-40 28 32)">
                    <rect x="8" y="20" width="40" height="22" rx="11" {...stroke} />
                    <path d="M28 20h9a11 11 0 0 1 0 22h-9z" fill="var(--color-primary)" stroke="currentColor" strokeWidth="3.5" strokeLinejoin="round" />
                </g>
            </g>
        </svg>
    );
}

export function CuidadoAvatar() {
    return (
        <svg {...common}>
            <circle cx="32" cy="32" r="30" fill="var(--color-primary)" opacity="0.12" />
            <g transform="translate(10,10) scale(0.7)">
                <circle cx="28" cy="13" r="6" fill="currentColor" />
                <path d="M16 30c0-9 5-16 12-16s12 7 12 16c0 10-5 18-12 18s-12-8-12-18z" fill="currentColor" opacity="0.92" />
                <path d="M50 20l2 5 5 2-5 2-2 5-2-5-5-2 5-2z" fill="var(--color-primary)" />
            </g>
        </svg>
    );
}