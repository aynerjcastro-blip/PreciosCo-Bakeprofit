import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MainLayout from './layouts/MainLayout';
import Home from './pages/Home';
import Comparator from './pages/Comparator';
import Search from './pages/Search';
import ProtectedRoute from './components/ProtectedRoute';
import Followed from './pages/Followed';
import AuthPage from './pages/AuthPage';

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
          <Route path="/login" element={<AuthPage type="login" />} />
          <Route path="/register" element={<AuthPage type="register" />} />

          {/* Rutas Protegidas (Requieren Login) */}
          <Route element={<ProtectedRoute />}>
            <Route
              path="seguidos"
              element={<Followed />}
              
            />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  );
}