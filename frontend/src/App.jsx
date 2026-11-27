import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MainLayout from './layouts/MainLayout';
import Home from './pages/Home';
import Comparator from './pages/Comparator';
import Search from './pages/Search';
import ProtectedRoute from './components/layout/ProtectedRoute';
import Followed from './pages/Followed';
import AuthPage from './pages/AuthPage';
import ForgotPassword from '@/components/password/ForgotPassword';
import ResetPassword from '@/components/password/ResetPassword';
import Profile from '@/components/profile/Profile';
import SectionPresentation from './pages/SectionPresentation';
import Explore from './pages/Explore';
import Contact from './pages/Contact';
import { AuthProvider } from '@/context/AuthContext';
export default function App() {
  return (
    <AuthProvider>
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
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<ResetPassword />} />
            <Route path="/secciones/:slug" element={<SectionPresentation />} />
            <Route path="explorar" element={<Explore />} />
            <Route path="contacto" element={<Contact />} />

            {/* Rutas Protegidas (Requieren Login) */}
            <Route element={<ProtectedRoute />}>
              <Route path="seguidos" element={<Followed />} />
              <Route path="perfil" element={<Profile />} />
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}