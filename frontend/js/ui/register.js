/*
 * registro.js (capa ui)
 *
 * Este archivo SOLO se encarga de manipular el DOM de registro.html.
 * La comunicacion real con el backend vive en js/api/auth.js.
 *
 * Debe cargarse en registro.html asi:
 *   <script type="module" src="/frontend/js/ui/registro.js"></script>
 */

import { register } from "../api/auth.js";
import { saveSession, isLoggedIn } from "../utils/auth-helper.js";

const form = document.querySelector(".auth-form");
const submitButton = form.querySelector(".auth-form__submit");

if (isLoggedIn()) {
    window.location.href = "/frontend/index.html";
}

/**
 * Muestra uno o varios mensajes de error debajo del formulario.
 * El backend puede devolver varios errores de validacion a la vez
 * (ej. nombre vacio y email invalido juntos), separados por coma,
 * asi que se muestran todos, no solo el primero.
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

function clearError() {
    const errorBox = form.querySelector(".auth-form__error");
    if (errorBox) {
        errorBox.remove();
    }
}

/**
 * Validacion basica en el cliente, antes de llamar al backend.
 * No reemplaza la validacion del backend (@Valid + DTO), solo evita
 * una peticion innecesaria si algo obvio esta mal.
 * @returns {string|null} el mensaje de error, o null si esta todo bien
 */
function validateClientSide(formData) {
    if (!formData.terms) {
        return "Debes aceptar los terminos y condiciones";
    }
    return null;
}

form.addEventListener("submit", async (event) => {
    event.preventDefault();

    clearError();

    const name = form.querySelector("#register-name").value.trim();
    const email = form.querySelector("#register-email").value.trim();
    const password = form.querySelector("#register-password").value;
    const terms = form.querySelector("#register-terms").checked;

    const clientError = validateClientSide({ terms });
    if (clientError) {
        showError(clientError);
        return;
    }

    submitButton.disabled = true;
    submitButton.textContent = "Creando cuenta...";

    try {
        // Nota: "role" nunca se envia desde el cliente. El backend
        // siempre asigna Role.USER en el registro publico, por
        // seguridad (ver UserService.register).
        const authResponse = await register({ name, email, password });

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
        submitButton.textContent = "Registrarse";
    }
});