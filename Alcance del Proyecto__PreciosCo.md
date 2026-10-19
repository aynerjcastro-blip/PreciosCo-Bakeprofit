# Alcance del Proyecto de Software — PreciosCo

Plataforma web para comparación de productos y precios

**Versión 2.0** · Actualizado a partir de las decisiones tomadas durante la Fase 2 (octubre de 2026). Los requisitos funcionales y no funcionales completos, el modelo de negocio, las historias de usuario y las matrices de trazabilidad están en el documento **[PreciosCo — Análisis y especificación](https://claude.ai/code/artifact/a461c386-dc60-44af-ba80-be1a686b1631)**; este documento conserva la visión general del proyecto.

## 1. Descripción del proyecto

**Descripción general:** transformar un bot de Telegram en una plataforma web que permita comparar productos y precios de supermercados, droguerías y tiendas de cuidado facial, incorporando notificaciones, calificaciones, inteligencia artificial especializada y participación directa de proveedores.

Diseñar y desarrollar una plataforma web que permita a los usuarios consultar, comparar y calificar productos de diferentes establecimientos comerciales. La plataforma mostrará los precios disponibles por supermercado o tienda, identificará las mejores opciones de compra y ofrecerá recomendaciones mediante inteligencia artificial.

El sistema es una evolución del bot de Telegram existente, conservando su función principal de comparación de productos y ampliándolo con una interfaz web, un sistema de notificaciones, nuevos tipos de establecimientos y herramientas para que los proveedores publiquen sus productos y precios.

**Glosario:** Consumidor — persona registrada que consulta y compara precios (rol USER). Visitante — consumidor sin sesión. Proveedor — establecimiento con cuenta comercial (rol SUPPLIER). Administrador — rol ADMIN.

## 2. Objetivo general

Desarrollar una plataforma web que centralice y compare los precios de productos de supermercados, droguerías y tiendas de cuidado facial, permitiendo a los usuarios encontrar las mejores ofertas, recibir alertas de cambios de precio, consultar recomendaciones mediante IA y calificar los productos o establecimientos.

## 3. Objetivos específicos

- Migrar las funcionalidades principales del bot de Telegram a una plataforma web.
- Organizar los productos en categorías como granos, aceites, lácteos, aseo, medicamentos y cuidado facial.
- Comparar productos de supermercados como D1, Jumbo, Éxito y Olímpica.
- Permitir consultas por nombre, marca o categoría y mostrar la mejor opción de compra.
- Incorporar calificaciones y opiniones de los usuarios.
- Implementar notificaciones cuando disminuya el precio de un producto seguido.
- Añadir comparadores para droguerías y tiendas de cuidado facial.
- Incorporar asistentes de IA especializados según la categoría consultada.
- Permitir que las tiendas registren sus productos, precios y disponibilidad como proveedores.
- Facilitar la actualización de la información de productos y precios.

Objetivos del negocio asociados (ver el documento de Análisis y especificación): OB-01 reducir el tiempo y esfuerzo para comparar precios; OB-02 aumentar el ahorro al elegir dónde comprar; OB-03 mejorar la confianza en la información de precios; OB-04 ampliar las compras del hogar cubiertas; OB-05 mejorar la confianza en la calidad de productos y establecimientos; OB-06 facilitar que los establecimientos mantengan su oferta actualizada.

## 4. Funcionalidades incluidas

### A. Plataforma web para usuarios

- Página de inicio con buscador.
- Registro, inicio de sesión, cierre de sesión y recuperación de contraseña.
- Perfil básico del usuario.
- Búsqueda por nombre, marca o categoría.
- Filtros por precio, tienda, marca y categoría.
- Comparación de precios entre establecimientos.
- Ordenamiento de resultados del menor al mayor precio.
- Visualización de la fecha de actualización del precio.
- Diseño adaptable para computador, tableta y celular.

### B. Categorías de productos

- Granos y cereales.
- Aceites.
- Lácteos.
- Bebidas.
- Aseo para el hogar.
- Droguería.
- Cuidado facial.
- Otras categorías definidas durante el análisis.

### C. Comparación de establecimientos

- Supermercados: D1, Jumbo, Éxito y Olímpica.
- Droguerías.
- Tiendas de productos para cuidado facial.
- Otros proveedores comerciales en fases posteriores.

### D. Calificaciones y comentarios

- Calificación (1 a 5 estrellas) y comentario sobre un producto en una tienda, registrados juntos en una sola acción: no se califica sin comentar ni se comenta sin calificar.
- Un consumidor opina una sola vez por producto en cada tienda; puede editar su calificación y su comentario durante el día siguiente al registro, y eliminar su comentario en cualquier momento (su calificación se conserva, pero ya no puede volver a comentar ese producto en esa tienda).
- Promedio de calificación de productos y de establecimientos (el de un establecimiento se deriva del de sus productos).
- Reporte de comentarios inapropiados, indicando la razón y, si aplica, un detalle; cada reporte queda pendiente hasta que el administrador lo revisa.
- Controles para evitar calificaciones y comentarios repetidos o fraudulentos.

### E. Sistema de notificaciones

- Seguimiento de productos.
- Definición de precio objetivo o alerta ante cualquier reducción.
- Activación y desactivación de notificaciones.
- Consulta de productos seguidos.
- Consulta de mis avisos, con marcado de leído.
- Avisos dentro de la plataforma cuando baje el precio de un producto seguido.
- Avisos por correo electrónico cuando baje el precio de un producto seguido.

### F. Inteligencia artificial

- IA para recomendaciones de supermercados.
- IA para sugerir recetas con productos disponibles.
- IA para consultar productos de droguería y sus precios, sin sustituir a profesionales de la salud.
- IA para recomendaciones de cuidado facial según tipo de piel, necesidades y presupuesto.

### G. Módulo para proveedores

- Creación de cuenta comercial.
- Registro de datos del proveedor.
- Publicación de productos, marcas, categorías y presentaciones; el producto se publica y se muestra de inmediato (ver sección 10, Supuestos).
- Registro de precios y disponibilidad.
- Actualización o eliminación de productos.
- Consulta de productos publicados.
- Consulta de la fecha de última actualización de cada producto.

### H. Panel administrativo

- Gestión de usuarios y proveedores.
- Aprobación o rechazo de solicitudes de proveedor.
- Revisión de los productos publicados por un proveedor: conservarlo o darlo de baja.
- Administración de categorías y marcas.
- Revisión de precios.
- Moderación de comentarios reportados.
- Reportes básicos.
- Bloqueo de cuentas.
- Consulta del historial de cambios administrativos, con fecha y responsable.

## 5. Entregables del proyecto

- Documento de análisis: modelo de negocio (objetivos, actores, procesos y reglas), historias de usuario, requisitos funcionales y no funcionales, y matrices de trazabilidad.
- Diseño de arquitectura e interfaz.
- Base de datos de usuarios, productos, tiendas, precios y calificaciones.
- Plataforma web para usuarios.
- Módulos de comparación, calificaciones, notificaciones e IA.
- Módulo para proveedores y panel administrativo.
- Migración de información o lógica disponible en el bot.
- Pruebas funcionales y de seguridad.
- Manual de usuario y documentación técnica.
- Publicación de la plataforma en un servidor.

## 6 y 7. Requisitos funcionales y no funcionales

Se documentan por separado, con numeración, trazabilidad hacia los objetivos y reglas de negocio, y criterios de prueba, en **PreciosCo — Análisis y especificación**. Esa es la fuente única: este documento no repite el detalle de cada requisito para evitar que ambos se desactualicen entre sí.

## 8. Alcance recomendado por fases

| Fase | Funcionalidades principales |
| --- | --- |
| Fase 1: MVP | Plataforma web, usuarios, categorías, productos, precios y comparación de supermercados. |
| Fase 2 | Calificaciones, comentarios, seguimiento de productos y notificaciones. |
| Fase 3 | Comparadores de droguerías y cuidado facial. |
| Fase 4 | IA especializada para recetas, supermercados, droguerías y cuidado facial. |
| Fase 5 | Módulo de proveedores para publicar productos y actualizar precios, y panel administrativo (gestión de cuentas, aprobaciones, revisión de productos, categorías, revisión de precios, moderación, bloqueo e historial). |
| Fase 6 | Mejoras, reportes e integraciones externas. |

Hasta que el panel administrativo (Fase 5) esté construido, la moderación de comentarios reportados y el bloqueo de cuentas se realizan de forma manual en la base de datos.

## 9. Elementos fuera del alcance

- Aplicación móvil nativa.
- Compra directa, pagos y domicilios.
- Inventario interno de cada supermercado.
- Garantía de disponibilidad física del producto.
- Diagnósticos médicos o dermatológicos realizados por la IA.
- Prescripciones médicas.
- Integración con todas las tiendas existentes.
- Integraciones iniciales con WhatsApp, SMS u otras aplicaciones.
- Administración logística de proveedores.
- Garantía de actualización de precios en tiempo real.
- Publicidad o monetización avanzada, salvo definición explícita.
- Expansión internacional.

**Nota:** el sistema mostrará los precios registrados o suministrados por las fuentes disponibles, pero no será responsable de variaciones, errores o diferencias entre el precio publicado y el precio final aplicado por cada establecimiento.

## 10. Supuestos y restricciones

**Supuestos:** los proveedores proporcionarán información correcta y actualizada; se tendrá acceso a la información del bot; los productos podrán identificarse por nombre, marca, presentación o código; las tiendas aceptarán participar; y la IA tendrá acceso a una base de datos organizada. El visitante puede buscar y comparar precios sin registrarse. Los precios se cargan desde una fuente externa (el scraper) o los registra el proveedor, ambos con fecha y fuente. Los productos que publique un proveedor se muestran de inmediato tras su registro; el administrador los revisa después y decide si se conservan o se dan de baja — no requieren aprobación previa, porque esperarla sería lento para el proveedor y dispendioso para el administrador.

**Restricciones:** el presupuesto y el tiempo pueden limitar las categorías iniciales; los precios cambian con frecuencia; algunas tiendas pueden no ofrecer mecanismos oficiales de consulta; será necesario revisar aspectos legales sobre marcas e información comercial; y la calidad de la IA dependerá de los datos disponibles.

**Restricciones del proyecto:**

| ID | Restricción |
| --- | --- |
| RP-01 | La base de datos debe estar alojada en un servicio de base de datos en la nube. |
| RP-02 | La documentación de la API debe mantenerse actualizada con Swagger/OpenAPI. |
| RP-03 | El código fuente debe gestionarse con Git/GitHub y una rama por funcionalidad. |
| RP-04 | Cada integrante usa como máximo 2 conexiones a la base de datos compartida, para evitar el error EMAXCONNSESSION. |

Inicialmente, el sistema no incluye compras, pagos, domicilios, aplicación móvil, diagnósticos médicos ni garantía de disponibilidad en tiempo real de los productos. Estas funcionalidades podrán considerarse como ampliaciones posteriores.

## Stack tecnológico propuesto (puede cambiar)

### 1. Backend — MVP (Fase 1)

| Componente | Tecnología | Por qué / rol en el proyecto |
| --- | --- | --- |
| Lenguaje / Framework | Java + Spring Boot | Framework principal del backend; expone la API REST que consume el frontend |
| ORM | Spring Data JPA + Hibernate | Mapea las entidades (Usuario, Producto, Tienda, Categoría, Precio) a la base de datos sin escribir SQL manual |
| Base de datos | PostgreSQL en Supabase | Motor relacional gestionado; requisito del profesor de usar BD en la nube |
| Autenticación | Spring Security + JWT | Cubre registro, inicio de sesión, cierre de sesión y recuperación de contraseña del alcance |
| Documentación de API | Swagger / OpenAPI (springdoc-openapi) | Genera documentación interactiva automática de los endpoints |
| Pruebas manuales de API | Postman | Probar endpoints (GET/POST/PUT/DELETE) antes de tener el frontend listo |

### 2. Frontend — MVP (Fase 1)

| Componente | Tecnología | Por qué / rol en el proyecto |
| --- | --- | --- |
| Framework | React + Vite | Interfaz de usuario y navegación sin recargar la página |
| Estilos | CSS con variables (design tokens) | Diseño adaptable (responsive) para computador, tableta y celular |
| Interactividad | fetch() contra la API REST | Consume la API de Spring Boot; maneja filtros, búsqueda y ordenamiento de resultados |

### 3. Fase 2 — Calificaciones, comentarios y notificaciones

| Componente | Tecnología | Por qué / rol en el proyecto |
| --- | --- | --- |
| Validación de calificaciones y comentarios | Lógica en capa Service (Spring Boot) | Evita opiniones repetidas o fraudulentas por usuario, producto y tienda; calificación y comentario se registran juntos |
| Envío de notificaciones | Spring Boot Starter Mail (JavaMailSender) | Avisos por correo electrónico cuando baja el precio de un producto seguido |
| Tareas periódicas | Spring @Scheduled | Revisa periódicamente cambios de precio para disparar alertas |

### 4. Fase 3 — Nuevos comparadores (droguerías, cuidado facial)

No se agrega tecnología nueva: se extiende el modelo de datos (Categoría, Establecimiento) reutilizando la misma base Spring Boot + PostgreSQL.

### 5. Fase 4 — Inteligencia artificial especializada

| Componente | Tecnología | Por qué / rol en el proyecto |
| --- | --- | --- |
| Consumo de IA | Spring WebClient / RestTemplate | Llama la API de un modelo de IA desde el backend |
| Contexto por sección | Lógica propia (RAG) sobre PostgreSQL | Arma el prompt según la categoría consultando datos reales antes de responder |

### 6. Fase 5 y 6 — Proveedores y panel administrativo

Se reutiliza el mismo backend Spring Boot y frontend React, agregando nuevas entidades (Proveedor, Solicitud de aprobación) y vistas protegidas por rol (Spring Security: ADMIN, SUPPLIER, USER).

### 7. Control de versiones y flujo de trabajo

| Componente | Tecnología | Por qué / rol en el proyecto |
| --- | --- | --- |
| Repositorio | Git + GitHub | Control de versiones y colaboración del equipo |
| Flujo de ramas | Feature branches | Una rama feature/ por funcionalidad, integrada a main mediante Pull Request |

### 8. Base de datos en la nube

Supabase (PostgreSQL gestionado, con panel visual incluido). La migración desde el modelo del bot (Oracle) ya se definió: cada calificación y cada comentario llevan producto y tienda, ambos obligatorios.

### 9. Herramientas de apoyo

DBeaver o pgAdmin para inspeccionar la base de datos; Docker, opcional, para empaquetar el backend.

## Control de cambios

| Fecha | Sección | Cambio | Motivo |
| --- | --- | --- | --- |
| 16-oct-2026 | Todo el documento | Versión 2.0: se agregó el glosario; las secciones 6 y 7 se reemplazan por una referencia al documento de Análisis y especificación | Evitar que dos documentos describan los mismos requisitos con distinto detalle |
| 16-oct-2026 | 3, 4.A, 4.D, 4.E, 4.G, 4.H | Objetivos del negocio referenciados; cierre de sesión agregado; calificación y comentario unificados en una sola acción, con edición de 1 día y eliminación lógica; dos canales de aviso; fecha de actualización como consulta; revisión posterior de productos de proveedor; bloqueo e historial separados | Alinear con las decisiones tomadas durante la Fase 2 |
| 16-oct-2026 | 8, 10 | Nota de moderación manual hasta la Fase 5; supuestos y restricciones del proyecto (RP) agregados | Resolver inconsistencias de fases y dejar explícitas las restricciones del proyecto |
