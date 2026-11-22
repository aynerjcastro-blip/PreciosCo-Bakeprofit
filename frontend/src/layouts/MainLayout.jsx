import { useState } from 'react';
import { useContext } from 'react';
import { useEffect } from 'react';
import { Outlet, useLocation, useNavigationType } from 'react-router-dom';
import Header from '../components/layout/Header/Header';
import Footer from '../components/layout/Footer/Footer';
import AiDock from '@/components/ai/AiDock';
import AiChatDrawer from '@/components/ai/AiChatDrawer';
import AiFab from '@/components/ai/AiFab';
import { AuthContext } from '@/context/AuthContext';
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
    const { user } = useContext(AuthContext);
    const [chatSection, setChatSection] = useState(null);



    return (
        <>
            <ScrollToTopOnNavigate />
            <Header />
            <div className="layout-content">
                <main><Outlet /></main>
            </div>

            {user && <AiDock onOpenChat={setChatSection} />}
            {user && <AiFab onOpenChat={setChatSection} />}   {/* ← nuevo */}

            {chatSection && (
                <AiChatDrawer
                    section={chatSection}
                    onClose={() => setChatSection(null)}
                />
            )}
            <Footer />
        </>
    );
}
// TODO (SCRUM-347): Verificar límite de consultas antes de enviar.
// Depende de SCRUM-329 (HT-23) — endpoint GET /api/ai/quota
// Si el usuario agotó sus consultas, mostrar mensaje y bloquear envío.