import { BrowserRouter, Routes, Route } from 'react-router-dom';

// Importacion de la estructura maestra
import MainLayout from './layouts/MainLayout';

// Importacion de las vistas
import Home from './pages/Home';
import Search from './pages/Search';
import Login from './pages/Login';
import Register from './pages/Register';
import Comparator from './pages/Comparator';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* MainLayout envuelve las rutas anidadas para mantener el esqueleto UI */}
        <Route path="/" element={<MainLayout />}>

          {/* La propiedad index define el componente que carga en la ruta raiz */}
          <Route index element={<Home />} />

          <Route path="buscar" element={<Search />} />
          <Route path="login" element={<Login />} />
          <Route path="register" element={<Register />} />

          {/* Ruta del comparador que leera el productId de la URL */}
          <Route path="comparador" element={<Comparator />} />

          {/* Redireccion de seguridad para rutas no encontradas (404) */}
          <Route path="*" element={<Home />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}