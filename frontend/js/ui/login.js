/*
 * login.js (capa ui)
 *
 * Este archivo SOLO se encarga de manipular el DOM de login.html:
 * leer el formulario, mostrar errores, redirigir al usuario.
 * La comunicacion real con el backend vive en js/api/auth.js.
 *
 * Debe cargarse en login.html asi:
 *   <script type="module" src="/frontend/js/ui/login.js"></script>
 */

import { login } from "../api/auth.js";
import { saveSession, isLoggedIn } from "../utils/auth-helper.js";

const form = document.querySelector(".auth-form");
const submitButton = form.querySelector(".auth-form__submit");

// Si el usuario ya tiene sesion activa, no tiene sentido mostrarle
// el formulario de login de nuevo.
if (isLoggedIn()) {
    window.location.href = "/frontend/index.html";
}

/**
 * Muestra un mensaje de error debajo del formulario.
 * Si ya existe un mensaje de un intento anterior, lo reemplaza
 * en vez de apilar varios mensajes.
 * @param {string} message
 */
function showError(message) {
    let errorBox = form.querySelector(".auth-form__error");

    if (!errorBox) {
        errorBox = document.createElement("p");
        errorBox.className = "auth-form__error";
        form.appendChild(errorBox);
    }

    errorBox.textContent = message;
}

/**
 * Quita el mensaje de error, si existe, antes de un nuevo intento.
 */
function clearError() {
    const errorBox = form.querySelector(".auth-form__error");
    if (errorBox) {
        errorBox.remove();
    }
}

form.addEventListener("submit", async (event) => {
    // Evita que el navegador recargue la pagina al enviar el formulario,
    // que es el comportamiento por defecto de un <form>.
    event.preventDefault();

    clearError();

    const email = form.querySelector("#login-email").value.trim();
    const password = form.querySelector("#login-password").value;

    // Deshabilita el boton mientras se procesa, para evitar doble envio
    // si el usuario hace varios clicks seguidos.
    submitButton.disabled = true;
    submitButton.textContent = "Ingresando...";

    try {
        const authResponse = await login(email, password);

        saveSession(authResponse.token, {
            name: authResponse.name,
            email: authResponse.email,
            role: authResponse.role,
        });

        window.location.href = "/frontend/index.html";
    } catch (error) {
        showError(error.message);
    } finally {
        submitButton.disabled = false;
        submitButton.textContent = "Iniciar sesion";
    }
});