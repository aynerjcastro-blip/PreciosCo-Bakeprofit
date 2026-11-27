import { Link } from 'react-router-dom';
import LegalPage from './LegalPage';
import { CONTACT_EMAIL, LEGAL_UPDATED } from '@/config/contact';

/**
 * Política de tratamiento de datos personales (Ley 1581 de 2012 y Decreto 1377 de 2013).
 * Describe lo que la plataforma hace hoy: datos de cuenta, seguimientos, opiniones, cuotas de IA,
 * pagos de prueba (sin datos de tarjeta), almacenamiento local del navegador y los encargados
 * (base de datos en la nube, Google y Mercado Pago).
 */
const mail = <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>;

const SECTIONS = [
    {
        id: 'responsable',
        title: 'Responsable del tratamiento',
        content: (
            <>
                <p>
                    El responsable de tus datos es <strong>PreciosCo</strong>, un proyecto académico sin razón
                    social ni NIT, desarrollado en Colombia.
                </p>
                <p>Canal de atención para todo lo relacionado con tus datos: {mail}.</p>
            </>
        ),
    },
    {
        id: 'datos',
        title: 'Qué datos tratamos',
        content: (
            <>
                <h3>Si solo visitas la plataforma</h3>
                <p>
                    Puedes buscar y comparar precios sin cuenta. Tratamos la dirección IP de forma transitoria
                    para limitar el número de peticiones y proteger el servicio; no la asociamos a tu identidad.
                </p>
                <h3>Si creas una cuenta</h3>
                <ul>
                    <li><strong>Datos de cuenta:</strong> nombre, correo y contraseña (guardada cifrada con un
                        algoritmo de hash, nunca en texto plano). Si entras con Google, recibimos tu nombre y
                        correo de tu cuenta de Google.</li>
                    <li><strong>Seguridad:</strong> intentos fallidos de inicio de sesión, para bloquear la cuenta
                        temporalmente ante accesos sospechosos, y enlaces de recuperación de contraseña.</li>
                    <li><strong>Actividad:</strong> productos que sigues, precios objetivo, avisos de baja de
                        precio, calificaciones, comentarios y reportes de comentarios.</li>
                    <li><strong>Plan y consumo:</strong> tu plan (Gratuito o Premium) y los contadores mensuales
                        de consultas y recetas de IA.</li>
                    <li><strong>Pagos de Premium:</strong> la referencia de la transacción de Mercado Pago, su
                        estado, monto y fechas. <strong>No recibimos ni guardamos datos de tarjetas</strong>:
                        el pago se hace directamente en Mercado Pago.</li>
                </ul>
                <h3>Si usas los asistentes de IA</h3>
                <p>
                    Los mensajes que escribes en el chat (y los anteriores de esa conversación) se envían al
                    servicio de IA para generar la respuesta. PreciosCo <strong>no guarda las conversaciones en
                    sus servidores</strong>: solo se conservan en tu navegador mientras el chat está abierto y se
                    pierden al cerrarlo o recargar la página. Solo registramos cuántas consultas usaste.
                </p>
                <p>
                    No es necesario que compartas datos de salud para usar los asistentes de Droguería o
                    Cuidado facial. Si describes síntomas o condiciones de tu piel, hazlo solo en la medida
                    necesaria: se usan únicamente para responder esa consulta.
                </p>
            </>
        ),
    },
    {
        id: 'finalidades',
        title: 'Para qué usamos tus datos',
        content: (
            <ul>
                <li>Crear y administrar tu cuenta y permitirte iniciar sesión.</li>
                <li>Mostrar tus productos seguidos y enviarte avisos de baja de precio en la plataforma y,
                    si lo activas, por correo.</li>
                <li>Publicar tus calificaciones y comentarios con tu nombre (nunca con tu correo) y
                    moderar los reportados.</li>
                <li>Responder tus consultas a los asistentes de IA y controlar los límites de tu plan.</li>
                <li>Activar Premium cuando se confirma el pago.</li>
                <li>Enviarte correos necesarios del servicio, como la recuperación de contraseña.</li>
                <li>Proteger la plataforma contra accesos indebidos y abusos.</li>
            </ul>
        ),
    },
    {
        id: 'terceros',
        title: 'Con quién compartimos datos',
        content: (
            <>
                <p>
                    No vendemos ni cedemos tus datos. Solo los compartimos con proveedores que nos prestan un
                    servicio (encargados del tratamiento), en la medida necesaria:
                </p>
                <ul>
                    <li><strong>Supabase</strong> (base de datos en la nube): almacena la información de la
                        plataforma.</li>
                    <li><strong>Google</strong>: inicio de sesión con Google, envío de correos y el servicio de
                        IA Gemini, que recibe los mensajes del chat para generar las respuestas.</li>
                    <li><strong>Mercado Pago</strong>: procesa el pago de Premium.</li>
                </ul>
                <p>
                    Algunos de estos servicios guardan o procesan datos fuera de Colombia (por ejemplo, en
                    Estados Unidos). Al aceptar esta política autorizas esa transferencia, que se hace con
                    proveedores que aplican medidas de seguridad reconocidas.
                </p>
                <p>
                    Las tiendas cuyos precios comparamos no reciben tus datos: al abrir el enlace a un
                    producto, pasas a su sitio, que tiene su propia política.
                </p>
            </>
        ),
    },
    {
        id: 'navegador',
        title: 'Datos guardados en tu navegador',
        content: (
            <>
                <p>
                    No usamos cookies de publicidad ni de seguimiento. En el almacenamiento local de tu
                    navegador guardamos:
                </p>
                <ul>
                    <li>Tu sesión (un token de acceso y datos básicos del usuario), para no pedirte que inicies
                        sesión en cada visita.</li>
                    <li>Tu preferencia de modo claro u oscuro.</li>
                    <li>La conversación del chat de IA mientras está abierta (se borra al cerrar la pestaña).</li>
                </ul>
                <p>Al cerrar sesión se elimina la información de la sesión.</p>
            </>
        ),
    },
    {
        id: 'conservacion',
        title: 'Cuánto tiempo conservamos tus datos',
        content: (
            <p>
                Conservamos tus datos mientras tengas una cuenta activa. Si pides eliminarla, los suprimimos,
                salvo lo que debamos conservar por obligación legal o para resolver reclamaciones (por ejemplo,
                el registro de un pago). Un comentario eliminado deja de mostrarse; la calificación asociada se
                conserva de forma agregada en los promedios.
            </p>
        ),
    },
    {
        id: 'derechos',
        title: 'Tus derechos',
        content: (
            <>
                <p>Como titular de tus datos tienes derecho a:</p>
                <ul>
                    <li>Conocer, actualizar y rectificar tus datos.</li>
                    <li>Pedir prueba de la autorización que nos diste.</li>
                    <li>Saber qué uso les hemos dado.</li>
                    <li>Revocar la autorización y pedir la supresión de tus datos, incluida la eliminación de tu
                        cuenta.</li>
                    <li>Presentar quejas ante la Superintendencia de Industria y Comercio (SIC) después de
                        acudir a nosotros.</li>
                    <li>Acceder gratis a tus datos.</li>
                </ul>
            </>
        ),
    },
    {
        id: 'como-ejercer',
        title: 'Cómo ejercer tus derechos',
        content: (
            <>
                <p>
                    Escríbenos a {mail} desde el correo de tu cuenta, indicando qué necesitas (consulta,
                    corrección, eliminación de la cuenta, etc.). Podemos pedirte información para confirmar tu
                    identidad.
                </p>
                <ul>
                    <li><strong>Consultas:</strong> respondemos en máximo 10 días hábiles.</li>
                    <li><strong>Reclamos</strong> (corrección, supresión o revocación): respondemos en máximo
                        15 días hábiles.</li>
                </ul>
                <p>Algunos datos, como tu nombre, también puedes cambiarlos desde tu perfil.</p>
            </>
        ),
    },
    {
        id: 'seguridad',
        title: 'Seguridad',
        content: (
            <p>
                Protegemos tus datos con contraseñas cifradas mediante hash, acceso con tokens, bloqueo
                temporal ante intentos fallidos, control de permisos por rol y límites de uso. Ningún sistema
                es completamente seguro; si detectamos un incidente que afecte tus datos, te lo informaremos.
            </p>
        ),
    },
    {
        id: 'menores',
        title: 'Menores de edad',
        content: (
            <p>
                PreciosCo está dirigido a mayores de 18 años. No recolectamos a sabiendas datos de menores sin
                autorización de su representante legal; si detectamos una cuenta en ese caso, la eliminaremos.
            </p>
        ),
    },
    {
        id: 'cambios',
        title: 'Cambios a esta política',
        content: (
            <p>
                Podemos actualizar esta política. La fecha de la última actualización aparece al inicio. Si
                el cambio afecta las finalidades del tratamiento, te lo avisaremos y, cuando corresponda,
                pediremos de nuevo tu autorización.
            </p>
        ),
    },
];

export default function Privacy() {
    return (
        <LegalPage
            title="Política de Privacidad"
            updated={LEGAL_UPDATED}
            intro={
                <>
                    <p>
                        Esta política explica qué datos personales tratamos en PreciosCo, para qué y cómo puedes
                        ejercer tus derechos, conforme a la Ley 1581 de 2012 y el Decreto 1377 de 2013.
                    </p>
                    <p>
                        Al crear una cuenta o usar las funciones que requieren datos personales, autorizas el
                        tratamiento descrito aquí. Consulta también los{' '}
                        <Link to="/terminos">Términos y Condiciones</Link>.
                    </p>
                </>
            }
            sections={SECTIONS}
            related={{ to: '/terminos', label: 'Términos y Condiciones' }}
        />
    );
}
