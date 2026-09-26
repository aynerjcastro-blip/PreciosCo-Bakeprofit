/*
 * theme.js
 *
 * Maneja el cambio manual entre modo claro y oscuro.
 * Este archivo NO decide los colores (eso vive en tokens.css),
 * solo cambia el atributo data-theme en <html>, que es lo que
 * el CSS usa para saber que variables aplicar.
 *
 * Conceptos usados:
 * - document.documentElement: es el elemento <html> completo,
 *   el lugar correcto para poner el atributo data-theme porque
 *   CSS puede verlo desde cualquier parte de la pagina.
 * - localStorage: guarda la eleccion del usuario para que se
 *   recuerde la proxima vez que visite el sitio.
 */

const STORAGE_KEY = "preciosco_theme";

/**
 * Aplica un tema especifico, cambiando el atributo en <html>
 * y guardando la eleccion para la proxima visita.
 * @param {"light"|"dark"} theme
 */
export function applyTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    localStorage.setItem(STORAGE_KEY, theme);
    updateToggleIcon(theme);
}

/**
 * Cambia entre claro y oscuro segun el tema actual.
 * Esta es la funcion que conecta con el click del boton.
 */
export function toggleTheme() {
    const current = document.documentElement.getAttribute("data-theme");
    const next = current === "dark" ? "light" : "dark";
    applyTheme(next);
}

/**
 * Actualiza el texto/icono del boton segun el tema activo,
 * para que el usuario sepa que accion va a pasar si hace click
 * (ej. mostrar el icono de sol si esta en oscuro, invitando a
 * volver a claro).
 * @param {"light"|"dark"} theme
 */
function updateToggleIcon(theme) {
    const button = document.querySelector("#theme-toggle");
    if (!button) {
        return;
    }
    button.textContent = theme === "dark" ? "Modo claro" : "Modo oscuro";
}

/**
 * Se ejecuta una sola vez al cargar cada pagina. Revisa si el
 * usuario ya habia elegido un tema antes (localStorage); si no,
 * no hace nada y deja que el @media prefers-color-scheme de
 * tokens.css decida automaticamente segun el sistema operativo.
 */
export function initTheme() {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
        document.documentElement.setAttribute("data-theme", saved);
    }
    updateToggleIcon(saved || (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light"));

    const button = document.querySelector("#theme-toggle");
    if (button) {
        button.addEventListener("click", toggleTheme);
    }
}