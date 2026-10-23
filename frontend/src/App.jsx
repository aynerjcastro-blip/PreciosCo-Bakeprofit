import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MainLayout from './layouts/MainLayout';
import Home from './pages/Home';
import Comparator from './pages/Comparator';
import Search from './pages/Search';
import Login from './pages/Login';
import Register from './pages/Register';
import ProtectedRoute from './components/ProtectedRoute';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Layout Principal con Header y Footer compartidos */}
        <Route path="/" element={<MainLayout />}>
          {/* Rutas Públicas */}
          <Route index element={<Home />} />
          <Route path="comparador" element={<Comparator />} />
          <Route path="buscar" element={<Search />} />
          <Route path="login" element={<Login />} />
          <Route path="register" element={<Register />} />

          {/* Rutas Protegidas (Requieren Login) */}
          <Route element={<ProtectedRoute />}>
            <Route
              path="seguidos"
              element={
                <div style={{ padding: '2rem', textAlign: 'center' }}>
                  <h2>Mis Tiendas y Productos Seguidos</h2>
                  <p>Esta sección solo es accesible para usuarios autenticados.</p>
                </div>
              }
            />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  );
}