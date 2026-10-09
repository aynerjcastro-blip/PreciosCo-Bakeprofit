import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'

// Importaciones de tus CSS globales
import './styles/normalize.css';
import './styles/tokens.css';
import './styles/base.css';
import './styles/layout.css';
// Si responsive.css aplica a todo, impórtalo también aquí
import './styles/responsive.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)