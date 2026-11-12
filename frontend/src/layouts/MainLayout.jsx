import { useEffect } from 'react';
import { Outlet, useLocation, useNavigationType } from 'react-router-dom';
import Header from '../components/layout/Header/Header';
import Footer from '../components/layout/Footer/Footer';

/**
 * Lleva la ventana al inicio al entrar a otra página.
 * Por qué: BrowserRouter no restablece el scroll, así que una página nueva abría a la misma
 * altura que la anterior (al entrar al comparador desde la mitad de los resultados, aparecía
 * en la sección de opiniones).
 * Solo cambia con la ruta, no con los parámetros: filtrar o paginar en /buscar no debe saltar
 * arriba. Con Atrás y Adelante (POP) no se toca, para que el navegador recupere la posición.
 */
function ScrollToTopOnNavigate() {
    const { pathname } = useLocation();
    const navigationType = useNavigationType();

    useEffect(() => {
        if (navigationType !== 'POP') {
            window.scrollTo(0, 0);
        }
    }, [pathname, navigationType]);

    return null;
}

export default function MainLayout() {
    return (
        <>
            <ScrollToTopOnNavigate />
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
