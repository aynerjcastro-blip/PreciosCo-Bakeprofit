# PreciosCo — Plataforma web de comparación de precios

Análisis y especificación: historias de usuario, modelo de negocio, requisitos y trazabilidad

Documento iniciado el 15 de octubre de 2026 · Versión 1.2 (planificación del Sprint 4) · Actualizado el 21 de noviembre de 2026

**Universidad Popular del Cesar** · Programa de Ingeniería de Sistemas · Asignatura: Ingeniería de Software I · Docente: John Jairo Patiño Vanegas

**Integrantes:** Abisael Freyle Almenarez, Ayner José Castro Benavides, Darwin Banquett, Sebastián Ramos

Valledupar, Colombia · actualización de noviembre de 2026

## 1. Planteamiento y contexto

PreciosCo compara los precios de productos del hogar, de droguería y de cuidado facial entre establecimientos. El proyecto contempla extender la comparación a repuestos de automóviles, según el resultado de la prueba de datos.

**El problema.** Quien hace su mercado no sabe en qué establecimiento están las mejores opciones de compra y no puede recorrer supermercado por supermercado para comparar. Pierde tiempo y transporte, y termina pagando más de lo necesario.

### Problemas detectados

| ID | Problema | Origen |
| --- | --- | --- |
| PB-1 | El consumidor no sabe en qué establecimiento está la mejor opción de compra | Enunciado por el equipo |
| PB-2 | No puede recorrer establecimiento por establecimiento para comparar | Enunciado por el equipo |
| PB-3 | Los precios cambian con frecuencia y no se entera de las bajadas | Por validar con usuarios |
| PB-4 | No sabe si el precio que ve está vigente ni de dónde proviene | Por validar con usuarios |
| PB-5 | No conoce la opinión de otros compradores sobre productos y tiendas | Por validar con usuarios |
| PB-6 | La comparación existente solo cubre supermercados; droguería, cuidado facial y repuestos de automóviles quedan fuera | Observado en el bot actual |
| PB-7 | Los establecimientos no tienen un canal propio para mantener sus precios al día | Por validar con usuarios |

### Estado de partida (AS-IS del bot)

- Proceso manual: el consumidor decide qué comprar, recorre o consulta tiendas, compara por su cuenta y compra donde cree que conviene.
- Bot de Telegram: navega por categorías y productos, compara precios entre supermercados y muestra el promedio de calificación.
- Cada usuario califica de 1 a 5 estrellas un producto en un supermercado, una sola vez.
- Un asistente de IA convierte una receta o lista en ingredientes de un catálogo fijo y devuelve sus precios.
- Solo cubre supermercados; los usuarios se registran automáticamente por su chat; cada precio guarda fecha y fuente.

### Estado deseado (TO-BE)

Una plataforma web (React, Spring Boot y PostgreSQL) que conserva la comparación del bot y suma cuentas de usuario, calificaciones y comentarios de un producto en una tienda con sus promedios, y seguimiento de productos con avisos de baja de precio. Las fases posteriores agregan droguerías, cuidado facial, repuestos para automóviles, IA especializada, suscripciones, un panel administrativo y un módulo para que los proveedores publiquen sus precios.

### Alcance por fases

| Fase | Contenido | Estado en este documento |
| --- | --- | --- |
| 1 (MVP) | Usuarios, categorías, productos, precios y comparación de supermercados | Con historias |
| 2 | Calificaciones, comentarios, seguimiento de productos y notificaciones | Con historias |
| 3 | Comparadores de droguerías, cuidado facial y repuestos para automóviles | Cuidado facial: Cutis, Medipiel y BellaPiel. Sprint 4: prueba de datos y scraper de repuestos. El comparador queda en backlog |
| 4 | IA especializada en tres secciones y planes Gratuito y Premium | Historias y requisitos del Sprint 4 |
| 5 | Módulo de proveedores y panel administrativo | Historias del panel priorizado para el Sprint 4. Proveedores y gestión ampliada quedan para fases de trabajo posteriores |
| 6 | Reportes, mejoras e integraciones externas | Solo requisitos |

**Fuera del alcance:** compra directa, pagos y domicilios de productos; repuestos para motocicletas en el alcance actual; verificación de compatibilidad con modelos de automóvil; inventario interno de las tiendas; garantía de disponibilidad física del producto y de precios en tiempo real; diagnósticos médicos o dermatológicos y prescripciones; aplicación móvil nativa; integración con todas las tiendas; integraciones iniciales con WhatsApp o SMS; logística de proveedores; monetización avanzada; expansión internacional.

El pago de la suscripción Premium a PreciosCo se incorpora por una exigencia de Programación Web. Las historias y requisitos de suscripción trazan ese origen en OB-07 y en la decisión DP-51.

### Restricciones y entregables

| ID | Restricción del proyecto |
| --- | --- |
| RP-01 | La base de datos debe estar alojada en un servicio en la nube (PostgreSQL) |
| RP-02 | La documentación de la API debe mantenerse actualizada con Swagger/OpenAPI |
| RP-03 | El código se gestiona con Git/GitHub y una rama por funcionalidad |
| RP-04 | Cada integrante usa como máximo 2 conexiones a la base compartida (maximum-pool-size=2) para evitar el error EMAXCONNSESSION |

Entregables de cada fase: E-01 pruebas funcionales y de seguridad; E-02 manual de usuario y documentación técnica.

### Estado y límites de este documento

Conserva el modelo de negocio, las historias anteriores y la reconciliación del 18-nov-2026. Incorpora el alcance y los requisitos del Sprint 4 según los acuerdos del 20-nov-2026. La descripción del bot es el estado de partida, no el estado actual de la web. El comportamiento acordado se especifica aquí; el detalle técnico, la evidencia y los pendientes de integración se registran en DECISIONS.md. Reconstruye el modelo de negocio a partir del bot, del documento de alcance y de los planes de trabajo, y las historias de usuario se derivaron de los requisitos para validarlos. Los problemas PB-3, PB-4, PB-5 y PB-7 y las reglas marcadas «por confirmar» no se han validado con usuarios reales. Los valores con asterisco (\*) son propuestas del equipo. La moderación de comentarios y el bloqueo de cuentas se hacen manualmente en la base de datos hasta que estén disponibles las funciones correspondientes del panel.

### Glosario

- **Consumidor:** persona registrada que consulta y compara precios (rol USER). **Visitante:** consumidor sin sesión.
- **Proveedor:** establecimiento con cuenta comercial (rol SUPPLIER).&#32;
- **Administrador:** rol ADMIN.
- **Plan Gratuito:** acceso básico a IA y seguimiento para el consumidor con sesión, sujeto a topes por cuenta.
- **Plan Premium:** suscripción a PreciosCo que amplía los topes de IA y seguimiento.
- **Calificación:** puntaje de un producto en una tienda. Una opinión es la calificación y el comentario que se registran juntos.&#32;
- **Borrado lógico:** el registro se conserva con estado ELIMINADO y deja de mostrarse.
- **Observación/oferta cruda:** dato capturado en staging; todavía no es un precio publicado.
- **SKU local:** identificador de un artículo dentro de una tienda; no acredita identidad entre tiendas.
- **Producto genérico por peso:** referencia curada de 1000 G, sin EAN/marca/referencia, usada para comparar COP/kg de SKU físicamente equivalentes.
- **CONFLICT:** estado de una observación que requiere revisión; no es una respuesta HTTP 409 ni la cláusula SQL ON CONFLICT.
- **Nombres internos:** el repositorio usa BakeProfit (backend) y ComprasCo (paquete Java); el nombre del producto es PreciosCo.

## 2. Objetivos del negocio

Los objetivos abarcan el ahorro del consumidor, la calidad de la información y la sostenibilidad del servicio.

| ID | Objetivo | Problema | Indicador |
| --- | --- | --- | --- |
| OB-01 | Reducir el tiempo y esfuerzo que una persona invierte en comparar precios para su mercado | PB-1, PB-2 | Minutos para decidir dónde comprar una lista |
| OB-02 | Aumentar el ahorro que logra al elegir dónde comprar | PB-1, PB-3 | Diferencia entre el precio promedio y el de la mejor opción |
| OB-03 | Mejorar la confianza en la información de precios (vigencia y origen) | PB-4 | % de precios actualizados dentro de un plazo definido |
| OB-04 | Ampliar las compras cubiertas, de supermercados a droguería, cuidado facial y repuestos de automóviles | PB-6 | Tipos de establecimiento y categorías comparables |
| OB-05 | Mejorar la confianza en la calidad de productos y establecimientos | PB-5 | % de productos con al menos una calificación |
| OB-06 | Facilitar que los establecimientos mantengan su oferta actualizada | PB-7 | % de productos de proveedores actualizados en plazo |
| OB-07 | Generar ingresos sostenibles mediante un modelo freemium y mantener gratuita la comparación de precios | Exigencia de Programación Web | Tasa de altas Premium y disponibilidad de la comparación pública |

Las metas numéricas de los indicadores se fijan cuando haya datos de uso reales.

## 3. Actores

Los roles USER, SUPPLIER y ADMIN corresponden a los actores humanos registrados. Los servicios externos y las tareas programadas participan como actores de soporte.

| ID | Actor | Naturaleza | Participación | Rol en el sistema |
| --- | --- | --- | --- | --- |
| ACT-01 | Visitante | Humano | Iniciador | Sin sesión; puede buscar y comparar mediante endpoints públicos |
| ACT-02 | Consumidor registrado | Humano | Iniciador | USER |
| ACT-03 | Proveedor | Humano | Iniciador | SUPPLIER |
| ACT-04 | Administrador | Humano | Iniciador | ADMIN |
| ACT-05 | Servicio de IA | Sistema externo | Secundario | — |
| ACT-06 | Servicio de correo | Sistema externo | Secundario | — |
| ACT-07 | Revisión periódica de precios | Tiempo | Iniciador | Tarea programada |
| ACT-08 | Fuente externa de precios | Sistema externo | Secundario | Scraper que captura observaciones; el backend valida y publica precios. Ejecución local o manual en GitHub Actions |
| ACT-09 | Proveedor de identidad (Google) | Sistema externo | Secundario | Valida la cuenta de Google al iniciar sesión |  
| ACT-10 | Pasarela de pagos | Sistema externo | Secundario | Procesa la suscripción a PreciosCo y comunica su resultado. Proveedor por seleccionar |

Solo el rol USER puede calificar, comentar y reportar: ADMIN y SUPPLIER reciben 403. No se consideran actores «el sistema», la base de datos, el bot de Telegram, el panel administrativo ni «el usuario» genérico.

## 4. Procesos del negocio

Los procesos recorren el servicio desde el acceso y la comparación hasta la suscripción y la administración de la plataforma.

| ID | Proceso | Actor | Entrada | Salida |
| --- | --- | --- | --- | --- |
| P-01 | Gestionar cuenta de consumidor | Consumidor, proveedor de identidad | Datos personales y credenciales, o cuenta de Google | Cuenta activa y acceso |
| P-02 | Consultar y comparar precios | Visitante o consumidor | Sección, producto o categoría y filtros compatibles | Precios comparables, mejor opción y fecha de confirmación |
| P-03 | Calificar, comentar y reportar | Consumidor registrado | Producto, tienda, puntaje, comentario, razón del reporte | Calificación o comentario registrado y promedio actualizado |
| P-04 | Seguir productos y recibir alertas | Consumidor registrado, revisión periódica | Producto seguido, precio objetivo | Aviso de baja de precio |
| P-05 | Consultar asesoría de compra | Consumidor registrado, servicio de IA | Sección y lista, receta o necesidad | Recomendación en chat temporal, con productos y precios elegibles del catálogo y los avisos de la sección |
| P-06 | Publicar y actualizar oferta | Proveedor | Datos del establecimiento, productos, precios, disponibilidad | Oferta publicada de inmediato y pendiente de revisión |
| P-07 | Administrar y moderar | Administrador | Cuentas, actividad, catálogo, solicitudes, reportes, precios y datos de operación | Cambios registrados en el historial, métricas, rankings y gráficas |
| P-08 | Gestionar plan y suscripción | Consumidor registrado, pasarela de pagos | Plan vigente, consumo, solicitud de cambio y confirmación de pago | Consulta de límites o activación confirmada de Premium |

**Subproceso de soporte a P-02 y P-04 (RF51):** capturar observaciones de fuentes permitidas, cerrar la corrida, verificar identidad/presentación, resolver duplicados y registrar o refrescar el precio. Los rechazos y conflictos se revisan antes de autorizar su reintento. Este soporte no otorga al proveedor ni al visitante acceso directo a staging o equivalencias.

## 5. Historias de usuario

Las historias usan el formato Como…, quiero…, para… y se vinculan con criterios de aceptación, requisitos y objetivos. Las historias del Sprint 4 conservan su tamaño por estimar cuando el plan no lo define.

### Cómo leerlas: INVEST y SMART

- **INVEST** (cómo debe ser una historia): Independiente, Negociable (dice el qué, no el cómo), Valiosa, Estimable, Pequeña (cabe en una iteración) y Testeable.
- **SMART** (cómo deben ser los criterios): Específicos, Medibles, Alcanzables, Relevantes (ligados a un objetivo) y Temporales (ligados a una fase).
- **Tamaños:** S hasta 1 día, M de 2 a 3 días, L más de 3 días (propuesta del equipo).
- Las historias se derivaron de los requisitos una vez escritos estos: sirven para validarlos y para explicar el valor que recibe cada rol.

### Criterios comunes a todas las historias

- Se ve bien en computador, tableta y celular (RNF01).
- Los errores se devuelven en JSON con el código correcto: 400, 401, 403, 404 o 409 (RNF08).
- El endpoint está documentado en Swagger y tiene un caso de éxito y uno de error en Postman.
- Se probó de extremo a extremo contra el backend real y usa los tokens del sistema de diseño.

### Fase 1

**HU-01 · Registrarme** — RF01 · OB-05 · S

Como visitante, quiero registrarme con mi nombre, correo y contraseña, para poder calificar, comentar y seguir productos.

- Con un correo nuevo la cuenta se crea y quedo con sesión iniciada.
- Con un correo ya registrado recibo 409 con un mensaje claro.
- Con un correo mal formado o campos vacíos recibo 400 que indica el campo.
- Mi contraseña no se guarda en texto plano.

**HU-02 · Iniciar sesión** — RF02 · OB-05 · S

Como consumidor registrado, quiero iniciar sesión con mi correo y contraseña, para que la plataforma me reconozca.

- Credenciales válidas devuelven un token con vigencia de 24 horas.
- Una contraseña incorrecta devuelve 401 con un mensaje claro.
- Una cuenta desactivada no puede iniciar sesión.

**HU-03 · Conocer la plataforma desde el inicio** — RF50 · OB-01 · M

Como visitante, quiero ver en la página de inicio qué es PreciosCo y cómo funciona, para decidir rápido si me sirve y llegar a la búsqueda.

- La portada muestra presentación del servicio, ejemplos con productos y precios reales, cifras y tiendas activas, acceso a secciones comerciales, beneficios, cómo funciona, quiénes somos, preguntas frecuentes y llamada a la acción.
- Los bloques sin datos se omiten sin inventar cifras; no se muestran testimonios ni asistentes de IA todavía no implementados.
- Cada página de sección muestra disponibilidad y avisos aplicables y enlaza al buscador mediante `?sector=<slug>`.
- Desde el inicio llego a la búsqueda en un clic.
- La navegación y el pie son iguales en todas las páginas.

**HU-04 · Buscar por nombre** — RF05 · OB-01 · S

Como visitante, quiero buscar productos escribiendo parte del nombre, para encontrar lo que voy a comprar sin recorrer listados.

- Un texto parcial devuelve los productos activos cuyo nombre lo contiene, sin distinguir mayúsculas.
- Sin coincidencias veo «No se encontraron productos» y no un error.
- La búsqueda por marca se agrega cuando la marca entre al modelo (DP-11); hasta entonces queda fuera.

**HU-05 · Explorar por categoría** — RF05, RF11 · OB-04 · S

Como visitante, quiero ver los productos agrupados por categoría, para explorar cuando no sé el nombre exacto.

- Elegir una categoría lista solo sus productos activos.
- Combinar categoría y texto filtra por ambos (mínimo 2 casos probados).
- Las categorías siguen dos niveles: sección raíz y categoría hoja; los productos pertenecen a la hoja.

**HU-06a · Filtrar por nombre y categoría** — RF06 (parcial; se completa con HU-06b y HU-35) · OB-01, OB-02 · S

Como visitante, quiero filtrar los resultados por nombre y categoría, para quedarme solo con lo que me conviene.

- El filtro combinado (nombre y categoría) se aplica en una sola consulta.
- Cada filtro se resuelve en el backend, no con una manipulación visual del cliente.
- Si hay una sección seleccionada, las opciones de categoría y tienda pertenecen a esa sección y la búsqueda envía su ID real. «Todas» elimina la categoría, no la sección.
- Al cambiar de sección se limpian categoría, tiendas y página; un sector desconocido no ejecuta una búsqueda global y un fallo de filtros ofrece reintento.
- Una categoría inexistente responde 404; una categoría que existe pero no tiene productos responde vacío, sin error.
- Se separó de HU-06 por alcance del Sprint 1; los filtros por tienda y por rango de precio pasaron a HU-06b, y el filtro por marca, a HU-35.

**HU-06b · Filtrar por tienda y rango de precio** — RF06 · OB-01, OB-02 · M

Como visitante, quiero filtrar los resultados por tienda y por rango de precio, para quedarme solo con lo que me conviene.

- Filtrar por una o varias tiendas deja solo los productos con un precio vigente en alguna de esas tiendas; solo cuentan las tiendas activas.
- Filtrar por precio mínimo y máximo deja solo los productos cuyo precio vigente (el más reciente de cada tienda) está dentro del rango; con tiendas seleccionadas, solo cuenta el precio de esas tiendas (DP-34).
- Cada filtro se resuelve en el backend, no con una manipulación visual del cliente.
- Si hay una sección seleccionada, las opciones de categoría y tienda pertenecen a esa sección y la búsqueda envía su ID real. «Todas» elimina la categoría, no la sección.
- Al cambiar de sección se limpian categoría, tiendas y página; un sector desconocido no ejecuta una búsqueda global y un fallo de filtros ofrece reintento.
- Combinado con nombre y categoría (HU-06a), el resultado cumple todos los filtros a la vez.
- Una tienda inexistente o inactiva responde 404; un precio negativo o un mínimo mayor que el máximo responde 400; una búsqueda sin resultados responde vacío, sin error (DP-35).
- Los resultados se muestran por páginas, ordenados por mayor ahorro (DP-36, DP-39), y los filtros de tienda, precio y página quedan en la dirección de la búsqueda.
- Sin tienda ni rango, la búsqueda se comporta como en HU-06a, incluidos los productos sin precio.

**HU-07 · Comparar precios por tienda** — RF07, RF10, RF12 · OB-01, OB-02, OB-03 · M

Como visitante, quiero ver el precio de un producto en cada supermercado con la fecha en que la tienda lo confirmó por última vez, para comparar y saber qué tan vigente es.

- Un producto vendido en 3 tiendas muestra los 3 precios, cada uno con la fecha de su última confirmación (DP-32).
- Por tienda solo aparece el precio más reciente.
- Solo aparecen las tiendas activas (DP-37).
- D1, Éxito, Jumbo y Olímpica pueden aparecer en la comparación.
- Un producto sin precios responde vacío, sin error 500.

**HU-08 · Ver la mejor opción** — RF08, RF09 · OB-02 · S

Como visitante, quiero que se resalte la tienda más barata y ver los precios de menor a mayor, para decidir sin hacer cuentas.

- Se resalta la tienda con el menor precio más reciente.
- Cada precio de la lista es menor o igual al siguiente.
- En un empate se resaltan todas las tiendas empatadas (por confirmar).

**HU-27 · Ir a la tienda desde la comparación** — RF57 · OB-01 · S

Como visitante, quiero abrir la página del producto en la tienda desde la comparación, para comprar donde está más barato sin buscarlo de nuevo.

- Cada precio con página registrada muestra un enlace que abre la página del producto en la tienda, en una pestaña nueva.
- Un precio sin página registrada se muestra sin enlace, nunca con un enlace roto.
- El enlace funciona sin iniciar sesión.

### Fase 2: sesión

**HU-09 · Ver mi sesión y cerrarla** — RF02, RF49 · OB-05 · S

Como consumidor registrado, quiero ver mi nombre en el encabezado y poder cerrar sesión, para saber que estoy identificado y proteger mi cuenta en un equipo compartido.

- El encabezado cambia al iniciar y cerrar sesión sin recargar la página (RF49).
- Cerrar sesión borra mi token y mis datos del navegador.
- Sin sesión, entrar a una ruta protegida como /seguidos me lleva a /login.

**HU-24 · Iniciar sesión con Google** — RF56 · OB-01, OB-05 · M
 
Como visitante, quiero iniciar sesión con mi cuenta de Google, para entrar sin crear ni recordar otra contraseña.
 
- Con una cuenta de Google cuyo correo no está registrado, se crea una cuenta con rol USER y quedo con sesión iniciada.
- Si el correo ya está registrado, entro a esa misma cuenta y no se crea otra (provisional, DP-22).
- Si cancelo en Google o la validación falla, vuelvo a /login con un mensaje claro.
- Una cuenta desactivada no puede iniciar sesión, tampoco con Google.
- La sesión recibe el mismo token de 24 horas que el inicio de sesión normal.

**HU-25 · Recuperar mi contraseña** — RF03 · OB-05 · M
 
Como consumidor registrado, quiero recibir en mi correo un enlace para crear una contraseña nueva, para volver a entrar si la olvidé.
 
- El enlace llega solo al correo registrado y deja de servir a los 30 minutos\*.
- Un enlace usado o vencido se rechaza con un mensaje claro.
- Si el correo no está registrado, la respuesta es la misma que si lo estuviera, para no revelar qué correos existen.

**HU-26 · Modificar mi perfil** — RF04 (DEBERÍA) · OB-05 · S
 
Como consumidor registrado, quiero cambiar mi nombre y mi correo, para mantener mis datos al día.
 
- El nombre nuevo aparece en el encabezado sin volver a iniciar sesión.
- Un correo que ya usa otra cuenta devuelve 409.
- Una cuenta creada con Google no cambia su correo desde el perfil (provisional, DP-23).

### Fase 2: calificaciones y comentarios

**HU-10 · Calificar y comentar un producto en una tienda** — RF13, RF14, RF16, RF18 · OB-05 · M

Como consumidor registrado, quiero calificar con 1 a 5 estrellas y comentar un producto en una tienda en una sola acción, para compartir mi experiencia completa y ayudar a otros a elegir dónde comprar.

- La calificación y el comentario se registran al mismo tiempo: no puedo calificar sin comentar ni comentar sin calificar.
- 4 estrellas con un comentario de 10 a 500 caracteres (sin contar espacios al inicio y al final) se registran con su fecha; 0 o 6 estrellas, o 9 o 501 caracteres, devuelven 400.
- Sin sesión recibo 401; con rol ADMIN o SUPPLIER, 403.
- Si el producto o la tienda están inactivos, no puedo opinar (409); si el producto, la tienda o el par producto-tienda sin precio no existen, tampoco (404).
- Opinar de nuevo sobre el mismo producto en la misma tienda devuelve 409 y el promedio no cambia.

**HU-11 · Corregir mi opinión** — RF19 · OB-05 · S

Como consumidor registrado, quiero modificar mi calificación y mi comentario durante el día siguiente a registrarlos, para corregir un error sin romper la regla de una sola opinión por producto y tienda.

- Dentro de las 24 horas el puntaje y el texto se actualizan y el promedio se recalcula.
- Pasado ese plazo el cambio se rechaza con un mensaje claro (código propuesto: 409).
- Solo el autor puede modificarla.
- El plazo cuenta desde el registro y no se reinicia con cada edición (por confirmar).

**HU-12 · Ver mi opinión** — RF20 · OB-05 · S

Como consumidor registrado, quiero ver la calificación y el comentario que ya registré de un producto en una tienda, para no intentar opinar dos veces y saber si todavía puedo corregirlos.

- Si ya opiné, veo mi puntaje y mi comentario.
- Si no, la API responde 204 y veo las estrellas vacías.
- Si mi rol no es USER, no se me muestra el formulario.

**HU-13 · Ver promedios** — RF15 · OB-05 · M

Como visitante, quiero ver el promedio y la cantidad de calificaciones de un producto, de una tienda y de un producto en una tienda, para saber qué opinan otros antes de comprar.

- Calificaciones 5 y 3 dan promedio 4,0 con cantidad 2.
- Sin calificaciones: promedio nulo y cantidad 0, sin error 500.
- El promedio de una tienda se deriva de las calificaciones de sus productos.
- Lo puedo ver sin iniciar sesión.

**HU-14 · Leer comentarios** — RF21 · OB-05 · S

Como visitante, quiero leer los comentarios de un producto en una tienda, de los más recientes a los más antiguos, para informarme sin cargar todo de golpe.

- Veo páginas de máximo 50 comentarios, con «Ver más».
- Los comentarios OCULTO y ELIMINADO no aparecen.
- La respuesta no incluye el estado ni el correo del autor.

**HU-15 · Eliminar mi comentario** — RF22 · OB-05 · S

Como consumidor registrado, quiero eliminar un comentario mío, para retirar algo que escribí por error o que ya no quiero publicar.

- Solo el autor puede eliminarlo, en cualquier momento mientras no esté OCULTO (un comentario oculto por el administrador no se puede eliminar); otro usuario recibe 403.
- Intentar eliminar un comentario OCULTO devuelve 409 con un mensaje claro y el comentario sigue OCULTO; eliminar uno ya eliminado devuelve 204 sin cambios.
- El comentario queda en estado ELIMINADO (borrado lógico) y desaparece del listado.
- Después de eliminarlo no puedo volver a comentar ese producto en esa tienda.
- Mi calificación se conserva y los reportes pendientes del comentario siguen PENDIENTE para el administrador (por confirmar).

**HU-16 · Reportar un comentario** — RF17 · OB-05 · M

Como consumidor registrado, quiero reportar un comentario inapropiado indicando la razón y un detalle, para que lo revisen y salga si incumple las normas.

- La razón es obligatoria; el detalle es obligatorio si la razón es OTRO.
- El reporte nace PENDIENTE y el comentario pasa a REPORTADO y sigue visible. Reportar sin razón, o con razón OTRO sin detalle, devuelve 400; reportar el propio comentario devuelve 400; reportar un comentario oculto, eliminado o inexistente devuelve 404.
- Repetir mi reporte responde 204 sin duplicarlo; no puedo reportar mi propio comentario.

### Fase 2: seguimiento y avisos

**HU-17 · Seguir un producto** — RF23 · OB-01, OB-02 · S

Como consumidor registrado, quiero seguir un producto, para enterarme cuando baje de precio sin revisarlo cada día.

- Seguirlo dos veces devuelve 409 con un mensaje claro.
- Sin sesión, el botón me lleva a /login.
- Solo yo veo y modifico mis seguimientos.

**HU-18 · Definir mi alerta** — RF24 · OB-02 · S

Como consumidor registrado, quiero fijar un precio objetivo o dejar la alerta ante cualquier reducción, para que me avisen solo cuando me convenga.

- Un precio objetivo de 0 o negativo devuelve 400.
- Sin precio objetivo, cualquier reducción cuenta.
- Puedo cambiar o quitar el objetivo en cualquier momento.

**HU-19 · Pausar un aviso** — RF25 · OB-01 · S

Como consumidor que sigue un producto, quiero activar o desactivar su aviso, para pausar las notificaciones sin perder el seguimiento.

- Con el aviso desactivado, una baja de precio no genera aviso.
- Reactivarlo no genera avisos de bajas ocurridas mientras estuvo desactivado (por confirmar).

**HU-20 · Ver lo que sigo** — RF26 (DEBERÍA) · OB-01 · S

Como consumidor registrado, quiero ver la lista de productos que sigo con su precio objetivo y si el aviso está activo, para gestionarlos en un solo lugar.

- La lista muestra solo mis seguimientos y la ruta /seguidos está protegida.
- Si no sigo nada, veo un mensaje de lista vacía.

**HU-21 · Recibir un aviso de baja** — RF27 · OB-02 · L

Como consumidor que sigue un producto, quiero recibir un aviso dentro de la plataforma cuando su precio baje, para aprovechar la oferta antes de que cambie.

- Una baja de $6.000 a $4.800 con objetivo de $5.000 genera un solo aviso.
- Sin precio anterior, con el aviso desactivado o con objetivo no alcanzado, no se genera aviso.
- Correr la revisión dos veces seguidas no duplica avisos.
- La revisión corre cada 24 horas\* y lee la tabla de precios, porque el scraper inserta directo en la base de datos.

**HU-22 · Ver mis avisos** — RF27, RF53, RF54 · OB-02 · M

Como consumidor registrado, quiero ver en el encabezado cuántos avisos no he leído y marcarlos como leídos, para saber qué cambió desde mi última visita.

- El contador baja al marcar un aviso y solo aparece con sesión iniciada.
- Cada aviso muestra producto, tienda, precio anterior, precio nuevo y fecha.

**HU-23 · Recibir un correo de baja** — RF28 (DEBERÍA) · OB-02 · M

Como consumidor que sigue un producto, quiero recibir un correo cuando su precio baje, para enterarme aunque no abra la plataforma.

- Llega al correo registrado con los mismos datos del aviso.
- Si el envío falla, el aviso en la plataforma existe igual y no se duplica.
- Es la única historia que puede pasar a la siguiente fase si no alcanza el tiempo.

### Fase 3: comparadores

Además de los criterios comunes, en estas historias cada nueva sección se agrega como categoría raíz de la jerarquía existente, sin cambiar el esquema base (RNF04, RN-05); cada precio cargado tiene valor, fecha y fuente (RN-01) y, cuando la fuente la registró, la página del producto en la tienda (RF57); y las búsquedas y comparaciones de supermercados siguen funcionando igual.

**HU-28 · Comparar precios de droguería** — RF52 · OB-04 · M

Como visitante, quiero comparar el precio de un producto de droguería entre varias droguerías, para comprar donde esté más barato sin tener que recorrerlas.

- Un producto de droguería vendido en 2 o más droguerías muestra el precio de cada una con su fecha.
- Se resalta la droguería con el menor precio más reciente y los precios se ordenan de menor a mayor (RF08, RF09).
- Solo se compara el mismo producto, identificado por su EAN: «Acetaminofén 500 mg x 10» no se compara con «Acetaminofén 500 mg x 100» (RN-02, DP-27).
- Si el mismo producto también se vende en un supermercado, ese precio aparece en la misma comparación (RF07, DP-25).
- Cada precio con página registrada muestra el enlace a la tienda; uno sin página se muestra sin enlace (RF57).

**HU-29 · Explorar la sección de droguería** — RF06, RF11, RF52 · OB-04 · S

Como visitante, quiero entrar a la sección de droguería y ver sus categorías, para encontrar productos cuando no sé el nombre exacto.

- Droguería aparece como categoría raíz, con sus categorías.
- La sección muestra los productos activos que tienen precio en al menos una droguería, aunque estén clasificados en otro dominio (DP-26).
- Elegir una categoría de Droguería lista solo sus productos activos.
- El buscador ofrece únicamente categorías y tiendas activas de esa sección; quitar la categoría conserva la sección y sus filtros compatibles.
- Combinar la sección con texto, tienda o rango de precio filtra por todo a la vez (HU-06a, HU-06b).

**HU-30 · Comparar precios de cuidado facial** — RF58 · OB-04 · M

Como visitante, quiero comparar el precio de un producto de cuidado facial entre varias tiendas, para elegir dónde comprarlo según mi presupuesto.

- Un producto de cuidado facial vendido en 2 o más tiendas muestra el precio de cada una con su fecha.
- Se resalta la tienda con el menor precio más reciente y los precios se ordenan de menor a mayor (RF08, RF09).
- Solo se compara el mismo producto comercial y presentación: un limpiador de 150 ml no se compara con uno de 400 ml (RN-02, DP-27).
- Una tienda autorizada como BellaPiel puede resolver un SKU local mediante equivalencia o coincidencia secundaria controlada hacia un maestro comercial existente; no crea otro producto por semejanza de nombre ni acepta una imagen como prueba suficiente de EAN.
- Cada precio con página registrada muestra el enlace a la tienda; uno sin página se muestra sin enlace (RF57).

**HU-31 · Explorar la sección de cuidado facial** — RF06, RF11, RF58 · OB-04 · S

Como visitante, quiero entrar a la sección de cuidado facial y ver sus categorías, para descubrir productos según lo que necesito.

- Cuidado facial aparece como categoría raíz, con sus categorías.
- La sección muestra los productos activos que tienen precio en al menos una tienda de cuidado facial (DP-26).
- Elegir una categoría de Cuidado facial lista solo sus productos activos.
- El buscador ofrece únicamente categorías y tiendas activas de esa sección; quitar la categoría conserva la sección y sus filtros compatibles.
- Combinar la sección con texto, tienda o rango de precio filtra por todo a la vez (HU-06a, HU-06b).

Las historias HU-32 a HU-34 quedan en backlog. SCRUM-295 y HT-28 cubren durante el Sprint 4 la prueba de datos y el scraper de repuestos para automóviles (DP-31 y DP-50).

**HU-32 · Comparar un repuesto por su referencia** — RF55 · OB-04 · M

Como visitante, quiero comparar el precio de un repuesto de automóvil entre varias tiendas usando su referencia del fabricante, para estar seguro de que comparo exactamente la misma pieza.

- Un repuesto con la misma marca y referencia en dos tiendas muestra los dos precios, cada uno con su fecha (RN-32).
- Dos repuestos con nombre parecido y distinta referencia, o con la misma referencia y distinta marca, no se comparan entre sí.
- La marca y la referencia se muestran junto al nombre del repuesto en la comparación.
- Un repuesto sin referencia registrada no entra en la comparación (por confirmar, DP-28).
- La comparación muestra un aviso de que la plataforma no verifica la compatibilidad con un modelo de automóvil (DP-20).

**HU-33 · Buscar un repuesto por su referencia** — RF05, RF55 · OB-01, OB-04 · S

Como visitante, quiero buscar un repuesto escribiendo su referencia o código del fabricante, para encontrar la pieza exacta sin depender de cómo la nombra cada tienda.

- Escribir una referencia completa devuelve los repuestos con esa referencia, sin distinguir mayúsculas.
- Lo que escribe el usuario se normaliza con la misma regla que la referencia guardada, solo letras y números en mayúsculas: «ABC-123» y «abc 123», o «0451.103.318» y «0 451 103 318», encuentran lo mismo (RN-32).
- Sin coincidencias veo «No se encontraron productos» y no un error.

**HU-34 · Explorar la sección de repuestos** — RF11, RF55 · OB-04 · S

Como visitante, quiero entrar a la sección de repuestos para automóviles y ver sus categorías, para encontrar la pieza que busco por tipo.

- Repuestos aparece como categoría raíz, con sus categorías.
- La sección muestra los productos activos que tienen precio en al menos una tienda de repuestos (DP-26).
- Elegir una categoría de Repuestos lista solo sus productos activos.
- Combinar la sección con texto, tienda o rango de precio filtra por todo a la vez (HU-06a, HU-06b).

### Historias de búsqueda y vigencia

HU-35 (SCRUM-305) se retoma en el Sprint 4 después de que el flujo de pagos funcione de punta a punta. HU-36 permanece en backlog, según DP-38.

**HU-35 · Filtrar por marca** — RF06 (completa el filtro por marca que no cubrieron HU-06a ni HU-06b) · OB-01, OB-02 · S

Como visitante, quiero filtrar los resultados por marca, para quedarme solo con los productos de las marcas que me interesan.

- Filtrar por una marca deja solo los productos de esa marca.
- El filtro se resuelve en el backend, no con una manipulación visual del cliente.
- Combinado con nombre, categoría, tienda y rango de precio (HU-06a y HU-06b), el resultado cumple todos los filtros a la vez y conserva la paginación.
- Un producto sin marca no aparece cuando se filtra por marca, y sí aparece cuando no hay filtro de marca.

**HU-36 · Vigencia del precio según su última confirmación** — RF07, RF08, RF10 · OB-02, OB-03 · por estimar

Como visitante, quiero que un precio que la tienda dejó de publicar no cuente como vigente, para no comparar con ofertas que ya no existen.

- Se define una ventana de vigencia según la fecha de la última confirmación del precio (configurable).
- Un precio cuya última confirmación es más antigua que la ventana no cuenta como vigente, ni en la búsqueda ni en el comparador; la regla se aplica igual en los dos.
- Se retoma cuando el scraper corra a diario de forma automática; antes de estimar se decide la duración de la ventana y qué se muestra de un precio vencido (DP-38).

### Sprint 4 — IA especializada

Las cuatro historias comparten el chat temporal de RF59 y la política de acceso y cuotas de los planes. Los criterios se aplican a cada asistente por sección.

**HU-37 · Recomendar dónde comprar una lista** — RF29, RF59 · OB-01, OB-02, OB-07 · por estimar

Como consumidor con sesión, quiero enviar mi lista al asistente de Supermercados para saber dónde encontrar mejores precios.

- El avatar de Supermercados abre el chat de esa sección. Una lista con coincidencias devuelve opciones respaldadas por el catálogo y señala los artículos que no puede cotizar.
- Al cerrar el chat o recargar, se pierde la conversación. El consumo de la cuota se conserva hasta el reinicio mensual.

**HU-38 · Sugerir recetas con productos del catálogo** — RF30, RF59 · OB-01, OB-02 · por estimar

Como consumidor con sesión, quiero pedir una receta para saber cómo preparar el plato y qué ingredientes comprar.

- El asistente de Supermercados muestra pasos de preparación e identifica los ingredientes disponibles en el catálogo.
- Si un ingrediente no tiene coincidencia, el asistente informa que no está disponible en lugar de atribuirle un precio.

**HU-39 · Consultar productos de droguería** — RF31, RF59, RF63 · OB-04 · por estimar

Como consumidor con sesión, quiero consultar productos y precios de droguería para orientar una compra.

- La respuesta muestra el aviso de orientación incluso si la consulta pide un diagnóstico o un producto que requiere fórmula médica.
- Los productos que requieren fórmula médica se excluyen del resultado incluso cuando están en el catálogo y se solicitan por nombre.

**HU-40 · Recibir recomendaciones de cuidado facial** — RF32, RF59 · OB-04 · por estimar

Como consumidor con sesión, quiero buscar productos de cuidado facial según mi tipo de piel, necesidades y presupuesto.

- El asistente de Cuidado facial muestra coincidencias del catálogo dentro del presupuesto, cuando existen, e indica cuando no encuentra una opción.
- La respuesta incluye el aviso de que la recomendación es orientativa y no sustituye a un profesional de la salud.

### Sprint 4 — Planes y suscripción

**HU-41 · Consultar mi plan y sus límites** — RF64 · OB-07 · por estimar

Como consumidor con sesión, quiero ver mi plan vigente, los límites de IA y seguimiento y mi consumo para saber qué funciones puedo usar.

- En Gratuito se muestran 4 consultas al mes, 200 caracteres por mensaje completo y hasta 5 productos seguidos. El consumidor ve cuántas consultas y productos seguidos ha usado.
- En Premium se muestran los límites ampliados vigentes y su consumo.

**HU-42 · Adquirir el plan Premium** — RF62 · OB-07 · por estimar

Como consumidor con sesión, quiero suscribirme a Premium para ampliar mis límites de IA y seguimiento.

- Una confirmación válida de la pasarela activa Premium para la cuenta correspondiente y sus límites ampliados.
- Un intento pendiente, cancelado o rechazado conserva el plan vigente. Repetir una confirmación del mismo pago mantiene una única activación.

**HU-43 · Conocer el bloqueo al superar un límite gratuito** — RF60, RF61, RF64 · OB-07 · por estimar

Como consumidor con plan Gratuito, quiero conocer qué límite alcancé y cómo pasar a Premium cuando el sistema bloquea una acción.

- Se permiten la cuarta consulta del mes, un mensaje completo de 200 caracteres y el quinto producto seguido. Se bloquean la quinta consulta del mes, los mensajes completos de 201 caracteres y el sexto seguimiento simultáneo. Al reiniciarse el contador mensual vuelve a estar disponible la cuota de cuatro consultas.
- El aviso identifica el límite y ofrece adquirir Premium. Cerrar el chat o cambiar de sección no restaura la cuota.
- La comparación sigue disponible de forma gratuita. Un visitante puede comparar y recibe una invitación a iniciar sesión al intentar usar IA, seguir, comentar o calificar.

### Sprint 4 — Panel administrativo

**HU-44 · Gestionar los clientes** — RF40, RF47, RF48 · OB-03, OB-05 · por estimar

Como administrador, quiero listar, crear, consultar, editar, activar, desactivar y eliminar cuentas de clientes para administrar su acceso.

- El listado general refleja las operaciones permitidas y sus cambios de estado. Una cuenta bloqueada o desactivada no accede a la plataforma.
- Cada cambio queda en el historial con fecha y responsable. Un visitante o una cuenta con otro rol no accede al panel ni a sus endpoints.

**HU-45 · Visualizar la actividad de clientes** — RF65 · OB-03 · por estimar

Como administrador, quiero ver cuántos clientes están activos en tiempo real y los promedios de actividad para conocer el uso de la plataforma.

- La vista muestra el conteo y gráficas con el periodo de los promedios. Las métricas coinciden con los datos de prueba según la definición de actividad y frecuencia que se acuerden en DP-54.

**HU-46 · Consultar los productos más seguidos y más buscados** — RF66 · OB-03 · por estimar

Como administrador, quiero consultar dos rankings de productos para conocer qué productos concentran los seguimientos y las búsquedas.

- Cada ranking muestra el producto y su conteo, ordenado de mayor a menor. El ranking de búsquedas usa el registro de búsquedas y la regla de atribución acordada en DP-54.

HT-26 cubre el CRUD del catálogo (RF43 y RF67) y comienza con una evaluación de cuatro horas de una herramienta externa. HT-27 cubre la visualización de series históricas de precios por producto y tienda (RF44 y RF68). La ampliación del panel se identifica como backlog en RF69 a RF72 y en la decisión DP-55.

### Verificación INVEST

La tabla identifica las dependencias y decisiones que afectan la estimación y la verificación de las historias.

| Historia | Principio | Observación | Acción |
| --- | --- | --- | --- |
| HU-09 a HU-12, HU-15 a HU-23, HU-25 y HU-26 | Independiente | Requieren sesión: dependen de HU-02 y del PR de seguridad | Dependencia aceptada; el orden de merge ya la refleja |
| HU-11, HU-12, HU-15, HU-16 | Independiente | Necesitan HU-10: sin opinión no hay comentario que corregir, ver, eliminar o reportar | Implementar HU-10 primero |
| HU-10 | Pequeña | Une calificar y comentar por decisión del equipo (RN-30) | Mantener unidas; no dividir por tipo de dato |
| HU-21 y HU-23 | Independiente | Comparten el mismo evento y solo cambia el canal | Se mantienen separadas: el correo es DEBERÍA y puede aplazarse, el aviso en la plataforma no |
| HU-21 | Pequeña | Es L: detecta, compara y evita duplicados | Dividir en detección y generación del aviso |
| HU-21 a HU-23 | Estimable | Dependen del scraper programado y de las credenciales de correo | Confirmar ambas antes de estimar |
| HU-03 | Pequeña | Son 8 secciones | Si se retrasa, dividir en 2 historias |
| HU-28 a HU-31 | Estimable | Dependen de la selección de tiendas de droguería y cuidado facial | Seleccionar las tiendas antes de estimar |
| HU-32 a HU-34 | Estimable | Dependen de la prueba de datos de repuestos (DP-31) y del caso sin referencia (DP-28) | Hacer la prueba de datos y resolver DP-28 antes de estimar |
| HU-35 | Independiente | Se construye sobre la búsqueda paginada de HU-06b | Implementar después de HU-06b |
| HU-36 | Estimable | Faltan la duración de la ventana y qué se muestra de un precio vencido; necesita el scraper diario automático | Decidir DP-38 antes de estimar |
| HU-37 a HU-40 | Independiente | Comparten el cliente y el chat temporal. Droguería depende del dato verificable de venta bajo fórmula | Acordar interfaz móvil y fuente de clasificación |
| HU-41 a HU-43 | Estimable | Dependen de la elección de pasarela y del modelo de cuotas | Resolver HT-22 y HT-23 |
| HU-44 a HU-46 | Estimable | Dependen del alcance operativo del CRUD de clientes, definiciones de métricas y fuente de datos | Acordar DP-54 y evaluar la herramienta de HT-26 |

### Verificación SMART

| Dimensión | Estado |
| --- | --- |
| Específico | Cumple: cada criterio nombra rol, acción y resultado |
| Medible | Cumple: usa límites y resultados comprobables, incluidos 4 consultas al mes, 200 caracteres por mensaje completo y 5 seguimientos para el plan Gratuito |
| Alcanzable | HU-21 y HU-23 dependen del scraper programado y del correo. Las nuevas historias tienen dependencias abiertas de pasarela, cuotas, clasificación y métricas |
| Relevante | Cumple: cada historia lleva su objetivo (OB) |
| Temporal | Débil: cada historia tiene fase, pero no fecha; falta fijar la fecha de entrega de cada fase |

## 6. Reglas de negocio

| ID | Tipo | Regla | Estado |
| --- | --- | --- | --- |
| RN-01 | Validación | Todo precio publicado tiene valor positivo, fecha de registro, fuente y una base comparable. Se muestra su última confirmación; para un genérico por peso, el valor publicado corresponde a 1 kg y el total capturado se conserva en staging. | Definida |
| RN-02 | Restricción | Solo se comparan precios del mismo producto y presentación entre establecimientos. La identidad comercial es GTIN comparable y tamaño exacto; una equivalencia local no omite sus validaciones. Los productos por peso pueden compararse mediante un genérico curado de 1 kg, solo con SKU equivalentes y peso/base de precio comprobados. Una contradicción no se resuelve por mayoría ni por conversión supuesta G↔ML. | Definida |
| RN-03 | Cálculo | La mejor opción de compra es el establecimiento cuyo precio registrado más reciente sea el menor. | Definida |
| RN-04 | Restricción | Los precios son informativos y no vinculantes: no se garantiza su actualización en tiempo real. | Definida |
| RN-05 | Validación | Las categorías tienen dos niveles: sección raíz y categoría hoja. Cada establecimiento pertenece a una raíz y cada producto/objetivo a una hoja; cambiar una sección de búsqueda limita sus opciones, sin reclasificar automáticamente productos históricos. | Definida |
| RN-06 | Restricción | Los supermercados comparables en la etapa inicial son D1, Jumbo, Éxito y Olímpica; otros establecimientos se incorporan por fases. | Definida |
| RN-07 | Autorización | Solo un consumidor con sesión (rol USER) puede usar IA, calificar, comentar, reportar comentarios y seguir productos. | Definida |
| RN-08 | Validación | Cada cuenta se identifica con un correo único, también cuando se inicia sesión con Google, y la recuperación de contraseña solo se envía a ese correo. | Definida |
| RN-09 | Autorización | Cada rol (consumidor, proveedor, administrador) accede solo a las funciones de su rol. | Definida |
| RN-10 | Restricción | Una cuenta bloqueada/desactivada no puede acceder a la plataforma. | Definida |
| RN-11 | Validación | El puntaje de una calificación va de 1 a 5 estrellas y toda opinión conserva su fecha de registro. | Definida |
| RN-12 | Restricción | Un consumidor solo puede calificar una vez cada producto en cada tienda. | Definida |
| RN-13 | Cálculo | El promedio de un producto, de una tienda o de un producto en una tienda es la media aritmética de las calificaciones registradas; la tienda no se califica directamente: su promedio se deriva de las calificaciones de sus productos. El promedio se calcula sobre todas las calificaciones registradas, sin importar el estado de su comentario (PUBLICADO, REPORTADO, CONSERVADO, OCULTO o ELIMINADO): ocultar un comentario es una decisión sobre el texto, no sobre el puntaje. | Definida |
| RN-14 | Transición | Un comentario pasa de PUBLICADO o CONSERVADO a REPORTADO cuando recibe un reporte; el administrador decide si queda CONSERVADO u OCULTO; el autor puede pasarlo a ELIMINADO mientras no esté OCULTO (un comentario oculto por el administrador no puede ser eliminado por el autor; permanece OCULTO; el intento responde 409). Solo OCULTO y ELIMINADO dejan de ser visibles. | Definida |
| RN-15 | Restricción | Solo se notifica sobre productos seguidos con la notificación activa. | Definida |
| RN-16 | Cálculo | SI el consumidor definió un precio objetivo, ENTONCES se notifica cuando el precio sea menor o igual a ese objetivo, sin embargo si el precio ya es menor al objetivo no se notificará como baja en el momento; SI no lo definió, ENTONCES se notifica ante cualquier reducción del precio. | Definida |
| RN-17 | Restricción | Las recomendaciones sobre droguería y cuidado facial son orientativas: no constituyen diagnóstico ni prescripción ni sustituyen a un profesional de la salud. | Definida |
| RN-18 | Restricción | Las recomendaciones del servicio solo incluyen productos y precios registrados en el catálogo. | Definida |
| RN-19 | Transición | Un proveedor pasa de PENDIENTE a APROBADO o RECHAZADO por decisión del administrador; solo un proveedor aprobado publica. | Definida |
| RN-20 | Transición | Un producto de proveedor se publica y es visible de inmediato (PUBLICADO); el administrador lo revisa después y decide conservarlo (CONSERVADO) o darlo de baja (DADO DE BAJA); un producto dado de baja deja de ser visible. | Definida |
| RN-21 | Autorización | Un proveedor solo gestiona sus propios productos. | Definida |
| RN-22 | Validación | Todo producto de proveedor registra precio, disponibilidad y fecha de última actualización. | Definida |
| RN-23 | Restricción | Todo cambio administrativo queda en un historial con fecha y responsable. | Definida |
| RN-24 | Restricción | Solo se puede calificar o comentar un producto en una tienda si ambos están activos y existe un precio registrado de ese producto en esa tienda. | Definida |
| RN-25 | Restricción | Un consumidor puede editar su comentario durante el plazo de edición y eliminarlo en cualquier momento; la eliminación es lógica (el comentario queda en estado ELIMINADO) y le impide volver a comentar ese producto en esa tienda. | Definida |
| RN-26 | Restricción | Un consumidor solo puede reportar una vez cada comentario y no puede reportar el suyo; reportar un comentario oculto, eliminado o inexistente responde 404. | Definida |
| RN-27 | Validación | Todo reporte tiene razón, detalle (obligatorio si la razón es OTRO, máximo 200 caracteres) y estado: PENDIENTE o RESUELTO. Las razones disponibles son SPAM, LENGUAJE\_OFENSIVO, INFORMACION\_FALSA y OTRO (catálogo provisional). | Definida; el catálogo de razones es provisional |
| RN-28 | Validación | El texto de un comentario tiene entre 10 y 500 caracteres, sin contar espacios al inicio ni al final; cada consumidor solo puede comentar una vez cada producto en cada tienda. | Definida |
| RN-29 | Restricción | Una calificación y su comentario pueden modificarse por su autor hasta 1 día (24 horas) después de registrados; pasado ese plazo quedan definitivos. Solo el autor puede editar, y solo mientras el comentario siga PUBLICADO (uno reportado, oculto o eliminado no se reescribe); fuera de esas condiciones se responde 409. | Definida; el conteo del plazo está por confirmar |
| RN-30 | Restricción | Calificar y comentar un producto en una tienda son una sola acción: no se puede calificar sin comentar ni comentar sin calificar, y ambos se registran al mismo tiempo. El id de una opinión es el de su calificación y es el que usan editar, eliminar y reportar. | Definida |
| RN-31 | Restricción | El estado del comentario (incluido OCULTO) no afecta el promedio del producto ni del establecimiento; retirar una calificación del promedio exige una razón propia de la calificación (indicio de fraude), no la moderación de su comentario. | Definida |
| RN-32 | Restricción | Un repuesto solo se compara con el mismo repuesto, identificado por su marca y su referencia o código del fabricante, normalizada a solo letras y números en mayúsculas; dos repuestos con distinta marca o distinta referencia no se comparan. | Definida |
| RN-33 | Restricción | El servicio de IA de droguería excluye antes de enviar al modelo los productos que requieren fórmula médica. El filtro es independiente del prompt y de la configuración del scraper. Toda respuesta de droguería advierte que el asistente no sustituye el diagnóstico, la prescripción ni la orientación de un profesional de la salud. | Definida. Fuente de clasificación pendiente (DP-56) |
| RN-34 | Restricción | El dominio Repuestos se limita a piezas de automóviles. Los repuestos de motocicletas quedan excluidos en el alcance actual. | Definida |
| RN-35 | Acceso | Buscar y comparar precios es gratuito para visitantes y consumidores de cualquier plan. Las funciones de IA, seguimiento y registro de opiniones se rigen por RN-07. | Definida |
| RN-36 | Cuotas | El plan Gratuito permite hasta 4 consultas de IA por cuenta al mes, 200 caracteres por mensaje completo y 5 productos seguidos simultáneamente. El límite de 200 incluye todos los artículos de la lista en conjunto. Toda acción que exceda un tope se bloquea y ofrece pasar a Premium. El contador se reinicia mensualmente y se conserva al cambiar de sección, cerrar el chat o recargar. | Topes gratuitos y reinicio mensual definidos. Topes Premium pendientes (DP-52) |
| RN-37 | Suscripción | El pago por la pasarela externa cubre el acceso a funciones Premium de PreciosCo. La cuenta adquiere los límites ampliados cuando el backend valida la confirmación del pago correspondiente. La compra de productos, los pagos a tiendas y los domicilios quedan excluidos. | Definida |

## 7. Requisitos funcionales

Los requisitos se agrupan por módulo y conservan su numeración. DEBE indica obligación y DEBERÍA una función que puede aplazarse. La columna Fase señala la fase del producto y, cuando corresponde, la prioridad de Sprint 4 o backlog.

### Gestión de usuarios

| ID | Nombre | Actor | Fase | Requisito |
| --- | --- | --- | --- | --- |
| RF01 | Registrar cuenta | ACT-01 | 1 | El sistema DEBE permitir al visitante registrar una cuenta con nombre, correo y contraseña. |
| RF02 | Iniciar sesión | ACT-02, 03, 04 | 1 | El sistema DEBE permitir iniciar sesión con correo y contraseña a toda cuenta registrada y no bloqueada. |
| RF03 | Recuperar contraseña | ACT-02, ACT-06 | 2 | El sistema DEBE enviar al correo registrado un enlace de recuperación válido por 30 minutos\* cuando el consumidor lo solicite. |
| RF04 | Modificar perfil | ACT-02 | 2 | El sistema DEBERÍA permitir al consumidor modificar su nombre y correo. |
| RF49 | Cerrar sesión | ACT-02, 03, 04 | 1 | El sistema DEBE permitir al consumidor cerrar su sesión e invalidar su token. |
| RF56 | Iniciar sesión con Google | ACT-01, ACT-02, ACT-09 | 2 | El sistema DEBE permitir iniciar sesión con una cuenta de Google y crear la cuenta del consumidor si su correo no está registrado. |

### Búsqueda y comparación

| ID | Nombre | Actor | Fase | Requisito |
| --- | --- | --- | --- | --- |
| RF05 | Buscar productos | ACT-01, 02 | 1 | El sistema DEBE permitir buscar productos por nombre, marca o categoría, y los repuestos también por su referencia o código del fabricante. |
| RF06 | Filtrar resultados | ACT-01, 02 | 1 | El sistema DEBE filtrar por precio, establecimiento, marca y categoría. Con una sección seleccionada, las opciones de categoría y tienda DEBEN limitarse a esa sección; cambiarla limpia filtros incompatibles y reinicia la página. |
| RF07 | Comparar precios | ACT-01, 02 | 1 | El sistema DEBE mostrar, para un producto, el precio registrado en cada establecimiento donde esté disponible. |
| RF08 | Identificar mejor opción | ACT-01, 02 | 1 | El sistema DEBE resaltar el establecimiento con el menor precio registrado más reciente del producto consultado. |
| RF09 | Ordenar resultados | ACT-01, 02 | 1 | El comparador DEBE mostrar los precios de un producto de menor a mayor. La búsqueda DEBE ordenar productos por porcentaje de ahorro, con desempate por nombre e ID (DP-36, DP-39); no exige un selector de orden en la interfaz. |
| RF10 | Mostrar fecha de precio | ACT-01, 02 | 1 | El sistema DEBE mostrar junto a cada precio la fecha en que la fuente lo confirmó por última vez. |
| RF11 | Navegar por categorías | ACT-01, 02 | 1 | El sistema DEBE permitir consultar productos por sección y categoría hoja. La opción «Todas» conserva la sección elegida; un enlace de sección inexistente no debe convertirse silenciosamente en una búsqueda global. |
| RF12 | Comparar cadenas iniciales | ACT-01, 02 | 1 | El sistema DEBE incluir en la comparación los precios de D1, Jumbo, Éxito y Olímpica. |
| RF50 | Mostrar página de inicio | ACT-01 | 1 | El sistema DEBE mostrar una portada con presentación del servicio, datos y ejemplos reales disponibles y acceso a las secciones comerciales. DEBE omitir bloques sin respaldo y enlazar cada sección con su búsqueda correspondiente, sin testimonios ni IA ficticios. |
| RF51 | Cargar precios de fuente externa | ACT-08 | 1 | El sistema DEBE capturar observaciones en staging y validar identidad/presentación antes de publicar precios. Procesa pendientes de corridas cerradas o legado sin corrida, deduplica por tienda/identidad/corrida y mantiene historial por periodos. Un precio igual refresca su confirmación; las contradicciones quedan para revisión sin registrar precios. |
| RF52 | Comparar droguerías | ACT-01, 02 | 3 | El sistema DEBE incluir en la comparación los precios de droguerías. |
| RF55 | Comparar repuestos | ACT-01, 02 | 3 (backlog) | Si SCRUM-295 confirma la viabilidad de la comparación, el sistema DEBE incluir los precios de repuestos de automóviles con la misma marca y referencia del fabricante (RN-32 y RN-34). |
| RF57 | Enlazar a la tienda | ACT-01, 02 | 1 | El sistema DEBE mostrar junto a cada precio un enlace a la página del producto en la tienda, cuando la fuente la haya registrado. |
| RF58 | Comparar cuidado facial | ACT-01, 02 | 3 | El sistema DEBE incluir en la comparación los precios de tiendas de cuidado facial. |

### Calificaciones y comentarios

| ID | Nombre | Actor | Fase | Requisito |
| --- | --- | --- | --- | --- |
| RF13 | Calificar producto en tienda | ACT-02 | 2 | El sistema DEBE permitir al consumidor calificar con 1 a 5 estrellas un producto en una tienda, registrando junto con la calificación su comentario (RN-30). |
| RF14 | Comentar producto en tienda | ACT-02 | 2 | El sistema DEBE permitir al consumidor publicar un comentario de 10 a 500 caracteres sobre un producto en una tienda, registrado junto con su calificación (RN-30). |
| RF15 | Calcular promedios | ACT-01, 02 | 2 | El sistema DEBE calcular y mostrar el promedio y la cantidad de calificaciones de un producto, de una tienda y de un producto en una tienda. |
| RF16 | Registrar fecha de opinión | ACT-02 | 2 | El sistema DEBE registrar y mostrar la fecha de cada calificación y comentario. |
| RF17 | Reportar comentario | ACT-02 | 2 | El sistema DEBE permitir al consumidor reportar un comentario indicando la razón y un detalle. |
| RF18 | Impedir calificación repetida | ACT-02 | 2 | El sistema DEBE rechazar la calificación y el comentario de un producto en una tienda que el consumidor ya calificó. |
| RF19 | Modificar opinión | ACT-02 | 2 | El sistema DEBE permitir al consumidor modificar su calificación y su comentario dentro de las 24 horas siguientes a su registro. |
| RF20 | Consultar mi opinión | ACT-02 | 2 | El sistema DEBERÍA permitir al consumidor consultar la calificación y el comentario que ya registró de un producto en una tienda. |
| RF21 | Consultar comentarios | ACT-01, 02 | 2 | El sistema DEBE permitir consultar los comentarios visibles de un producto en una tienda, paginados (máximo 50 por página) y de más recientes a más antiguos, sin mostrar el estado del comentario ni el correo de su autor. |
| RF22 | Eliminar comentario | ACT-02 | 2 | El sistema DEBE permitir al consumidor eliminar un comentario suyo. |

### Notificaciones

| ID | Nombre | Actor | Fase | Requisito |
| --- | --- | --- | --- | --- |
| RF23 | Seguir producto | ACT-02 | 2 | El sistema DEBE permitir al consumidor seguir un producto, sujeto al plan y a RF61. |
| RF24 | Definir alerta | ACT-02 | 2 | El sistema DEBE permitir definir, para un producto seguido, un precio objetivo o la alerta ante cualquier reducción. |
| RF25 | Activar o desactivar aviso | ACT-02 | 2 | El sistema DEBE permitir activar o desactivar la notificación de cada producto seguido. |
| RF26 | Listar productos seguidos | ACT-02 | 2 | El sistema DEBERÍA permitir consultar la lista de productos seguidos. |
| RF27 | Notificar baja en plataforma | ACT-07, ACT-02 | 2 | El sistema DEBE generar un aviso dentro de la plataforma cuando el precio de un producto seguido cumpla la condición de RN-16. |
| RF28 | Notificar baja por correo | ACT-07, 06, 02 | 2 | El sistema DEBERÍA enviar un correo al consumidor cuando el precio de un producto seguido cumpla la condición de RN-16. |
| RF53 | Consultar mis avisos | ACT-02 | 2 | El sistema DEBE permitir al consumidor consultar sus avisos con su cantidad de no leídos. |
| RF54 | Marcar aviso como leído | ACT-02 | 2 | El sistema DEBE permitir al consumidor marcar un aviso como leído. |

### Inteligencia artificial

| ID | Nombre | Actor | Fase | Requisito |
| --- | --- | --- | --- | --- |
| RF29 | Recomendar supermercado | ACT-02, ACT-05 | 4 | El sistema DEBE recomendar, mediante un asistente de IA, en qué supermercado comprar una lista de productos. |
| RF30 | Sugerir recetas | ACT-02, ACT-05 | 4 | El sistema DEBE sugerir, mediante un asistente de IA, recetas con su preparación e ingredientes, identificando los disponibles en el catálogo. |
| RF31 | Consultar droguería | ACT-02, ACT-05 | 4 | El sistema DEBE responder, mediante un asistente de IA, consultas sobre productos de droguería y sus precios, aplicando la exclusión de medicamentos bajo fórmula y el aviso definidos en RF63. |
| RF32 | Recomendar cuidado facial | ACT-02, ACT-05 | 4 | El sistema DEBE recomendar, mediante un asistente de IA, productos de cuidado facial según tipo de piel, necesidades y presupuesto indicados, con un aviso de que la recomendación es orientativa y no sustituye a un profesional de la salud. |
| RF59 | Abrir un chat temporal por sección | ACT-02, ACT-05 | 4 · Sprint 4 | El sistema DEBE ofrecer al consumidor con sesión avatares flotantes en el lateral derecho para Supermercados, Droguería y Cuidado facial. El avatar abre el chat temporal de su sección. El historial se pierde al cerrar el chat o recargar la página (RNF13). En móvil los avatares se reúnen en un único botón flotante. |
| RF63 | Restringir la asesoría de droguería | ACT-02, ACT-05 | 4 · Sprint 4 | El sistema DEBE excluir de las recomendaciones los medicamentos que requieren fórmula médica y mostrar en cada respuesta el aviso de RN-33. La exclusión se aplica en el servicio de IA antes de enviar productos al modelo, con independencia del prompt y del scraper. |

### Planes y suscripción

Origen: exigencia de Programación Web integrada a la planificación del Sprint 4 (DP-51).

| ID | Nombre | Actor | Fase | Requisito |
| --- | --- | --- | --- | --- |
| RF60 | Validar límites gratuitos de IA | ACT-02 | 4 · Sprint 4 | El backend DEBE permitir hasta 4 consultas de IA por cuenta al mes, con un máximo de 200 caracteres por mensaje completo, incluida la lista entera. El contador DEBE reiniciarse mensualmente. Ante un intento que exceda cualquiera de esos límites, DEBE bloquear la consulta y ofrecer Premium. |
| RF61 | Validar el límite gratuito de seguimiento | ACT-02 | 4 · Sprint 4 | El backend DEBE permitir hasta 5 productos seguidos simultáneamente en el plan Gratuito. Ante un intento de exceder el tope, DEBE bloquear el nuevo seguimiento y ofrecer Premium. |
| RF62 | Adquirir Premium | ACT-02, ACT-10 | 4 · Sprint 4 | El sistema DEBE permitir iniciar el pago de la suscripción mediante una pasarela externa, comprobar la confirmación en el backend y activar los límites ampliados de Premium solo para la cuenta correspondiente. La integración del sprint usa el entorno de prueba. |
| RF64 | Consultar plan, límites y consumo | ACT-02 | 4 · Sprint 4 | El sistema DEBE mostrar al consumidor el plan vigente, sus topes de IA y seguimiento y el consumo de las cuotas aplicables. |

### Módulo de proveedores

| ID | Nombre | Actor | Fase | Requisito |
| --- | --- | --- | --- | --- |
| RF33 | Solicitar cuenta de proveedor | ACT-03 | 5 | El sistema DEBE permitir a un establecimiento solicitar una cuenta de proveedor. |
| RF34 | Registrar datos del proveedor | ACT-03 | 5 | El sistema DEBE permitir al proveedor registrar los datos de su establecimiento. |
| RF35 | Publicar productos | ACT-03 | 5 | El sistema DEBE publicar de inmediato los productos que un proveedor aprobado registra, con marca, categoría y presentación. |
| RF36 | Registrar precio y disponibilidad | ACT-03 | 5 | El sistema DEBE permitir al proveedor aprobado registrar el precio y la disponibilidad de cada producto publicado. |
| RF37 | Modificar o eliminar productos | ACT-03 | 5 | El sistema DEBE permitir al proveedor modificar o eliminar sus productos publicados. |
| RF38 | Consultar productos publicados | ACT-03 | 5 | El sistema DEBE permitir al proveedor consultar la lista de sus productos publicados. |
| RF39 | Mostrar última actualización | ACT-03 | 5 | El sistema DEBE mostrar al proveedor la fecha de última actualización de cada uno de sus productos. |

### Panel administrativo

| ID | Nombre | Actor | Fase | Requisito |
| --- | --- | --- | --- | --- |
| RF40 | Gestionar cuentas | ACT-04 | 5 · clientes en Sprint 4 | El sistema DEBE permitir al administrador listar clientes y crear, consultar, editar, activar, desactivar y eliminar cuentas de consumidores conforme a las reglas de integridad. La consulta y modificación de cuentas de proveedores permanece en el módulo ampliado. |
| RF41 | Aprobar o rechazar proveedores | ACT-04 | 5 | El sistema DEBE permitir al administrador aprobar o rechazar solicitudes de cuenta de proveedor. |
| RF42 | Revisar productos publicados | ACT-04 | 5 | El sistema DEBE permitir al administrador conservar o dar de baja un producto publicado por un proveedor. |
| RF43 | Administrar categorías y marcas | ACT-04 | 5 · Sprint 4 | El sistema DEBE permitir al administrador listar, crear, consultar, modificar, activar, desactivar y eliminar categorías y marcas según la integridad de sus relaciones. |
| RF44 | Consultar precios registrados | ACT-04 | 5 | El sistema DEBE permitir al administrador consultar los precios registrados con su fecha y fuente. |
| RF45 | Moderar comentarios | ACT-04 | 5 (panel en backlog, moderación manual vigente) | El sistema DEBE permitir al administrador conservar u ocultar un comentario reportado, lo que resuelve sus reportes. |
| RF46 | Generar reportes | ACT-04 | 6 | El sistema DEBERÍA generar reportes con el total de usuarios, productos y búsquedas por periodo. |
| RF47 | Bloquear cuentas | ACT-04 | 5 (manual en la Fase 2) | El sistema DEBE permitir al administrador bloquear una cuenta. |
| RF48 | Consultar historial | ACT-04 | 5 | El sistema DEBE permitir al administrador consultar el historial de cambios administrativos con fecha y responsable. |
| RF65 | Consultar actividad de clientes | ACT-04 | 5 · Sprint 4 | El sistema DEBE mostrar al administrador los clientes activos en tiempo real y gráficas con los promedios de actividad por periodo (DP-54). |
| RF66 | Consultar rankings de productos | ACT-04 | 5 · Sprint 4 | El sistema DEBE registrar las búsquedas para generar un ranking de productos más buscados y mostrar al administrador ese ranking y el de productos más seguidos, con sus conteos. |
| RF67 | Administrar productos y tiendas | ACT-04 | 5 · Sprint 4 | El sistema DEBE permitir al administrador listar, crear, consultar, modificar, activar, desactivar y eliminar productos y tiendas según la integridad de sus relaciones. |
| RF68 | Visualizar el historial de precios | ACT-04 | 5 · Sprint 4 | El sistema DEBE mostrar series históricas de precios por producto y tienda, con fechas, a partir de los precios registrados. |
| RF69 | Gestionar objetivos y ofertas capturadas | ACT-04 | 5 · backlog | El sistema DEBE permitir al administrador consultar y gestionar los objetivos de captura y revisar las ofertas capturadas según sus estados y controles de publicación. |
| RF70 | Gestionar reglas de equivalencia y presentación | ACT-04 | 5 · backlog | El sistema DEBE permitir al administrador gestionar equivalencias SKU–EAN, vínculos SKU–producto y correcciones de presentación con nombres legibles y evidencia para cada recurso. |
| RF71 | Consultar el estado de las suscripciones | ACT-04 | 5 · backlog | El sistema DEBE permitir al administrador ver el plan y el estado de suscripción de los clientes. |
| RF72 | Gestionar seguimientos y calificaciones | ACT-04 | 5 · backlog | El sistema DEBE ofrecer al administrador vistas y operaciones de gestión para seguimientos y calificaciones, preservando las reglas de avisos, opiniones, promedios y auditoría. |

## 8. Requisitos no funcionales

Los requisitos de calidad definen cómo funciona el sistema. Las pruebas y la documentación se mantienen como entregables (E-01 y E-02). Los identificadores RNF11 y RNF12 permanecen retirados.

| ID | Categoría | Requisito | Criterio de verificación |
| --- | --- | --- | --- |
| RNF01 | Usabilidad | La plataforma DEBE ser adaptable a computador, tableta y celular. | Se ve sin desplazamiento horizontal en los tres tamaños |
| RNF02 | Seguridad | El sistema DEBE almacenar las contraseñas cifradas (hashing). | Ninguna contraseña en texto plano |
| RNF03 | Disponibilidad | El sistema DEBE estar publicado en un servidor accesible por internet. | Se accede por una URL pública |
| RNF04 | Escalabilidad | El sistema DEBE permitir incorporar categorías y establecimientos sin rediseñar la arquitectura base. | Agregar una categoría no cambia el esquema base |
| RNF05 | Mantenibilidad | El sistema DEBE permitir agregar fases de forma modular sobre la misma arquitectura base. | Una fase nueva no obliga a reescribir las anteriores |
| RNF06 | Calidad de datos | Las fuentes y evidencias de precios DEBEN ser identificables y revisables, respetando las restricciones de la integración. | Conservar tienda, URL cuando exista, fecha, código/SKU y evidencia de equivalencias o conflictos; un checksum o nombre de imagen no prueba la identidad física ni un acuerdo oficial con la tienda |
| RNF07 | Seguridad | El sistema DEBE firmar los tokens de sesión con una clave secreta de al menos 32 bytes y hacerlos vencer a las 24 horas. | La aplicación no arranca con una clave menor; un token vencido se rechaza |
| RNF08 | Robustez de la API | Los errores reales DEBEN conservar código y JSON uniforme mientras la conexión sea utilizable; una desconexión reconocida del cliente no debe provocar otra respuesta ni un falso error interno. | Verificar 400/401/403/404/409 y 500 real; una cancelación reconocida deja registro DEBUG sin stack trace y no oculta fallos de aplicación, base o API externa |
| RNF09 | Seguridad | El sistema DEBE mostrar los textos escritos por usuarios (comentarios y reportes) como texto plano, sin interpretarlos como HTML. | Un comentario con una etiqueta de script se ve literal |
| RNF10 | Rendimiento | La búsqueda de productos y la comparación de precios DEBEN responder en menos de 3 segundos\* en el 95% de las consultas, con hasta 20 usuarios concurrentes\*. | En 30 consultas consecutivas sobre la plataforma publicada, al menos 29 responden en menos de 3 s |
| RNF13 | Privacidad | PreciosCo DEBE mantener el historial del chat en memoria durante su uso y evitar su persistencia en almacenamiento local, base de datos o registros de aplicación. El contador de consultas se conserva por cuenta, separado del historial. | Cerrar o recargar deja el chat vacío. No aparecen mensajes en medios persistentes de PreciosCo y el contador sigue reflejando el uso previo |
| RNF14 | Integridad | El backend DEBE aplicar las validaciones de plan aun cuando las solicitudes coincidan en el tiempo y procesar de forma idempotente la confirmación de una transacción Premium. | Dos acciones simultáneas por el último cupo permiten solo una. Repetir una confirmación válida procesa una única activación |

## 9. Matrices de trazabilidad

Las matrices conectan los objetivos con los procesos, reglas, requisitos e historias. Las funciones que siguen en backlog conservan su trazabilidad.

### 9.1 Objetivo → procesos → reglas → requisitos

| Objetivo | Procesos | Reglas | Requisitos |
| --- | --- | --- | --- |
| OB-01 | P-01, P-02, P-04, P-05 | RN-01, 02, 04, 05, 06, 07, 08, 09, 10, 15, 18, 32, 34, 35 | RF01–RF07, RF10, RF12, RF23, RF25, RF26, RF29, RF30, RF50, RF55, RF56, RF57, RF59 |
| OB-02 | P-02, P-04, P-05 | RN-01 a RN-07, RN-15, RN-16, RN-18, RN-35 | RF05–RF10, RF12, RF23, RF24, RF27 a RF30, RF53, RF54, RF59 |
| OB-03 | P-02, P-06, P-07 | RN-01, 02, 03, 04, 06, 09, 10, 19, 20, 22, 23 | RF07, RF08, RF10, RF12, RF36, RF39 a RF42, RF44, RF46 a RF48, RF51, RF65 a RF70 |
| OB-04 | P-02, P-05, P-06, P-07 | RN-02, 05, 07, 09, 17, 18, 20, 21, 32, 33, 34 | RF05, RF06, RF11, RF31, RF32, RF35, RF43, RF52, RF55, RF58, RF59, RF63, RF67 |
| OB-05 | P-01, P-03, P-07 | RN-07 a RN-14, RN-23 a RN-31 | RF01–RF04, RF13–RF22, RF40, RF45, RF47, RF48, RF49, RF56, RF72 |
| OB-06 | P-06, P-07 | RN-01, 05, 09, 19 a 23 | RF33–RF39, RF41, RF42, RF46 |
| OB-07 | P-02, P-04, P-05, P-07, P-08 | RN-07, RN-09, RN-18, RN-23, RN-35, RN-36, RN-37 | RF05 a RF12, RF23, RF29, RF59 a RF62, RF64, RF71 |

### 9.2 Historia → requisitos → reglas → objetivos

| Historia | Requisitos | Reglas | Objetivos |
| --- | --- | --- | --- |
| HU-01 Registrarme | RF01 | RN-08 | OB-05 |
| HU-02 Iniciar sesión | RF02 | RN-09, RN-10 | OB-05 |
| HU-03 Conocer la plataforma | RF50 | — | OB-01 |
| HU-04 Buscar por nombre | RF05 | RN-05 | OB-01 |
| HU-05 Explorar por categoría | RF05, RF11 | RN-05 | OB-04 |
| HU-06a Filtrar por nombre y categoría | RF06 | RN-05 | OB-01, OB-02 |
| HU-06b Filtrar por tienda y rango de precio | RF06 | RN-05 | OB-01, OB-02 |
| HU-07 Comparar precios | RF07, RF10, RF12 | RN-01, 02, 04, 06 | OB-01, OB-02, OB-03 |
| HU-08 Ver la mejor opción | RF08, RF09 | RN-03 | OB-02 |
| HU-09 Ver mi sesión y cerrarla | RF02, RF49 | RN-09 | OB-05 |
| HU-10 Calificar y comentar | RF13, RF14, RF16, RF18 | RN-07, 11, 12, 24, 28, 30 | OB-05 |
| HU-11 Corregir mi opinión | RF19 | RN-07, RN-25, RN-29 | OB-05 |
| HU-12 Ver mi opinión | RF20 | RN-07, RN-12 | OB-05 |
| HU-13 Ver promedios | RF15 | RN-13 | OB-05 |
| HU-14 Leer comentarios | RF21 | RN-14, RN-25 | OB-05 |
| HU-15 Eliminar mi comentario | RF22 | RN-07, 14, 25 | OB-05 |
| HU-16 Reportar un comentario | RF17 | RN-07, 14, 26, 27 | OB-05 |
| HU-17 Seguir un producto | RF23 | RN-07, RN-15 | OB-01, OB-02 |
| HU-18 Definir mi alerta | RF24 | RN-16 | OB-02 |
| HU-19 Pausar un aviso | RF25 | RN-15 | OB-01 |
| HU-20 Ver lo que sigo | RF26 | RN-15 | OB-01 |
| HU-21 Recibir un aviso de baja | RF27 | RN-15, RN-16 | OB-02 |
| HU-22 Ver mis avisos | RF27, RF53, RF54 | RN-15 | OB-02 |
| HU-23 Recibir un correo de baja | RF28 | RN-15, RN-16 | OB-02 |
| HU-24 Iniciar sesión con Google | RF56 | RN-08, RN-09, RN-10 | OB-01, OB-05 |
| HU-25 Recuperar mi contraseña | RF03 | RN-08 | OB-05 |
| HU-26 Modificar mi perfil | RF04 | RN-08 | OB-05 |
| HU-27 Ir a la tienda | RF57 | RN-01 | OB-01 |
| HU-28 Comparar precios de droguería | RF52 | RN-01, RN-02 | OB-04 |
| HU-29 Explorar la sección de droguería | RF06, RF11, RF52 | RN-05 | OB-04 |
| HU-30 Comparar precios de cuidado facial | RF58 | RN-01, RN-02 | OB-04 |
| HU-31 Explorar la sección de cuidado facial | RF06, RF11, RF58 | RN-05 | OB-04 |
| HU-32 Comparar un repuesto por su referencia | RF55 | RN-02, RN-32, RN-34 | OB-04 |
| HU-33 Buscar un repuesto por su referencia | RF05, RF55 | RN-05, RN-32, RN-34 | OB-01, OB-04 |
| HU-34 Explorar la sección de repuestos | RF11, RF55 | RN-05, RN-34 | OB-04 |
| HU-35 Filtrar por marca | RF06 | RN-05 | OB-01, OB-02 |
| HU-36 Vigencia del precio | RF07, RF08, RF10 | RN-01, RN-04 | OB-02, OB-03 |
| HU-37 Recomendar dónde comprar una lista | RF29, RF59 | RN-07, RN-18 | OB-01, OB-02, OB-07 |
| HU-38 Sugerir recetas con productos del catálogo | RF30, RF59 | RN-07, RN-18 | OB-01, OB-02 |
| HU-39 Consultar productos de droguería | RF31, RF59, RF63 | RN-07, RN-17, RN-18, RN-33 | OB-04 |
| HU-40 Recibir recomendaciones de cuidado facial | RF32, RF59 | RN-07, RN-17, RN-18 | OB-04 |
| HU-41 Consultar mi plan y sus límites | RF64 | RN-07, RN-36, RN-37 | OB-07 |
| HU-42 Adquirir el plan Premium | RF62 | RN-07, RN-37 | OB-07 |
| HU-43 Conocer el bloqueo al superar un límite gratuito | RF60, RF61, RF64 | RN-07, RN-35, RN-36 | OB-07 |
| HU-44 Gestionar los clientes | RF40, RF47, RF48 | RN-09, RN-10, RN-23 | OB-03, OB-05 |
| HU-45 Visualizar la actividad de clientes | RF65 | RN-09 | OB-03 |
| HU-46 Consultar los productos más seguidos y más buscados | RF66 | RN-09 | OB-03 |

El CRUD de catálogo (RF43 y RF67) se planifica en HT-26 y las gráficas históricas de precios (RF44 y RF68) en HT-27. Los requisitos de proveedores, moderación y gestión administrativa ampliada tienen planificación independiente.

### 9.3 Regla → requisitos

| Regla | Requisitos | Regla | Requisitos |
| --- | --- | --- | --- |
| RN-01 | RF10, RF36, RF44, RF51, RF57, RF68 | RN-16 | RF24, RF27, RF28, RF53 |
| RN-02 | RF07, RF51, RF52, RF55, RF58, RF69, RF70 | RN-17 | RF31, RF32, RF63 |
| RN-03 | RF08, RF09 | RN-18 | RF29 a RF32, RF63 |
| RN-04 | RF07, RF10 | RN-19 | RF33, RF34, RF41 |
| RN-05 | RF05, RF06, RF11, RF35, RF43, RF52, RF55, RF58 | RN-20 | RF35, RF42 |
| RN-06 | RF12 | RN-21 | RF35, RF37, RF38 |
| RN-07 | RF13, RF14, RF17, RF19, RF20, RF22, RF23, RF29 a RF32, RF59 a RF64 | RN-22 | RF36, RF39 |
| RN-08 | RF01, RF03, RF04, RF56 | RN-23 | RF40, RF41, RF42, RF43, RF45, RF47, RF48, RF67, RF69, RF70, RF72 |
| RN-09 | RF02, RF33, RF40 a RF48, RF56, RF65 a RF72 | RN-24 | RF13, RF14 |
| RN-10 | RF02, RF40, RF47, RF56 | RN-25 | RF19, RF21, RF22 |
| RN-11 | RF13, RF16 | RN-26 | RF17 |
| RN-12 | RF13, RF18, RF20 | RN-27 | RF17, RF45 |
| RN-13 | RF15 | RN-28 | RF14, RF18 |
| RN-14 | RF17, RF21, RF22, RF45 | RN-29 | RF19 |
| RN-15 | RF23, RF25, RF26, RF27, RF28, RF53, RF72 | RN-30 | RF13, RF14 |
| RN-31 | RF15, RF72 | RN-32 | RF05, RF51, RF55 |
| RN-33 | RF31, RF63 | RN-34 | RF05, RF51, RF55 |
| RN-35 | RF05 a RF12, RF13, RF14, RF23, RF59, RF60, RF61 | RN-36 | RF23, RF60 a RF62, RF64 |
| RN-37 | RF62, RF64, RF71 | — | — |

### 9.4 Requisitos no funcionales → objetivos y requisitos afectados

| RNF | Objetivo | Requisitos o módulos afectados | Regla |
| --- | --- | --- | --- |
| RNF01 | OB-01, OB-03, OB-04, OB-07 | Todos los requisitos con interfaz, incluidos panel, chat y suscripción | — |
| RNF02 | OB-05 | RF01, RF02, RF03, RF33 | — |
| RNF03 | OB-01 | Todos | — |
| RNF04 | OB-04 | RF11, RF12, RF43, RF52, RF55, RF58 | RN-06 |
| RNF05 | OB-04, OB-06 | Módulos por fase | — |
| RNF06 | OB-03, OB-04 | RF36, RF44, RF51, RF55, RF63, RF68 a RF70 | RN-01, RN-02, RN-33 |
| RNF07 | OB-05 | RF02, RF56 | RN-09 |
| RNF08 | OB-01, OB-03, OB-07 | Todos los requisitos con API, incluidos cuotas, panel y confirmación de pagos | RN-09, RN-36, RN-37 |
| RNF09 | OB-05 | RF14, RF17, RF21 | — |
| RNF10 | OB-01 | RF05 a RF09 | — |
| RNF13 | OB-04, OB-07 | RF29 a RF32, RF59, RF60 | RN-07, RN-36 |
| RNF14 | OB-07 | RF60, RF61, RF62 | RN-36, RN-37 |

## Anexos

### Anexo A. Criterios de prueba por requisito

Cada requisito se verifica con un criterio de «pasa si…»; los valores con asterisco siguen por confirmar.

| RF | Objetivo | Regla | Pasa si… |
| --- | --- | --- | --- |
| RF01 | OB-01, OB-05 | RN-08 | Un correo nuevo crea la cuenta y uno repetido se rechaza |
| RF02 | OB-01, OB-05 | RN-09, RN-10 | Credenciales válidas acceden y una cuenta bloqueada no |
| RF03 | OB-01, OB-05 | RN-08 | Llega el enlace y deja de servir a los 30 minutos\* |
| RF04 | OB-01, OB-05 | — | El nombre nuevo aparece en el perfil |
| RF05 | OB-01, OB-02 | RN-05 | Todo resultado coincide con el nombre, la marca o la categoría buscados; una referencia de repuesto escrita con o sin guiones devuelve el repuesto con esa referencia |
| RF06 | OB-01, OB-02 | RN-05 | Filtrar por Éxito y precio máximo deja solo esos productos; un producto barato en otra tienda pero caro en Éxito no aparece; un mínimo mayor que el máximo responde 400 |
| RF07 | OB-01, OB-02 | RN-02, RN-04 | Un producto vendido en 3 tiendas muestra los 3 precios |
| RF08 | OB-02 | RN-03 | Se resalta la tienda de menor precio más reciente |
| RF09 | OB-02 | RN-03 | En el comparador cada precio es menor o igual al siguiente; la búsqueda respeta el ahorro porcentual y sus desempates |
| RF10 | OB-03 | RN-01, RN-04 | Todo precio mostrado exhibe la fecha de su última confirmación. |
| RF11 | OB-04 | RN-05 | Elegir «Lácteos» lista solo productos de esa categoría |
| RF12 | OB-01, OB-02 | RN-06 | Aparecen D1, Jumbo, Éxito y Olímpica |
| RF13 | OB-05 | RN-07, 11, 12, 24, 30 | Calificar con 4 estrellas y comentar un producto activo con precio en esa tienda se registra; 0 o 6 estrellas da 400; producto o tienda inactivos da 409, inexistentes o sin precio del par da 404; ADMIN recibe 403; calificar sin comentar se rechaza |
| RF14 | OB-05 | RN-07, 24, 28, 30 | Un comentario de 10 a 500 caracteres se publica; uno de 9 o de 501 se rechaza; comentar sin calificar se rechaza |
| RF15 | OB-05 | RN-13 | Calificaciones 5 y 3 dan promedio 4,0 y cantidad 2 |
| RF16 | OB-05 | RN-11 | Cada calificación y comentario muestra su fecha |
| RF17 | OB-05 | RN-07, 14, 26, 27 | Un reporte válido deja el comentario en REPORTADO y crea un reporte PENDIENTE; repetirlo no duplica (204); razón OTRO sin detalle da 400; reportar el propio da 400; reportar uno oculto, eliminado o inexistente da 404 |
| RF18 | OB-05 | RN-12, RN-28 | La segunda calificación del mismo producto en la misma tienda da 409; en otra tienda se acepta; un segundo comentario en el mismo producto y tienda da 409 |
| RF19 | OB-05 | RN-07, RN-25, RN-29 | A las 23 h 59 min la calificación y el comentario se actualizan y el promedio se recalcula; a las 24 h 1 min se rechaza |
| RF20 | OB-05 | RN-07, RN-12 | Devuelve la calificación y el comentario propios, o 204 si aún no opinó |
| RF21 | OB-05 | RN-14, RN-25 | Lista solo comentarios no OCULTOS ni ELIMINADOS, máximo 50 por página, recientes primero, sin estado ni correo |
| RF22 | OB-05 | RN-07, 14, 25 | El autor elimina su comentario: queda ELIMINADO y desaparece del listado; otro usuario recibe 403; no puede volver a comentar ese producto en esa tienda; eliminar un comentario OCULTO da 409 y sigue OCULTO; repetir la eliminación da 204 |
| RF23 | OB-01, OB-02, OB-07 | RN-07, RN-15, RN-36 | Seguir dos veces el mismo producto da 409. Un visitante debe iniciar sesión. El plan aplica el tope de RF61 |
| RF24 | OB-02 | RN-16 | Un precio objetivo de 0 o negativo da 400; sin objetivo cuenta cualquier reducción |
| RF25 | OB-01 | RN-15 | Con el aviso desactivado una baja no genera aviso |
| RF26 | OB-01 | RN-15 | Siguiendo 2 productos, la lista muestra esos 2 |
| RF27 | OB-02 | RN-15, RN-16 | Bajar de $6.000 a $4.800 con objetivo de $5.000 genera un solo aviso; correr la revisión dos veces no duplica |
| RF28 | OB-02 | RN-15, RN-16 | En el caso de RF27 llega el correo; si el envío falla, el aviso existe igual |
| RF29 | OB-01, OB-02 | RN-18 | Recomienda con precios del catálogo y sin productos ajenos a él |
| RF30 | OB-01, OB-02 | RN-18 | La respuesta incluye preparación e ingredientes e indica cuáles se encuentran en el catálogo |
| RF31 | OB-04 | RN-17, RN-18, RN-33 | La respuesta usa precios del catálogo, excluye medicamentos bajo fórmula y trae el aviso de que no sustituye el diagnóstico ni la prescripción médica |
| RF32 | OB-04 | RN-17, RN-18 | Recomienda dentro del presupuesto y con el aviso |
| RF33 | OB-06 | RN-09, RN-19 | La solicitud queda PENDIENTE y no puede publicar |
| RF34 | OB-06 | RN-19 | Los datos se guardan y se consultan |
| RF35 | OB-04, OB-06 | RN-05, 20, 21 | Un producto de un proveedor aprobado se ve de inmediato; uno de un proveedor pendiente no se publica |
| RF36 | OB-03, OB-06 | RN-01, RN-22 | El precio se guarda con la fecha actual |
| RF37 | OB-06 | RN-21 | Modifica o elimina lo propio y no lo de otro proveedor |
| RF38 | OB-06 | RN-21 | La lista contiene solo sus productos |
| RF39 | OB-03, OB-06 | RN-22 | Cada producto muestra su última actualización |
| RF40 | OB-03, OB-05 | RN-09, RN-10, RN-23 | ADMIN lista, crea, consulta, modifica, activa, desactiva y elimina un cliente sujeto a integridad. Las operaciones no están disponibles para otros roles y se auditan cuando cambian datos |
| RF41 | OB-03, OB-06 | RN-19, RN-23 | Un proveedor rechazado no puede publicar |
| RF42 | OB-03, OB-06 | RN-20, RN-23 | Un producto dado de baja deja de ser visible y uno conservado sigue visible |
| RF43 | OB-04 | RN-05, RN-09, RN-23 | ADMIN gestiona marcas y categorías. Una categoría activa y válida queda disponible; una operación incompatible con la jerarquía o las relaciones se rechaza |
| RF44 | OB-03 | RN-01 | Cada precio se muestra con su fecha y fuente |
| RF45 | OB-05 | RN-14, 23, 27 | Ocultar un comentario lo quita de los listados y resuelve sus reportes |
| RF46 | OB-03, OB-06 | — | El reporte muestra los totales del periodo |
| RF47 | OB-05 | RN-10, RN-23 | La cuenta bloqueada no inicia sesión |
| RF48 | OB-03, OB-05 | RN-23 | Cada cambio administrativo aparece con fecha y responsable |
| RF49 | OB-05 | — | Cerrar sesión borra el token; una petición posterior con ese token devuelve 401 |
| RF50 | OB-01 | — | La portada muestra datos respaldados, omite bloques sin información y cada página de sección abre su búsqueda sin perder el sector |
| RF51 | OB-03 | RN-01, RN-02 | Una observación aceptada registra/refresca un precio; una corrida abierta o una oferta CONFLICT no publica; duplicados de una corrida registran solo el ganador válido y una repetición sin pendientes no añade precios |
| RF52 | OB-04 | RN-02, RN-05 | Un producto de droguería vendido en dos droguerías muestra los dos precios en la comparación |
| RF53 | OB-02 | RN-15, RN-16 | La lista de mis avisos incluye los generados por RF27 con su cantidad de no leídos |
| RF54 | OB-02 | — | Marcar un aviso como leído baja el contador del encabezado |
| RF55 | OB-04 | RN-02, RN-05, RN-32, RN-34 | Si se habilita el comparador, una pieza de automóvil con la misma marca y referencia en dos tiendas muestra ambos precios; una referencia o marca distinta no coincide y los repuestos de motocicletas quedan excluidos |
| RF56 | OB-01, OB-05 | RN-08, RN-10 | Una cuenta de Google nueva crea una cuenta USER e inicia sesión; un correo ya registrado entra a la misma cuenta; una cuenta desactivada no entra |
| RF57 | OB-01 | RN-01 | Un precio con página registrada muestra el enlace y abre la página del producto en la tienda; un precio sin página registrada no muestra enlace |
| RF58 | OB-04 | RN-02, RN-05 | Un producto de cuidado facial vendido en dos tiendas muestra los dos precios en la comparación |
| RF59 | OB-01, OB-02, OB-04, OB-07 | RN-07 | Cada avatar abre el chat de su sección al consumidor con sesión. En móvil se accede desde un único botón flotante. Cerrar o recargar borra el historial y un visitante recibe una invitación a iniciar sesión |
| RF60 | OB-07 | RN-07, RN-35, RN-36 | Con plan Gratuito se acepta la cuarta consulta del mes y se bloquea la quinta. Una lista completa de 200 caracteres se acepta y una de 201 se bloquea aunque cada artículo sea más corto. Cambiar de sección o reabrir el chat no restaura la cuota. El reinicio mensual habilita las cuatro consultas siguientes |
| RF61 | OB-07 | RN-07, RN-35, RN-36 | Se acepta el quinto producto seguido y se bloquea el sexto. Retirar un seguimiento libera un cupo. Comparar precios sigue disponible |
| RF62 | OB-07 | RN-07, RN-37, RN-36 | Una confirmación válida de prueba activa Premium y amplía topes; una confirmación duplicada no duplica la activación, y un pago pendiente, fallido o sin confirmación mantiene el plan anterior |
| RF63 | OB-04 | RN-17, RN-18, RN-33 | Ante una consulta por un medicamento bajo fórmula, el servicio lo excluye antes de enviar productos al modelo aunque esté en el catálogo o el scraper no lo excluya. El aviso se muestra también cuando no hay recomendaciones |
| RF64 | OB-07 | RN-07, RN-37, RN-36 | Tras usar una consulta o seguir un producto se muestra el consumo actualizado en Mi plan. Al activarse Premium se muestran sus límites ampliados |
| RF65 | OB-03 | RN-09 | ADMIN ve un total de clientes activos y la gráfica de promedios que corresponden al conjunto de actividad y periodo de prueba |
| RF66 | OB-03 | RN-09 | Un conjunto de búsquedas y seguimientos con conteos conocidos produce dos rankings ordenados, cada uno con su métrica |
| RF67 | OB-03, OB-04 | RN-09, RN-23 | ADMIN puede gestionar productos y tiendas y un cambio admisible queda auditado. Se rechaza una eliminación que infrinja la integridad de las relaciones |
| RF68 | OB-03 | RN-01, RN-09 | Un producto con precios en dos tiendas muestra ambas series con fechas en orden cronológico. Sin precios se informa que no hay datos |
| RF69 | OB-03 | RN-02, RN-09, RN-23 | ADMIN revisa un objetivo y una oferta en conflicto. Gestionar la oferta respeta su estado y no omite la validación que precede a publicar |
| RF70 | OB-03 | RN-02, RN-09, RN-23 | ADMIN identifica los tres recursos por su nombre legible. Una equivalencia modificada conserva evidencia y registra fecha y responsable |
| RF71 | OB-07 | RN-09, RN-37 | ADMIN consulta el plan de un cliente y el estado de su suscripción. Otro rol no accede a esa vista |
| RF72 | OB-05 | RN-09, RN-15, RN-23, RN-31 | Una acción permitida sobre un seguimiento o una calificación se audita. Moderar el texto de un comentario por sí solo no altera el promedio |

### Anexo B. Registro de decisiones y pendientes

Este registro reúne acuerdos definidos y decisiones que conservan un valor provisional. Las decisiones técnicas consolidadas el 18-nov-2026 se agregan desde DP-40; no se renumeran los IDs existentes. Desde RF49 (incluido), los IDs de requisito ya no se renumeran: lo nuevo se agrega al final del módulo que le corresponde, aunque quede fuera de orden en la página.

| ID | Decisión | Valor provisional | Afecta |
| --- | --- | --- | --- |
| DP-01 | ¿El visitante puede buscar y comparar sin registrarse? | Definida: sí, mediante consultas públicas | ACT-01, RF05 a RF12 |
| DP-02 | Código HTTP al modificar una opinión fuera del plazo | 409 | RF19 |
| DP-03 | ¿El plazo de 1 día se reinicia con cada edición? | No: cuenta desde el registro | RN-29 |
| DP-04 | Comentario eliminado que tiene reportes pendientes | Los reportes siguen PENDIENTE para el administrador | RN-14, RF22, RF45 |
| DP-05 | Reactivar un aviso: ¿genera avisos de bajas ocurridas mientras estuvo desactivado? | No | RF25, RF27 |
| DP-06 | Frecuencia de la revisión de bajas de precio | Evento inmediato tras un precio nuevo y detector de respaldo cada 24 horas, configurable | RF27 |
| DP-07 | Reglas sin validar con usuarios o con el equipo | Se mantienen como «por confirmar» | RN-02, 08, 14, 24, 26, 27 |
| DP-08 | Seguir productos solo para el rol USER | Sí | RN-07, RF23 |
| DP-09 | Producto de proveedor visible de inmediato (RN-20): ¿se muestra una etiqueta «en revisión»? | No. Riesgo: precios erróneos visibles hasta la revisión; se mitiga con proveedor aprobado (RN-19) y con fuente y fecha obligatorias (RN-01) | RF35, RF42 |
| DP-10 | Evidencia de una fuente verificable | Criterio operativo definido: fuente pública permitida para la integración, tienda/fecha/URL y evidencia de identidad/presentación. Un acuerdo contractual u oficial no se presume por acceder a una API pública | RNF06, RF51 |
| DP-11 | Marca en RF05, RF06, RF35 y RF43 | Se integra al modelo (tabla `brands`, Sprint 2); la búsqueda y el filtro por marca se habilitan cuando esté en `main` | RF05, RF06, RF35, RF43 |
| DP-12 | Fase de RF03 y RF04 | Fase 2 (Sprint 2, épica Cuenta de usuario) | RF03, RF04 |
| DP-13 | La Fase 6 incluye «aplicación móvil» y el alcance excluye la app móvil nativa | Aclarar si es web adaptable o nativa | Fase 6 |
| DP-14 | Valores de RNF10 (3 segundos, 20 usuarios) | Propuestos; validar con una prueba de carga | RNF10 |
| DP-15 | Moderación y bloqueo mientras falten sus funciones en el panel | Manual en la base de datos | RF45, RF47 |
| DP-16 | Al eliminar su comentario, ¿se conserva la calificación? | Sí: la calificación queda y no puede volver a calificar ni a comentar ese producto en esa tienda | RN-12, RN-25, RF22 |
| DP-17 | Exigir un comentario para calificar (RN-30) puede reducir el número de calificaciones | Se mantiene; medirlo con el indicador de OB-05 | RN-30, OB-05 |
| DP-18 | Catálogo cerrado de razones de reporte | SPAM, LENGUAJE\_OFENSIVO, INFORMACION\_FALSA, OTRO (hasta ahora) | RN-27, RF17 |
| DP-19 | ¿Bajo qué criterio de fraude se puede retirar una calificación del promedio? | Ninguno todavía: el estado del comentario (incluido OCULTO) no retira la calificación; moderar el texto no debe cambiar el promedio | RN-13, RN-31, RF15, RF72 |
| DP-20 | Compatibilidad de un repuesto con un modelo de automóvil | Fuera del alcance: se compara solo por referencia | RF55, RN-32 |
| DP-21 | ¿Repuestos tiene un asistente de IA en la Fase 4? | No | RF29 a RF32 |
| DP-22 | Inicio de sesión con Google con un correo ya registrado | Entra a la cuenta existente si Google confirma el correo | RF56, RN-08 |
| DP-23 | Cuenta creada con Google: ¿tiene contraseña y puede cambiar su correo? | No tiene contraseña hasta que la cree con RF03; no cambia su correo desde el perfil | RF03, RF04, RF56 |
| DP-24 | ¿Proveedores y administradores pueden iniciar sesión con Google? | No: Google solo crea o abre cuentas con rol USER | RF56, RN-09 |
| DP-25 | Un producto que se vende en supermercado y en droguería: ¿se compara junto? | Sí: la comparación muestra todas las tiendas que lo venden, sin importar su dominio | RF07, RF52, RF58 |
| DP-26 | ¿Qué productos muestra una sección (Droguería, Cuidado facial, Repuestos)? | Los que tienen precio en al menos una tienda de ese dominio; un producto clasificado en otro dominio no aparece bajo las categorías de la sección (limitación conocida) | RF11, RF52, RF55, RF58 |
| DP-27 | ¿Cómo se identifica un mismo producto entre tiendas? | GTIN comparable y presentación exacta; identidad secundaria autorizada hacia un maestro existente para códigos locales; genérico curado por kg para peso variable; repuestos por marca y referencia. Conflictos y ausencia de evidencia no crean maestros | RN-02, RN-32, RF51 |
| DP-28 | Repuesto sin referencia registrada | No entra en la comparación (por confirmar) | RF55, RN-32 |
| DP-29 | Contenido de la página de inicio | Definida: presentación, ejemplos y cifras reales, tiendas, secciones comerciales, beneficios, cómo funciona, quiénes somos, preguntas frecuentes y llamada a la acción; sin testimonios o funciones no implementadas | RF50, HU-03 |
| DP-30 | Profundidad de la jerarquía de categorías | Dos niveles: dominio (raíz) y categoría (hoja); los productos cuelgan de la hoja, por ejemplo Supermercado › Granos y cereales | RN-05, RF11 |
| DP-31 | ¿Las tiendas de repuestos permiten comparar? | SCRUM-295 mide 20 productos por tienda en Importadoras Asociadas, Autoplanet y Zona Repuestera. Se mide la coincidencia exacta en dos o más tiendas por marca y referencia del fabricante, incluido un código OEM cuando identifica la misma pieza y se verifica su marca. Si la coincidencia es menor al 30 %, se replantea SCRUM-156 (EP-06) como listado con buscador por referencia | RF55, RN-32, RN-34 |
| DP-32 | ¿Qué fecha se muestra junto a un precio? | La última vez que la fuente lo confirmó (last_seen_at) | RF10, RN-01 |
| DP-33 | ¿Qué cumple la “fuente” que exige RN-01? | La tienda del precio, que siempre existe; la página del producto en la tienda (RF57) es evidencia adicional cuando se registró. prices.source guarda el nombre del producto tal como lo publica la tienda, para rastrear de qué oferta salió. Se revisa al llegar EP-08 | RN-01, RF51, RF57 |
| DP-34 | ¿Qué precio cuenta en el filtro por rango? | El vigente: el más reciente de cada par producto-tienda, nunca uno anterior, y solo en tiendas activas; con tiendas seleccionadas, solo el de esas tiendas; límites inclusivos | RF06, HU-06b |
| DP-35 | Códigos de la búsqueda | 200 también sin resultados; 400 por un precio negativo o un mínimo mayor que el máximo; 404 por una categoría inexistente o una tienda inexistente o inactiva | RF06, RNF08 |
| DP-36 | Tamaño y orden de las páginas de la búsqueda | 20 resultados por defecto y 50 como máximo, ordenados por mayor ahorro (DP-39) y luego por nombre e id; el usuario no elige el orden. Hasta HT-13 el orden era por nombre | RF05, RF06 |
| DP-37 | Tiendas inactivas en el comparador y en Seguidos | No se muestran ni cuentan para el precio más bajo, igual que en la búsqueda | RF07, RF08, RF26 |
| DP-38 | Precio que la tienda dejó de confirmar | Sigue contando como vigente (riesgo aceptado); se resuelve en HU-36 con una ventana según la fecha de la última confirmación | RF07, RF08, RF10 |
| DP-39 | ¿Cómo se ordena la búsqueda por ahorro? | Ahorro = (precio vigente máximo − mínimo) / máximo entre todas las tiendas activas, en porcentaje, aunque se filtre por algunas; con un solo precio o sin precio es 0 y el producto va al final. Con filtros: lista paginada por ahorro, desempate por nombre e id. Sin filtros: los 5 mejores de cada subcategoría (N provisional), agrupados por sección, en /best-savings | RF05, RF06, RF09, RF11 |
| DP-40 | EAN esperado de una página | `expected_ean` contiene el código esperado, no un booleano; debe verificarse antes de completar tamaño exacto del target | RF51, RN-02 |
| DP-41 | Identidad local de BellaPiel | `has_ean=false`: SKU real en ean/store_sku y mapa por tienda/SKU; NULL mantiene interpretación legada. El código publicado anterior queda en CSV; no se promueve una imagen/referencia genérica a EAN | RF51, RF58, RN-02 |
| DP-42 | Contradicciones dentro de una oferta facial | El scraper asigna CONFLICT, tamaño nulo y reason; CSV por fuente. No entra al procesador ni a la retención automática; la revisión precede a cualquier reapertura | RF51, RN-02, RNF06 |
| DP-43 | Límite de una corrida | UUID y cierre explícito; menor precio válido por tienda/identidad/corrida, empate por menor ID. Legado sin run_id permanece individual; no se agrupa por una hora supuesta | RF51, RN-01 |
| DP-44 | Comparación de peso variable | Genérico activo de 1000 G sin EAN/marca/referencia y enlace manual tienda/SKU; peso publicado y precio total verificado para calcular COP/kg. No se inventa peso ni se equiparan cortes/variedades distintos | RF07, RF51, RN-02 |
| DP-45 | Migraciones y saneamiento histórico | Scripts versionados, respaldo, prueba aislada y preflight. Un override o parche no cambia maestros/precios existentes ni autoriza borrados o reprocesos masivos | RF51, RNF05, RNF06 |
| DP-46 | Evidencia que entrega facial | sku_offers.csv inventaría ofertas sin EAN comercial; conflictos_tamanos.csv describe medidas por fuente. Ambos acompañan la inserción en staging y se publican en Actions | RF51, RNF06 |
| DP-47 | Opciones del buscador por sección | Catálogo de raíces activas y categorías/tiendas de la raíz; cambiar sección limpia categoría/tienda/página. Un error de catálogo no lanza búsqueda global | RF06, RF11, HU-29, HU-31 |
| DP-48 | Cancelaciones de navegación | Conservar AbortController y StrictMode; clasificar desconexiones reconocidas en DEBUG sin otra respuesta, manteniendo ERROR/500 en fallos reales | RNF08, RF05, RF50 |
| DP-49 | Conflictos entre ofertas y correcciones globales | PENDING/UNCLASSIFIED de corridas elegibles pueden aportar evidencia de tamaño; se omite solo aceptación de rango/unidad del target al evaluar esa evidencia. El override requiere una única identidad física y no cambia un maestro existente | RF51, RN-02 |
| DP-50 | Repuestos en el Sprint 4 | HT-28 y SCRUM-295 cubren automóviles. Las HU-32 a HU-34 quedan en backlog y se revisan con el resultado de DP-31. La publicación requiere validar marca y referencia | RF51, RF55, RN-32, RN-34 |
| DP-51 | Origen y selección de la pasarela | Exigencia de Programación Web. HT-22 evalúa Wompi, PayU y Stripe en modo de prueba y registra la elección motivada en DECISIONS.md. HT-24 implementa la integración elegida | OB-07, ACT-10, RF62, RN-37 |
| DP-52 | Cuotas mensuales y límites de Premium | HT-23 modela planes, suscripciones y contadores e implementa el reinicio mensual. Los topes gratuitos quedan definidos por RN-36 y HT-25 los valida desde el backend. El precio y los límites Premium quedan pendientes | RF60 a RF62, RF64, RN-36, RNF14 |
| DP-53 | Asistente y control móvil | HT-20 cubre el modelo compartido con contexto por sección. HT-21 cubre los avatares laterales, el chat temporal y el botón flotante único en móvil | RF29 a RF32, RF59, RNF01, RNF13 |
| DP-54 | Métricas, gestión de clientes y herramienta del panel | Definir cliente activo, periodo y frecuencia de actualización, atribución de búsquedas y política de eliminación de cuentas. HT-26 empieza con una evaluación de cuatro horas de una herramienta integrable. HT-27 usa series sobre los precios registrados | RF40, RF43, RF65 a RF68 |
| DP-55 | Operación ampliada del panel | Quedan en backlog planes, captura, equivalencias, comentarios/reportes y gestión de seguimientos/calificaciones. La carga manual de precios desde el panel queda por definir | RF45, RF69 a RF72 |
| DP-56 | Evidencia para la restricción de droguería | Definir la fuente de clasificación de medicamentos sujetos a fórmula. Aplicar la exclusión en el servicio de IA con independencia del prompt y del scraper | RF31, RF63, RN-33 |
| DP-57 | Selección de cuidado facial | Cutis, Medipiel y BellaPiel. Farmatodo–Cuidado facial, Ánimus Lab y Botanikalia fueron propuestas que nunca se implementaron. La selección está confirmada sin tarea adicional de verificación en el Sprint 4 | RF51, RF58, RNF06 |

### Anexo C. Criterios de aceptación de la ingesta y la navegación

Estos casos concretan RF51, RF06, RF11 y RNF06/RNF08; no crean nuevos requisitos ni cambian sus IDs.

| Caso | Pasa si… |
| --- | --- |
| Corrida abierta | Las observaciones permanecen PENDING y no crean productos/precios hasta el cierre; el legado sin run_id conserva su ruta individual. |
| Duplicados | En una corrida y tienda se usa el menor precio efectivo válido por identidad; las otras aceptadas quedan PROCESSED con motivo de duplicado. En peso variable se compara COP/kg. |
| EAN esperado | Una página con otro EAN o sin EAN comercial suficiente se rechaza; el esperado no se copia como dato recibido. |
| Tamaño ausente | Solo se completa desde target exacto con identidad esperada comprobada y datos parciales compatibles; no se convierte G↔ML. |
| Evidencia contradictoria entre tiendas | Un GTIN nuevo con tamaños distintos no crea maestro aunque una evidencia esté UNCLASSIFIED por no cumplir su target; los rechazos previos conservan motivo. |
| Conflicto interno facial | Se inserta CONFLICT con tamaño/unidad nulos, reason y CSV de fuentes; no hay precio, relleno automático ni borrado por retención. |
| Código local | Un SKU con forma de GTIN sigue siendo local si has_ean=false; se consulta la equivalencia y se revalidan atributos. Sin resolución suficiente no se crea maestro. |
| Mapa incompatible | Se rechaza con motivo específico sin sustituirlo silenciosamente ni bajar el umbral de similitud. |
| Peso variable | Solo un enlace curado y un peso/base de precio válidos producen precio por kg; un paquete fijo o producto por unidad no se transforma por su categoría. |
| Reintento | Corregir datos no reabre automáticamente UNCLASSIFIED/CONFLICT ni reprocesa PROCESSED; se revisan IDs concretos o se captura una oferta nueva. |
| Precios y avisos | Mismo precio refresca el periodo; uno distinto crea otro y emite evento después del commit. El primer precio o una subida no generan por sí solos aviso de baja. |
| Entrada desde una sección | Landing → sección → buscar conserva sector y carga solo sus filtros; «Todas» mantiene la raíz y un sector desconocido no busca globalmente. |
| Navegación y errores | Cambiar de vista cancela peticiones obsoletas; la petición vigente carga datos o muestra recuperación visible. Una desconexión del cliente no se confunde con un error de base de datos. |

### Anexo D. Actualización documental del 18-nov-2026

Se preservan los identificadores RF/RN/HU y las fases de IA, proveedores y administración. Se reconcilian RF09/RF50 con la interfaz real, se concreta RF51 y se agregan decisiones DP-40 a DP-49 y criterios verificables. La planificación de una fase no certifica su despliegue; los pendientes de curaduría, publicación de parches y base compartida figuran en DECISIONS.md.

### Anexo E. Actualización documental del Sprint 4 (21-nov-2026)

Los acuerdos del 20-nov-2026 agregan OB-07, ACT-10, P-08, HU-37 a HU-46, RN-37 a RN-33, RF59 a RF72 y RNF13 a RNF14. Se preservan los identificadores anteriores y se actualizan los criterios de prueba y las matrices de trazabilidad. Las condiciones abiertas figuran en DP-50 a DP-57. El registro técnico y de responsables está en [DECISIONS.md](DECISIONS.md).
