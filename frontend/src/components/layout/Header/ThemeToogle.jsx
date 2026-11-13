import { toggleTheme } from '@/utils/theme';
import { useTheme } from '@/hooks/useTheme';
import styles from '@/components/layout/Header/ThemeToogle.module.css';

function SunIcon() {
    return (
        <svg className={`${styles['theme-toggle__icon']} ${styles['theme-toggle__icon--sun']}`}
            width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <circle cx="12" cy="12" r="4" />
            <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
        </svg>
    );
}

function MoonIcon() {
    return (
        <svg className={`${styles['theme-toggle__icon']} ${styles['theme-toggle__icon--moon']}`}
            width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
        </svg>
    );
}

/**
 * Interruptor de modo oscuro. La perilla se desliza y el sol y la luna se cambian con un giro;
 * al pulsarlo, toda la página cambia de tema con un círculo que crece desde el botón.
 */
export default function ThemeToggle() {
    const theme = useTheme();
    const isDark = theme === 'dark';

    const handleClick = (event) => {
        // El círculo del cambio de tema nace en el centro del botón
        const rect = event.currentTarget.getBoundingClientRect();
        toggleTheme({ x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 });
    };

    return (
        <button
            type="button"
            role="switch"
            aria-checked={isDark}
            aria-label="Modo oscuro"
            className={`${styles['theme-toggle']} ${isDark ? styles['theme-toggle--dark'] : ''}`}
            onClick={handleClick}
        >
            <span className={styles['theme-toggle__label']}>Modo</span>
            <span className={styles['theme-toggle__track']}>
                <span className={styles['theme-toggle__thumb']}>
                    <SunIcon />
                    <MoonIcon />
                </span>
            </span>
        </button>
    );
}