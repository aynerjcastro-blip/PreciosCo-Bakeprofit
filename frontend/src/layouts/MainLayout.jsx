import { Outlet } from 'react-router-dom';
import Header from '../components/Header';
import Footer from '../components/Footer';

export default function MainLayout() {
    return (
        <>
            <Header />

            {/* 
        Contenedor principal. Su estructura modular facilita
        añadir un sidebar colapsable en el futuro si la app crece. 
      */}
            <div className="layout-content">
                <main>
                    {/* Aquí se inyectarán dinámicamente tus páginas (Inicio, Buscar, etc.) */}
                    <Outlet />
                </main>
            </div>

            <Footer />
        </>
    );
}