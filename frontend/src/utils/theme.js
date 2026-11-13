/*
 * theme.js
 *
 * Maneja el cambio manual entre modo claro y oscuro.
 * Este archivo NO decide los colores (eso vive en tokens.css): solo cambia el atributo
 * data-theme en <html>, que es lo que el CSS usa para saber qué variables aplicar, y avisa
 * a quien escuche (el botón del header) para que muestre el tema activo.
 *
 * La elección se guarda en localStorage y index.html la restaura antes de pintar la página,
 * para que no haya un destello del tema equivocado. Sin elección guardada, el tema sigue al
 * sistema operativo (@media prefers-color-scheme de tokens.css).
 */
import { flushSync } from 'react-dom';
import '../styles/theme-transition.css';

const STORAGE_KEY = 'preciosco_theme';
const listeners = new Set();

const prefersDark = () => window.matchMedia('(prefers-color-scheme: dark)');
const prefersReducedMotion = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/**
 * Tema que se está viendo ahora: el elegido por la persona o, si no eligió, el del sistema.
 * @returns {"light"|"dark"}
 */
export function getTheme() {
    const chosen = document.documentElement.getAttribute('data-theme');
    if (chosen === 'light' || chosen === 'dark') return chosen;
    return prefersDark().matches ? 'dark' : 'light';
}

/**
 * Para useSyncExternalStore: avisa cuando cambia el tema, ya sea por el botón o porque el
 * sistema operativo cambió de modo mientras no había elección guardada.
 * @param {() => void} listener
 * @returns {() => void} función que cancela la suscripción
 */
export function subscribeTheme(listener) {
    listeners.add(listener);
    const media = prefersDark();
    media.addEventListener('change', listener);
    return () => {
        listeners.delete(listener);
        media.removeEventListener('change', listener);
    };
}

/**
 * Aplica un tema, lo guarda para la próxima visita y avisa a los suscriptores.
 * @param {"light"|"dark"} theme
 */
export function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    try {
        localStorage.setItem(STORAGE_KEY, theme);
    } catch {
        // Sin almacenamiento (modo privado, por ejemplo): el cambio vale solo para esta visita
    }
    listeners.forEach((listener) => listener());
}

// Sin View Transitions: mezcla los colores unos instantes en lugar de cambiarlos de golpe
function applyWithFade(theme) {
    const root = document.documentElement;
    root.classList.add('theme-changing');
    applyTheme(theme);
    setTimeout(() => root.classList.remove('theme-changing'), 350);
}

/**
 * Cambia entre claro y oscuro. Si el navegador lo permite, la pantalla nueva se revela como un
 * círculo que crece desde `origin` (el botón); si no, los colores se mezclan; y con "reducir
 * movimiento" el cambio es inmediato.
 * @param {{x: number, y: number}} [origin] punto de la pantalla desde donde crece el círculo
 */
export function toggleTheme(origin) {
    const next = getTheme() === 'dark' ? 'light' : 'dark';

    if (prefersReducedMotion()) {
        applyTheme(next);
        return;
    }
    if (!document.startViewTransition) {
        applyWithFade(next);
        return;
    }

    const x = origin?.x ?? window.innerWidth / 2;
    const y = origin?.y ?? 0;
    const radius = Math.hypot(
        Math.max(x, window.innerWidth - x),
        Math.max(y, window.innerHeight - y),
    );

    // flushSync: React debe pintar el tema nuevo dentro de la transición, o la foto de "después"
    // saldría con el botón todavía en el estado anterior
    const transition = document.startViewTransition(() => {
        flushSync(() => applyTheme(next));
    });

    transition.ready
        .then(() => {
            document.documentElement.animate(
                { clipPath: [`circle(0px at ${x}px ${y}px)`, `circle(${radius}px at ${x}px ${y}px)`] },
                { duration: 550, easing: 'ease-in-out', pseudoElement: '::view-transition-new(root)' },
            );
        })
        .catch(() => {
            // Transición cancelada (por ejemplo, otro clic seguido): el tema ya quedó aplicado
        });
}