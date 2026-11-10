import { createContext, useState } from 'react';
import {
    getStoredUser,
    getToken,
    saveAuthData,
    clearAuthData,
    updateStoredUser 
} from '../utils/auth-helper';

//Se crea el contexto aquí mismo (elimina la importación de authContextValue.js)
// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext();

export function AuthProvider({ children }) {
    const [user, setUser] = useState(() => {
        const storedUser = getStoredUser();
        const token = getToken();
        return (storedUser && token) ? storedUser : null;
    });


    const updateUserData = (newData) => {
        setUser((prevUser) => {
            const updatedUser = { ...prevUser, ...newData };
            updateStoredUser(updatedUser); 
            return updatedUser;
        });
    };

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
        //  Se agrega updateUserData al value para que otros componentes puedan usarla
        <AuthContext.Provider value={{ user, isAuthenticated, loading, login, logout, updateUserData }}>
            {children}
        </AuthContext.Provider>
    );
}