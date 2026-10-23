import { useState } from 'react';
import {
    getStoredUser,
    getToken,
    saveAuthData,
    clearAuthData
} from '../utils/auth-helper';
import { AuthContext } from './authContextValue';


export function AuthProvider({ children }) {
    // Inicialización síncrona
    const [user, setUser] = useState(() => {
        const storedUser = getStoredUser();
        const token = getToken();
        return (storedUser && token) ? storedUser : null;
    });

    const [loading] = useState(false);

    const login = (authData) => {
        saveAuthData(authData);
        const userData = authData.user || {
            name: authData.name,
            email: authData.email,
            role: authData.role
        };
        setUser(userData);
    };

    const logout = () => {
        clearAuthData();
        setUser(null);
    };

    const isAuthenticated = Boolean(user && getToken());

    return (
        <AuthContext.Provider value={{ user, isAuthenticated, loading, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
}