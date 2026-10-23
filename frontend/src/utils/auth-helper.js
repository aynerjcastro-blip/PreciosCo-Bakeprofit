// Claves utilizadas en localStorage
const TOKEN_KEY = 'token';
const USER_KEY = 'user';

/**
 * Obtiene el token JWT almacenado.
 */
export const getToken = () => {
    return localStorage.getItem(TOKEN_KEY);
};

/**
 * Obtiene los datos del usuario almacenados.
 */
export const getStoredUser = () => {
    const user = localStorage.getItem(USER_KEY);
    try {
        return user ? JSON.parse(user) : null;
    } catch {
        return null;
    }
};

/**
 * Guarda el token y la información del usuario en localStorage.
 */
export const saveAuthData = (data) => {
    if (data.token) {
        localStorage.setItem(TOKEN_KEY, data.token);
    }
    const userData = data.user || { name: data.name, email: data.email, role: data.role };
    if (userData) {
        localStorage.setItem(USER_KEY, JSON.stringify(userData));
    }
};

/**
 * Elimina las credenciales del usuario de localStorage al cerrar sesión.
 */
export const clearAuthData = () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
};