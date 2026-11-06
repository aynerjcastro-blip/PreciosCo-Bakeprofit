import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { AuthProvider } from './context/AuthContext';
import { GoogleOAuthProvider } from '@react-oauth/google'; //** Provider de Google

// Solo estilos globales base
import './styles/normalize.css';
import './styles/tokens.css';
import './styles/base.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    {/***Envolvemos la app pasando el ID de cliente desde la variable de entorno */}
    <GoogleOAuthProvider clientId={import.meta.env.VITE_GOOGLE_CLIENTE_ID}>
      <AuthProvider>
        <App />
      </AuthProvider>
    </GoogleOAuthProvider>
  </React.StrictMode>
);