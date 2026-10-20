import { createContext, useState } from 'react';
import {
    getStoredUser,
    getToken,
    saveAuthData,
    clearAuthData
} from '../utils/auth-helper';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    // Carga el estado inicial de manera síncrona sin provocar renders en cascada
    const [user, setUser] = useState(() => {
        const storedUser = getStoredUser();
        const token = getToken();
        return (storedUser && token) ? storedUser : null;
    });

    const [loading] = useState(false);

    const login = (authData) => {
        saveAuthData(authData);
        setUser(authData.user || { name: authData.name, email: authData.email, role: authData.role });
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

