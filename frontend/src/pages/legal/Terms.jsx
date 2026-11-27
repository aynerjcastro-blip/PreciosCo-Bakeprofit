import { Link } from 'react-router-dom';
import LegalPage from './LegalPage';
import { CONTACT_EMAIL, LEGAL_UPDATED } from '@/config/contact';

/**
 * Términos y condiciones de uso. Contenido basado en el alcance y la especificación del
 * proyecto (docs/): comparador sin venta directa, precios informativos con fecha y fuente,
 * IA orientativa (RN-17, RN-33), planes Gratuito y Premium (RN-36, RN-37) y reglas de opinión.
 */
const mail = <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>;

const SECTIONS = [
    {
        id: 'aceptacion',
        title: 'Aceptación',
        content: (
            <p>
                Al usar PreciosCo, con o sin cuenta, aceptas estos términos y la{' '}
                <Link to="/privacidad">Política de Privacidad</Link>. Si no estás de acuerdo, no uses la
                plataforma.
            </p>
        ),
    },
    {
        id: 'que-es',
        title: 'Qué es PreciosCo',
        content: (
            <>
                <p>
                    PreciosCo es un <strong>proyecto académico</strong> que compara precios de productos de
                    supermercados, droguerías, tiendas de cuidado facial y repuestos para automóviles en
                    Colombia, para ayudarte a decidir dónde comprar.
                </p>
                <p>
                    PreciosCo <strong>no vende productos</strong> ni gestiona compras, pagos de productos,
                    domicilios ni inventarios. La compra se hace siempre en la tienda, a través del enlace a
                    su página.
                </p>
            </>
        ),
    },
    {
        id: 'cuentas',
        title: 'Cuentas de usuario',
        content: (
            <ul>
                <li>
                    Puedes buscar y comparar precios sin registrarte. Para seguir productos, opinar y usar los
                    asistentes de IA necesitas una cuenta.
                </li>
                <li>
                    Debes ser <strong>mayor de 18 años</strong>. Un menor solo puede usar una cuenta con
                    autorización de su representante legal.
                </li>
                <li>
                    Te registras con nombre, correo y contraseña, o con tu cuenta de Google. Cada correo
                    corresponde a una sola cuenta.
                </li>
                <li>
                    Eres responsable de mantener tu contraseña en secreto y de la actividad de tu cuenta. Tras
                    varios intentos fallidos de inicio de sesión, la cuenta se bloquea temporalmente por
                    seguridad.
                </li>
                <li>
                    Podemos bloquear o desactivar una cuenta que incumpla estos términos. Una cuenta bloqueada
                    no puede acceder a la plataforma.
                </li>
            </ul>
        ),
    },
    {
        id: 'precios',
        title: 'Precios e información de productos',
        content: (
            <>
                <p>
                    Los precios provienen de fuentes públicas de las tiendas o de los proveedores. Cada precio
                    muestra la fecha en que la tienda lo confirmó por última vez.
                </p>
                <ul>
                    <li>
                        Los precios son <strong>informativos</strong>: pueden cambiar en cualquier momento y
                        variar según la ciudad, el canal o las promociones de cada tienda.
                    </li>
                    <li>
                        No garantizamos que el precio publicado coincida con el precio final que cobra el
                        establecimiento, ni la disponibilidad del producto. Antes de comprar, confirma el precio
                        en la tienda.
                    </li>
                    <li>
                        Las marcas, logotipos y nombres comerciales pertenecen a sus respectivos dueños y se usan
                        solo para identificar productos y tiendas.
                    </li>
                </ul>
                <p>
                    Si encuentras un precio incorrecto, avísanos en {mail}.
                </p>
            </>
        ),
    },
    {
        id: 'ia',
        title: 'Asistentes de inteligencia artificial',
        content: (
            <>
                <p>
                    Los asistentes de Supermercados, Droguería y Cuidado facial responden con base en los
                    productos y precios del catálogo. Sus respuestas son <strong>orientativas</strong>, pueden
                    contener errores y no reemplazan tu propio criterio.
                </p>
                <ul>
                    <li>
                        <strong>Droguería:</strong> solo considera productos de venta libre. No recomienda
                        medicamentos que requieren fórmula médica, no diagnostica, no prescribe y no sustituye
                        al médico ni a otro profesional de la salud. Ante síntomas graves, acude a un médico o a
                        urgencias.
                    </li>
                    <li>
                        <strong>Cuidado facial:</strong> sus recomendaciones según tipo de piel, necesidades y
                        presupuesto son orientativas y no sustituyen la valoración de un dermatólogo.
                    </li>
                    <li>
                        <strong>Supermercados:</strong> las cotizaciones y recetas usan los precios del catálogo
                        en ese momento; lee siempre las etiquetas de los productos (alérgenos, ingredientes).
                    </li>
                </ul>
                <p>
                    La conversación es temporal: se pierde al cerrar el chat o recargar la página. El uso de
                    los asistentes está sujeto a los límites de tu plan.
                </p>
            </>
        ),
    },
    {
        id: 'planes',
        title: 'Planes Gratuito y Premium',
        content: (
            <>
                <p>
                    Comparar precios es siempre gratuito. Las funciones de IA y el seguimiento de productos
                    tienen límites en el plan Gratuito, que la plataforma muestra junto con tu consumo. El
                    contador de IA se reinicia cada mes. Si una acción supera un límite, se bloquea y se te
                    ofrece pasar a Premium.
                </p>
                <p>
                    El plan Premium amplía esos límites y se activa cuando el sistema confirma el pago hecho a
                    través de la pasarela Mercado Pago. El pago cubre solo el acceso a las funciones Premium de
                    PreciosCo, no la compra de productos en las tiendas.
                </p>
                <p>
                    <strong>Mientras PreciosCo sea un proyecto académico, la pasarela funciona en modo de
                        prueba y no se realizan cobros de dinero real.</strong> Si en el futuro se habilitan
                    pagos reales, el precio, la vigencia y las condiciones se informarán antes de pagar y estos
                    términos se actualizarán.
                </p>
            </>
        ),
    },
    {
        id: 'opiniones',
        title: 'Calificaciones y comentarios',
        content: (
            <ul>
                <li>
                    Puedes calificar (de 1 a 5 estrellas) y comentar un producto en una tienda una sola vez, y
                    editar tu opinión durante el día siguiente a publicarla. Puedes eliminar tu comentario en
                    cualquier momento; la calificación se conserva.
                </li>
                <li>
                    No publiques contenido ofensivo, discriminatorio, falso, publicidad, datos personales de
                    terceros ni opiniones fraudulentas o repetidas.
                </li>
                <li>
                    Cualquier usuario puede reportar un comentario. Los comentarios reportados se revisan y
                    pueden ocultarse.
                </li>
                <li>
                    Tus opiniones se muestran públicamente con tu nombre; nunca con tu correo.
                </li>
            </ul>
        ),
    },
    {
        id: 'uso',
        title: 'Uso aceptable',
        content: (
            <>
                <p>No está permitido:</p>
                <ul>
                    <li>Usar la plataforma para fines ilegales o para perjudicar a terceros.</li>
                    <li>
                        Extraer de forma masiva o automatizada el contenido o los precios de PreciosCo, ni
                        saturar el servicio con peticiones.
                    </li>
                    <li>Intentar acceder a cuentas ajenas o a partes no públicas del sistema.</li>
                    <li>Suplantar a otra persona o a una tienda.</li>
                </ul>
            </>
        ),
    },
    {
        id: 'propiedad',
        title: 'Propiedad intelectual',
        content: (
            <p>
                El diseño, el código y los textos propios de PreciosCo pertenecen a sus autores. Las imágenes
                de productos y los logotipos de las tiendas pertenecen a sus titulares y se muestran solo con
                fines de identificación y comparación.
            </p>
        ),
    },
    {
        id: 'responsabilidad',
        title: 'Limitación de responsabilidad',
        content: (
            <p>
                PreciosCo se ofrece "tal como está". No respondemos por diferencias entre el precio publicado
                y el precio final de una tienda, por la disponibilidad de productos, por las decisiones de
                compra que tomes con base en la información o en las respuestas de los asistentes de IA, ni
                por el contenido de los sitios de terceros a los que enlazamos.
            </p>
        ),
    },
    {
        id: 'cambios',
        title: 'Cambios a estos términos',
        content: (
            <p>
                Podemos actualizar estos términos. La fecha de la última actualización aparece al inicio de
                esta página. Si el cambio es importante, lo avisaremos en la plataforma.
            </p>
        ),
    },
    {
        id: 'ley',
        title: 'Ley aplicable',
        content: (
            <p>
                Estos términos se rigen por las leyes de la República de Colombia, incluido el Estatuto del
                Consumidor (Ley 1480 de 2011) en lo que aplique.
            </p>
        ),
    },
    {
        id: 'contacto',
        title: 'Contacto',
        content: <p>Para dudas sobre estos términos, escríbenos a {mail}.</p>,
    },
];

export default function Terms() {
    return (
        <LegalPage
            title="Términos y Condiciones"
            updated={LEGAL_UPDATED}
            intro={
                <p>
                    Estas son las reglas para usar PreciosCo. Las escribimos de forma sencilla para que sepas
                    qué puedes esperar de la plataforma y qué esperamos de ti.
                </p>
            }
            sections={SECTIONS}
            related={{ to: '/privacidad', label: 'Política de Privacidad' }}
        />
    );
}
