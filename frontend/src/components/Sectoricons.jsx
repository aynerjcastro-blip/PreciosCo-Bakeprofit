// Iconos SVG de los sectores. Usan currentColor para el trazo oscuro
// (se adapta al modo oscuro) y --color-primary para los acentos naranja.

const common = {
    width: 64,
    height: 64,
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

export function CartIcon() {
    return (
        <svg {...common}>
            <path d="M6 8h7l3 6" {...stroke} stroke="var(--color-primary)" />
            <path d="M14 14h40l-4 20H20z" {...stroke} />
            <path d="M22 22h24M24 28h20" {...stroke} />
            <circle cx="24" cy="50" r="4.5" fill="var(--color-primary)" />
            <circle cx="44" cy="50" r="4.5" fill="var(--color-primary)" />
        </svg>
    );
}

export function PillIcon() {
    return (
        <svg {...common}>
            <g transform="rotate(-40 28 32)">
                <rect x="8" y="20" width="40" height="22" rx="11" {...stroke} />
                <path d="M28 20h9a11 11 0 0 1 0 22h-9z" fill="var(--color-primary)" stroke="currentColor" strokeWidth="3.5" strokeLinejoin="round" />
            </g>
            <circle cx="46" cy="44" r="11" fill="var(--color-primary)" />
            <path d="M46 38v12M40 44h12" stroke="#fff" strokeWidth="3.5" strokeLinecap="round" />
        </svg>
    );
}

export function FaceIcon() {
    return (
        <svg {...common}>
            <circle cx="28" cy="13" r="6" fill="currentColor" />
            <path d="M16 30c0-9 5-16 12-16s12 7 12 16c0 10-5 18-12 18s-12-8-12-18z" fill="currentColor" opacity="0.92" />
            <path d="M20 31c0-5 3-9 8-9s8 4 8 9c0 7-3 13-8 13s-8-6-8-13z" fill="var(--bg-surface)" />
            <path d="M23 32q2.5 2 5 0M30 32q2.5 2 5 0" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
            <path d="M26 39q2 1.5 4 0" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
            <path d="M50 20l2 5 5 2-5 2-2 5-2-5-5-2 5-2z" fill="var(--color-primary)" />
            <path d="M46 40l1.5 3.5 3.5 1.5-3.5 1.5L46 50l-1.5-3.5L41 45l3.5-1.5z" fill="var(--color-primary)" />
        </svg>
    );
}

export function GearIcon() {
    return (
        <svg {...common}>
            <circle cx="28" cy="34" r="8" {...stroke} />
            <path
                d="M28 12v6M28 50v6M10 34h6M40 34h6M15.3 21.3l4.2 4.2M36.5 42.5l4.2 4.2M15.3 46.7l4.2-4.2M36.5 25.5l4.2-4.2"
                {...stroke}
            />
            <path d="M44 14l10 10-4 4-10-10z" fill="var(--color-primary)" />
            <path d="M50 28L38 40" stroke="var(--color-primary)" strokeWidth="5" strokeLinecap="round" />
        </svg>
    );
}

export function SearchIcon() {
    return (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true" focusable="false">
            <circle cx="11" cy="11" r="6.5" stroke="currentColor" strokeWidth="2.2" />
            <path d="M16 16l4.5 4.5" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
        </svg>
    );
}

export function EyeIcon() {
    return (
        <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true" focusable="false">
            <circle cx="12" cy="12" r="11" fill="#fff" />
            <path d="M4.5 12S7.5 6.8 12 6.8 19.5 12 19.5 12 16.5 17.2 12 17.2 4.5 12 4.5 12z" stroke="var(--color-primary)" strokeWidth="1.8" strokeLinejoin="round" />
            <circle cx="12" cy="12" r="2.4" fill="var(--color-primary)" />
        </svg>
    );
}