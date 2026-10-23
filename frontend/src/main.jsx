import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { AuthProvider } from './context/AuthContext';

// Importación de estilos globales del proyecto
import './styles/normalize.css';
import './styles/tokens.css';
import './styles/base.css';

// Nuevos archivos modulares que reemplazan a layout.css
import './styles/layout/header.css';
import './styles/layout/footer.css';
import './styles/components/ui.css';

import './styles/responsive.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    {/* AuthProvider envuelve a toda la aplicación para proveer la sesión */}
    <AuthProvider>
      <App />
    </AuthProvider>
  </React.StrictMode>
);