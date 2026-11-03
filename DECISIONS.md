# DECISIONS.md — PreciosCo / BakeProfit / ComprasCo

Registro de decisiones de equipo: desviaciones deliberadas del MVP y acuerdos tomados durante el desarrollo, para que no se lean como descuidos en una revisión de código o una evaluación.

## Modelo de datos

- **Jerarquía de categorías**: `category.parent_id` autorreferenciada. Las filas raíz son dominios (Supermercado hoy; Droguería, Cuidado facial y Repuestos más adelante). `store` apunta a una categoría raíz; `product` apunta a una categoría hoja. Reemplaza la idea de una tabla `establishment_type` separada.
- **Campo "marca"**: excluido deliberadamente del modelo actual (`Product`, `Category`, etc.). No es un vacío frente al MVP, es una decisión documentada.
- **Convención de FK**: `fk_tabla_tabla` (por ejemplo `fk_ratings_products`).

## Fase 2 — Calificaciones y comentarios

- **Modelo**: se adoptó el modelo del bot original (no el plan inicial de la Fase 2). Cada calificación lleva **producto y tienda**, ambos obligatorios; es única por (producto, tienda, usuario). Ya no existe la calificación directa de una tienda: su promedio se deriva de las calificaciones de sus productos.
- **Calificar y comentar son una sola acción**: se registran juntos, en una sola transacción (se guardan las dos filas o ninguna), pero siguen siendo dos entidades y dos tablas independientes (no una tabla unificada), para poder consultar el promedio sin cargar el texto del comentario.
- **Relación entre las dos tablas**: `comments` apunta a `ratings` con una FK única y obligatoria (`comments.rating_id`, relación 1 a 1). El usuario, el producto y la tienda del comentario se leen de la calificación, sin duplicarlos: así la base de datos impide un comentario de un par distinto al de su calificación y un segundo comentario por calificación. Se descartó repetir usuario, producto y tienda en `comments`, y se corrigió el checklist de la Fase 2 en consecuencia. Restricciones con nombre: FK `fk_ratings_users`, `fk_ratings_products`, `fk_ratings_stores`, `fk_comments_ratings`, `fk_comment_reports_comments` y `fk_comment_reports_users`; únicas `ratings_product_id_store_id_user_id_key`, `comments_rating_id_key` y `comment_reports_comment_id_user_id_key`; check `chk_ratings_score`.
- **Cuándo se puede opinar**: el producto y la tienda deben existir y estar activos, y debe existir al menos un precio registrado de ese producto en esa tienda.
- **Comentario**: mínimo 10 caracteres (confirmado), máximo 500. Uno solo por producto y tienda.
- **Quién puede opinar**: solo el rol USER califica, comenta y reporta (ADMIN y SUPPLIER no). La regla vive en el service y responde con un mensaje claro; `SecurityConfig` solo exige sesión en esas rutas.
- **Edición**: la calificación y el comentario se pueden editar durante **1 día** (24 horas) desde el registro — valor corregido de la propuesta inicial de 5 minutos. Pasado ese plazo, quedan definitivos. Solo se puede editar mientras el comentario siga en estado **PUBLICADO**; uno reportado, oculto o eliminado no se reescribe. El plazo es configurable con la propiedad `app.reviews.edit-window-hours` (24 por defecto).
- **Eliminación de comentario**: borrado lógico (estado `ELIMINADO`), no físico. Después de eliminar, ese usuario no puede volver a comentar ese producto en esa tienda (la fila del comentario se conserva con ese estado). Un comentario `OCULTO` por el administrador **no** puede ser eliminado por el autor (permanece `OCULTO`).
- **Promedio y moderación**: el estado del comentario (incluido `OCULTO`) **no** retira la calificación del promedio del producto ni del establecimiento. Ocultar un comentario es una decisión sobre el texto, no sobre el puntaje. Retirar una calificación exigiría un criterio de fraude propio de la calificación (cuentas duplicadas, patrones repetidos, etc.), que todavía no está definido — queda como decisión pendiente.
- **Reportes**: el campo se llama **razón**, no "tipo". Catálogo provisional: `SPAM`, `LENGUAJE_OFENSIVO`, `INFORMACION_FALSA`, `OTRO` (con detalle obligatorio y máximo 200 caracteres si la razón es `OTRO`). Cada reporte tiene su propio estado: `PENDIENTE` o `RESUELTO`, y se guarda en su propia tabla (`comment_reports`) con usuario, comentario, razón, detalle y estado; en la Fase 2 todos quedan `PENDIENTE`, porque la moderación es manual.
- **Reglas del reporte**: un reporte por usuario y comentario (repetirlo no crea otro); no se puede reportar el propio comentario; un reporte nuevo pasa el comentario a `REPORTADO` (o lo reabre si estaba `CONSERVADO`) y sigue visible; un comentario `OCULTO` o `ELIMINADO` no se puede reportar.
- **Visibilidad y listado**: se listan los comentarios `PUBLICADO`, `REPORTADO` y `CONSERVADO`; `OCULTO` y `ELIMINADO` no se muestran. El listado es paginado (10 por página por defecto, máximo 50), de más reciente a más antiguo, y el cliente no elige el orden. La respuesta pública no incluye el estado ni el correo del autor.
- **Rutas de la API**: todo bajo `/api/reviews`: `POST` (calificar y comentar), `PUT /{id}` (editar), `DELETE /{id}/comment` (eliminar el comentario), `GET` (lista), `GET /summary` (promedio y cantidad de un producto, una tienda o un producto en una tienda), `GET /me` (mi opinión) y `POST /{id}/report`. El id de una opinión es el de su calificación. La lista y el resumen son públicos; el resto exige sesión. Los controllers obtienen el usuario autenticado con `@CurrentUser` (Ayner); el usuario temporal fue transitorio y no se mergea a main.
- **Códigos de estado** *(por confirmar con el equipo; las pruebas automáticas y la colección de Postman se escriben contra esto)*: `201` al crear; `400` por datos inválidos, por razón `OTRO` sin detalle o por reportar el propio comentario; `401` sin sesión; `403` por rol no permitido o por no ser el autor; `404` por producto, tienda u opinión inexistentes, por un par sin precio o por reportar un comentario oculto o eliminado; `409` por opinión repetida (también después de eliminar el comentario), producto o tienda inactivos, plazo de edición vencido o comentario no editable; `204` al eliminar el comentario, al reportar (repetir cualquiera de los dos también responde `204`) y en `GET /me` cuando aún no hay opinión.
- **Productos de proveedor** (acordado con Darwin, aplica desde la Fase 5): moderación posterior, no previa. Un producto se publica y se muestra de inmediato; luego aparece en el panel del administrador para decidir si se conserva o se da de baja. Esperar la aprobación previa sería lento para el proveedor y dispendioso para el administrador.

### Pendientes de confirmar con el equipo (Fase 2)

- Criterio de fraude para retirar una calificación del promedio (ver "Promedio y moderación").

## Fase 2 — Frontend

- El formulario de calificar y comentar es un **diálogo** que se abre desde cada tarjeta de precio.
- La lista de comentarios es **una sola sección compartida**, filtrada por tienda con chips (no una lista por tienda).
- Los promedios por tienda se piden con **una petición por tienda** — por ahora no se agregan a la respuesta del comparador.
- Se agrega `storeId` a `PriceCompareResponse` para poder opinar por tienda desde el comparador.
- **Quién ve qué**: el visitante ve promedios y comentarios, y un enlace para iniciar sesión; el rol USER puede opinar, editar, eliminar su comentario y reportar; ADMIN y SUPPLIER solo ven la información.
- **Mi opinión**: se consulta con `GET /me` en cada tarjeta. Se puede editar mientras `editableUntil` no haya vencido; eliminar el comentario pide confirmación; si el comentario fue reportado, ocultado o eliminado, se muestra un aviso en lugar de las opciones de edición.
- **Estrellas**: dos tokens nuevos en `tokens.css`, `--color-star: #FF9F1C` y `--color-star-stroke: #B45309` (en modo oscuro el borde toma el color del relleno). El borde mantiene el contraste de las estrellas sobre fondo claro; se coordinó con el sistema de diseño de Ayner.
- Los diálogos usan el elemento `<dialog>` nativo (foco y cierre con Esc del navegador) a través de un componente `Dialog` reutilizable.
- El código nuevo sigue las convenciones ya establecidas del frontend: `react-router`, CSS Modules, capa `api/`.
- **Regla de lint**: con `eslint-plugin-react-hooks` 7 no se llama a `setState` de forma síncrona dentro de un efecto; lo que se puede calcular a partir de otro estado se calcula al renderizar (se corrigió así `Search.jsx`).

## Alcance y fases

- La moderación de comentarios y el bloqueo de cuentas del panel administrativo **no** pasan a la Fase 2: se quedan en fases posteriores (manual en la BD hasta entonces).
- **Comparador de repuestos**: se agrega a la Fase 3 un comparador de repuestos para vehículos y motos (RF55). Fuentes preseleccionadas para el scraper: Importadoras Asociadas, Autoplanet y Zona Repuestera. Los repuestos se comparan por referencia o código del fabricante (RN-32); la compatibilidad con un modelo de vehículo queda fuera del alcance.
- **Inicio de sesión con Google**: se agrega como RF56 en la épica Cuenta de usuario (Sprint 2), junto con RF03 y RF04. El backend valida la cuenta de Google y emite su propio JWT; solo crea o abre cuentas con rol USER.
- **Enlace a la tienda**: se documenta como RF57 (HU-27) una funcionalidad implementada el 17-oct-2026 sin requisito asociado. Junto a cada precio se muestra un enlace a la página del producto en la tienda (`prices.product_url`, que guarda el scraper); un precio sin URL se muestra sin enlace.

## Documentación (Ingeniería de Software I)

- Los requisitos funcionales y no funcionales se renumeran de forma consecutiva, sin saltos bruscos — hasta RF48. A partir de **RF49**, los IDs ya no se renumeran: lo nuevo se agrega al final del módulo correspondiente, aunque quede fuera de orden en la página.
- RNF11 (pruebas) y RNF12 (manual y documentación) salen de los requisitos no funcionales porque son entregables del proyecto, no cualidades del sistema.
- RNF06 se elimina por duplicar RF10; el contenido mínimo de un precio (valor, fecha, fuente) queda como regla de negocio (RN-01).
- Se mantiene la marca en los RF y los requisitos RF04 y RF40 sin una regla de negocio directa asociada.
- El documento de análisis deja para más adelante las fichas de requisito, el caso de uso, los casos de prueba y la sección de fuentes y método (sin tiempo para entrevistas por ahora).
- Pendiente de reflejar en el documento de análisis lo decidido en la Fase 2: la regla RN-30 debe decir "al registrar y al editar" (eliminar el comentario deja una calificación sin texto visible), los códigos HTTP en HU-10, HU-16 y RNF08, y las decisiones nuevas como decisiones pendientes con su valor provisional.

## Autores en el historial de Git

- `luminuos09` → Ramos
- `Andresbtm` → Darwin
- `kstrodeveloper` y `kstroDev09` → Ayner

## Intención de este archivo

Las decisiones de equipo se toman y registran a nivel de equipo, no individualmente — una distinción importante en evaluaciones o revisiones de código. Este archivo deja constancia de esas decisiones para que una exclusión deliberada no se confunda con un vacío frente al MVP original.
