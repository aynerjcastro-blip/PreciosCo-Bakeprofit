# Inventario de pantallas — Repaso final (Sprint 3 · EP-11 Rediseño)

> **Resultado del paso 4.** Se revisó cada fila del inventario contra el código de la rama `feature/login-registro-contra-profile` (último commit `da1ab5c`). El árbol compila y pasa ESLint sin errores.
>
> | Marca | Significado | Filas |
> | --- | --- | --- |
> | ✔ | Sigue funcionando igual (verificado en el código) | 166 |
> | ~ | Cambió a propósito; la nota dice qué cambió | 13 |
> | 🧪 | Depende de ejecutar la app; queda en la lista de pruebas (sección 14) | 6 |
> | ✘ | Se perdió | **0** |
>
> **Alcance de la rama:** 13 archivos, todos de las pantallas de cuenta, seguimientos y diálogos de opinión. No toca tokens, `App`, rutas, Header ni componentes base. Las filas de Header, Footer, Home, Search y Comparator describen lo que hace hoy la base de Ayner (HT-14); ahí el repaso solo comprueba que lo del inventario sigue existiendo.

Referencia de lo que la interfaz **actual** hace hoy y que el rediseño no debe perder. Se hizo leyendo el código de `frontend/src`.

**Cómo se leyó**

- **Paso 1:** el inventario original (qué hacía cada pantalla, descrito en la columna «Qué hace hoy»).
- **Paso 4 (este documento):** cada fila se comparó con el código actual y la última columna lleva el resultado. Cuando una fila cambió, la nota dice qué cambió y por qué es válido.
- Una fila describe algo que el usuario **ve o puede hacer**. Si en el rediseño cambia de lugar o de aspecto no importa; importa que siga existiendo y que se comporte igual.

**Responsables según el plan del Sprint 3**

| Zona | Responsable |
| --- | --- |
| Header, Footer, Home (landing), Search, Comparator | Ayner |
| Login, Registro, Recuperar y Restablecer contraseña, Perfil | Ramos |
| Seguidos, campana de avisos, diálogos de seguimiento y de opinión | Ramos |
| Componentes base y tokens (`Dialog`, `StarRating`, `RatingSummary`) | Ayner (Ramos solo los usa) |

**Roles** que cambian lo que se ve: visitante (sin sesión), `USER`, `ADMIN`, `SUPPLIER`. Solo `USER` sigue productos, recibe avisos, opina y reporta.

---

## 0. Mapa de rutas (`App.jsx`)

| Ruta | Pantalla | Acceso | Resultado del repaso |
| --- | --- | --- | --- |
| `/` | Home | Público | ✔ |
| `/buscar` | Search | Público | ✔ |
| `/comparador?productId=` | Comparator | Público | ✔ |
| `/login` | AuthPage (login) | Público | ✔ |
| `/register` | AuthPage (registro) | Público | ✔ |
| `/forgot-password` | ForgotPassword | Público | ✔ |
| `/reset-password?token=` | ResetPassword | Público | ✔ |
| `/seguidos` | Followed | Protegida (sesión) | ✔ |
| `/perfil` | Profile | Protegida (sesión) | ✔ |

**Rutas que se enlazan pero no existen** (hoy caen en página vacía): `/terminos`, `/privacidad`, `/sobre-nosotros`, `/como-funciona`, `/contacto`, `/recuperar`. Ver *Hallazgos*.

---

## 1. Elementos globales

### 1.1 Layout (`MainLayout`)

| # | Qué hace hoy | Resultado del repaso |
| --- | --- | --- |
| L1 | Todas las rutas se pintan dentro de Header + contenido + Footer. | ✔ |
| L2 | Al cambiar de ruta (no al filtrar ni paginar en `/buscar`) la ventana sube al inicio. Con Atrás/Adelante no se toca, para recuperar la posición. | ✔ |

### 1.2 Ruta protegida (`ProtectedRoute`)

| # | Qué hace hoy | Resultado del repaso |
| --- | --- | --- |
| P1 | Sin sesión, `/seguidos` y `/perfil` redirigen a `/login` guardando la ruta de origen. | ✔ |
| P2 | Tras iniciar sesión, el usuario vuelve a la ruta que intentaba abrir (no siempre a `/`). | ✔ |
| P3 | Mientras verifica la sesión muestra «Verificando sesión...». (Hoy `loading` siempre es `false`, así que casi no se ve.) | ✔ |
| P4 | Solo exige sesión, no rol: un ADMIN o SUPPLIER entra a `/seguidos` y ahí ve el mensaje de 403. | ✔ |

### 1.3 Encabezado (`Header`) — lo edita Ayner; Ramos lo verifica

| # | Elemento / acción | Comportamiento actual | Resultado del repaso |
| --- | --- | --- | --- |
| H1 | Logo «Precios» + badge CO | Lleva a `/`. | ✔ |
| H2 | Botón hamburguesa (móvil) | Abre/cierra la navegación; `aria-expanded`. | ✔ |
| H3 | Buscador del encabezado | Envía a `/buscar?q=...` (o `/buscar` si está vacío). **Se oculta** en `/`, `/buscar`, `/login` y `/register`. | ✔ |
| H4 | Botón «Modo oscuro / Modo claro» | Alterna el tema, lo guarda en `localStorage` (`preciosco_theme`) y cambia su texto. | ~ Ahora es un interruptor «Modo» (`role="switch"`, con animación). Ya no cambia su texto entre «Modo oscuro» y «Modo claro». Sigue guardando la elección en `preciosco_theme`. |
| H5 | Campana de avisos | Solo con sesión y rol `USER` (ver sección 10.3). | ✔ |
| H6 | Botón de usuario — visitante | Muestra «Ingresa». Panel con «Ingresa» (`/login`) y «Crea tu cuenta» (`/register`). | ✔ |
| H7 | Botón de usuario — con sesión | Avatar con inicial + primer nombre. Panel con nombre, correo, «Mi Perfil» (`/perfil`), «Seguidos» (`/seguidos`, solo `USER`) y «Salir». | ✔ |
| H8 | «Salir» | Borra token y usuario, cierra menús y lleva a `/`. | ✔ |
| H9 | Cierre del panel de usuario | Clic fuera o tecla Esc. | ✔ |
| H10 | Nombre en el encabezado tras editar el perfil | Cambia al instante, sin recargar (viene de `updateUserData`). | ✔ |
| H11 | Navegación por sectores | Supermercados, Farmacia, Repuestos, Belleza → `/buscar?sector=...`; Tecnología → `/buscar`. Cierra los menús al hacer clic. | ~ Quedan 4 sectores: Supermercados, Droguerías, Cuidado facial y Repuestos. Se quitó «Tecnología» y marca el sector activo. |

### 1.4 Pie de página (`Footer`)

| # | Qué hace hoy | Resultado del repaso |
| --- | --- | --- |
| F1 | Logo → `/`. | ✔ |
| F2 | Enlaces: Quiénes somos, Cómo funciona, Contáctanos (rutas sin página). | ✔ |
| F3 | Enlaces a sectores: Supermercados, Farmacia, Repuestos → `/buscar?sector=...`. | ✔ |
| F4 | Redes sociales (Facebook, Instagram, Twitter): abren en pestaña nueva con `noopener noreferrer`. | ✔ |
| F5 | Términos y Condiciones, Política de Privacidad (rutas sin página). | ✔ |

### 1.5 Sesión (`AuthContext`, `auth-helper`)

| # | Qué hace hoy | Resultado del repaso |
| --- | --- | --- |
| S1 | Token y usuario viven en `localStorage` (`token`, `user`); sobreviven a F5. | ✔ |
| S2 | `login()` guarda la respuesta; `logout()` la borra; `updateUserData()` mezcla datos nuevos y actualiza `localStorage`. | ✔ |
| S3 | `isAuthenticated` exige usuario **y** token. | ✔ |
| S4 | No hay verificación del token al cargar: un token vencido se detecta cuando una petición devuelve 401 (cada pantalla lo maneja a su manera). | ✔ |

---

## 2. Home — `/` (Ayner)

| # | Elemento / acción | Comportamiento actual | Resultado del repaso |
| --- | --- | --- | --- |
| HO1 | Hero con buscador grande | Enter o «Buscar» → `/buscar?q=...`; vacío → `/buscar`. | ✔ |
| HO2 | Chips «Explora nuestras categorías» | Supermercados, Farmacia, Repuestos, Belleza → `/buscar?sector=...`; Tecnología → `/buscar`. | ✔ |
| HO3 | Nota «Amplia cobertura para comparar en Colombia» | Texto fijo. | ✔ |
| HO4 | Quiénes somos | Título, logo (400 px) y dos párrafos. Ancla `#sobre-nosotros`. | ✔ |
| HO5 | ¿Cómo funciona? | Tres pasos: Busca y compara, Activa alertas de ahorro, Consulta a la IA. Ancla `#como-funciona`. | ✔ |
| HO6 | Testimonios | Tres tarjetas con datos fijos en el código (empresa, cita, avatar, autor, cargo). Ancla `#testimonios`. | ✔ |
| HO7 | Botón «Encuentra más Opiniones» | No hace nada hoy. | ✔ |

---

## 3. Búsqueda — `/buscar` (Ayner)

| # | Elemento / acción | Comportamiento actual | Resultado del repaso |
| --- | --- | --- | --- |
| SE1 | Barra de búsqueda por nombre | Con debounce de 400 ms. Escribir quita `q` y `pagina` de la URL y vuelve a la página 1. | ✔ |
| SE2 | Chips de categoría | «Todas» + categorías de `GET /api/categories/active`. Alternan la categoría. «Todas» limpia categoría y sector y activa «mostrar todo». | ~ Los chips pasaron a opciones de radio «Categoría» dentro del panel de filtros. «Todas» funciona igual. |
| SE3 | Tarjetas de sector | Supermercados, Droguerías, Cuidado Facial, Repuestos: imagen, ícono, subtítulo y logos de marcas (con texto de respaldo si el logo no carga). Alternan el sector (`aria-pressed`). | ~ Las tarjetas con imagen y logos de marcas pasaron a pestañas compactas con ícono (`aria-pressed`). El subtítulo queda como tooltip. Se pierden las imágenes y los logos (decisión de diseño de Ayner). |
| SE4 | Sector «llega pronto» | Droguerías, Cuidado Facial y Repuestos están marcados `available: false`: no consultan y muestran «X llega pronto». **Esto cambia en este sprint** (SCRUM-236). | ✔ Etiqueta «Pronto» en la pestaña y el mensaje «X llega pronto» se mantienen. |
| SE5 | Filtro de tiendas | Chips «Todas» + tiendas activas (`GET /api/stores/active`); selección múltiple; `tienda` repetible en la URL. | ~ Los chips pasaron a casillas de selección múltiple. «Todas» aparece solo si hay tiendas elegidas. El filtro admite conteos por tienda, pero `Search` aún no los pasa. |
| SE6 | Filtro de precio | Desde/Hasta + «Aplicar precio» (deshabilitado sin cambios) + «Quitar precio». Valida en el cliente: no negativos y mínimo ≤ máximo. | ~ Se agregaron rangos rápidos y un deslizador. Se conserva la validación (negativos y mínimo mayor que máximo), «Aplicar» deshabilitado sin cambios y «Quitar precio». |
| SE7 | URL como fuente de verdad | `q`, `sector`, `tienda`, `min`, `max`, `pagina` (desde 1). El enlace se puede compartir y funciona con Atrás/Adelante. | ✔ |
| SE8 | Estado guardado | Texto, categoría, sector y «mostrar todo» se guardan en `sessionStorage` (`searchState`) y se restauran al volver. | ✔ |
| SE9 | Contador | «1 producto» / «N productos» (solo con resultados). | ✔ |
| SE10 | Tarjeta de producto | Categoría, nombre y presentación; enlaza a `/comparador?productId=`. | ~ La tarjeta ahora trae ofertas por tienda, chip de ahorro y botón «Comparar precios». Sigue enlazando a `/comparador?productId=`. |
| SE11 | Paginación | 20 por página. «Anterior»/«Siguiente» con «Página X de Y»; se oculta con una sola página; al cambiar de página sube a la zona de filtros. | ✔ |
| SE12 | Estado inactivo | Sin texto ni filtros no consulta nada. | ✔ |
| SE13 | Estado cargando | «Cargando · Buscando productos...». | ~ El texto «Buscando productos...» pasó a un esqueleto de carga (`role="status"`). |
| SE14 | Estado sin resultados | «Sin resultados» (página 1). | ✔ |
| SE15 | Página vacía en página > 1 | «Esta página ya no tiene resultados» + botón «Ir a la primera página». | ✔ |
| SE16 | Error 404 con tienda filtrada | «Una de las tiendas ya no está disponible» + botón «Quitar filtro de tienda». | ✔ |
| SE17 | Error 400 | «Revisa el rango de precio» + botón «Quitar precio». | ✔ |
| SE18 | Otro error | «Algo salió mal · No pudimos conectar con el servidor.» | ✔ |
| SE19 | Accesibilidad | Zona de resultados con `aria-live="polite"`. | ✔ |

---

## 4. Comparador — `/comparador?productId=` (Ayner; las opiniones y el seguimiento los toca Ramos)

| # | Elemento / acción | Comportamiento actual | Resultado del repaso |
| --- | --- | --- | --- |
| CO1 | Botón «← Volver» | `navigate(-1)`. | ✔ `navigate(-1)` igual; el texto ahora es «← Volver a los resultados». |
| CO2 | Título | Nombre del producto («Comparando precios...» mientras carga). | ✔ |
| CO3 | Calificación del producto | Estrellas + promedio + cantidad de opiniones (ver sección 11.6, `ProductRating`). | ✔ |
| CO4 | Botón Seguir | Ver sección 10.1. | ✔ |
| CO5 | Subtítulo | «Precios ordenados de menor a mayor entre tiendas activas». | ~ Ahora es el título «Precio en cada tienda» con la nota «De menor a mayor · solo tiendas activas». |
| CO6 | Tarjeta de precio | Imagen (🛒 si falta o falla), tienda, nombre de la oferta (`source`), precio en formato `es-CO`, «Confirmado el {fecha}» (`lastSeenAt`). | ✔ |
| CO7 | Insignia «Mejor precio» | Solo en la primera tarjeta. | ~ «Mejor precio» aparece en todas las tiendas empatadas con el precio más bajo (antes solo la primera) y se suma un resumen arriba. |
| CO8 | Enlace a la tienda | Ícono que abre `productUrl` en pestaña nueva (`noopener noreferrer`). Si no hay URL, **no se muestra nada** (ni ícono deshabilitado). | ✔ |
| CO9 | Barra de opiniones por tienda | Dentro de cada tarjeta (ver sección 11.1). | ✔ |
| CO10 | Sección «Opiniones» | Debajo de las tarjetas (ver sección 11.4). | ✔ |
| CO11 | Estado cargando | «Cargando precios...». | ✔ |
| CO12 | Sin `productId`, error o sin precios | «No se encontraron precios para este producto.» | ✔ |
| CO13 | Clic en el promedio de una tienda | Filtra las opiniones por esa tienda y baja suavemente a `#opiniones`. | ✔ |
| CO14 | Al publicar, editar o eliminar una opinión | Se refrescan el promedio del producto, el de la tienda y la lista. | ✔ |

---

## 5. Inicio de sesión — `/login` (Ramos)

Pantalla de dos columnas: `AuthHero` (izquierda) + `AuthForm` (derecha).

| # | Elemento / acción | Comportamiento actual | Resultado del repaso |
| --- | --- | --- | --- |
| LG1 | Hero | Título «Vuelve a encontrar los mejores precios», subtítulo y tres ventajas (productos favoritos, alertas de precio, comparaciones guardadas). | ✔ |
| LG2 | Tarjeta de ahorro (`SavingsCard`) | Muestra una oferta real: producto, mejor precio y tienda, peor precio y tienda, «Ahorras $X». Enlaza a su comparador. **Se oculta sola** mientras carga o si no hay oferta. | ✔ |
| LG3 | Campo correo | `type=email`, obligatorio, `autocomplete=email`. | ✔ |
| LG4 | Campo contraseña | Obligatorio, `autocomplete=current-password`. Botón ojo para mostrar/ocultar (`aria-label` cambia). | ✔ |
| LG5 | Enlace «¿Olvidaste tu contraseña?» | → `/forgot-password`. | ✔ |
| LG6 | Botón «Iniciar sesion» | Deshabilitado mientras envía; texto «Iniciando...». | ✔ |
| LG7 | Éxito | Guarda la sesión y navega a la ruta de origen (o `/`) con `replace`, para que Atrás no regrese al login. | ✔ |
| LG8 | Error | Mensaje del backend (credenciales inválidas, cuenta inactiva, etc.) en un `<p role="alert">` sobre el botón. Sin conexión: «No se pudo conectar con el servidor. Intenta de nuevo en unos minutos.» | ✔ |
| LG9 | Botón de Google | Separador «o continua con». Valida el token en el backend (`POST /api/auth/google`) y entra como `USER`. | 🧪 Necesita credenciales reales de Google (`VITE_GOOGLE_CLIENTE_ID`). |
| LG10 | Error de Google | Mensaje propio, **separado** del error del formulario y junto al botón de Google. Si el login de Google falla en el cliente: «No se pudo iniciar sesión con Google. Intenta de nuevo o usa tu correo.» | 🧪 Probar con una cuenta desactivada o un fallo de Google. |
| LG11 | Enlace «Regístrate gratis» | → `/register`. | ✔ |
| LG12 | Sello «Acceso seguro y gratuito» | Texto fijo con ícono. | ✔ |

---

## 6. Registro — `/register` (Ramos)

| # | Elemento / acción | Comportamiento actual | Resultado del repaso |
| --- | --- | --- | --- |
| RG1 | Hero | Título «Compara mejor. Ahorra más.» y tres ventajas. | ✔ |
| RG2 | Campos | Nombre, correo y contraseña, todos obligatorios. Contraseña con botón ojo. | ✔ |
| RG3 | Pista «Mínimo 8 caracteres» | Solo texto; **no se valida en el cliente** (lo valida el backend). | ✔ |
| RG4 | Casilla de términos | Obligatoria. Enlaces a `/terminos` y `/privacidad`. Sin marcarla: «Debes aceptar los terminos y condiciones» y no se envía. | ✔ |
| RG5 | Botón «Empezar a comparar» | Deshabilitado mientras envía (el texto en ese momento es «Iniciando...»). | ~ El texto de carga ahora es «Creando cuenta...» en vez de «Iniciando...». |
| RG6 | Éxito | Entra con sesión iniciada (igual que el login). Solo se crea rol `USER`. | ✔ |
| RG7 | Errores del backend | Correo duplicado, formato inválido, etc.: mensaje del backend en `role="alert"`. | ✔ |
| RG8 | No hay botón de Google | Google crea la cuenta en el primer ingreso desde `/login`. | ✔ |
| RG9 | Enlace «Inicia sesión» y sello «Registro gratuito - Sin tarjeta de credito» | → `/login`. | ✔ |

---

## 7. Recuperar y restablecer contraseña (Ramos)

### 7.1 Recuperar — `/forgot-password`

| # | Elemento / acción | Comportamiento actual | Resultado del repaso |
| --- | --- | --- | --- |
| FP1 | Campo correo | Obligatorio; se recorta con `trim`. | ✔ |
| FP2 | Botón «Enviar enlace» | Deshabilitado con el campo vacío o enviando; texto «Enviando...». | ✔ |
| FP3 | Éxito | Pantalla «Revisa tu correo» con el correo escrito y aviso de revisar spam. **Mensaje genérico a propósito**: no revela si el correo existe. | ✔ |
| FP4 | Botones de la pantalla de éxito | «Volver al inicio de sesión» (`/login`) y «Usar otro correo» (vuelve al formulario). | ✔ |
| FP5 | Error | Mensaje del backend (o el genérico «No se pudo enviar el enlace...») en `role="alert"`. | ✔ |
| FP6 | Enlace «Volver al inicio de sesión» y sello «Tu información está protegida» | Fijos. | ✔ |

### 7.2 Restablecer — `/reset-password?token=`

| # | Elemento / acción | Comportamiento actual | Resultado del repaso |
| --- | --- | --- | --- |
| RP1 | Sin `token` en la URL | Pantalla «Enlace no válido» con «Solicitar un nuevo enlace» y «Volver al inicio de sesión». | ~ «Solicitar un nuevo enlace» ahora va a `/forgot-password` (antes iba a `/recuperar`, que no existía). |
| RP2 | Campos | «Nueva contraseña» y «Confirmar contraseña», cada uno con botón ojo. Pista «Mínimo 8 caracteres». | ✔ |
| RP3 | Medidor de seguridad | Aparece al escribir. Niveles: Débil, Regular, Segura, Muy segura (cuenta longitud ≥ 8, mayúscula y minúscula, número, símbolo). Solo orienta; el backend exige únicamente la longitud. | ✔ |
| RP4 | Coincidencia | «Las contraseñas coinciden» (con check) / «Las contraseñas no coinciden», anunciado con `aria-live`. | ✔ |
| RP5 | Botón «Restablecer contraseña» | Habilitado solo con ≥ 8 caracteres, coincidencia, sin enviar y sin éxito previo; texto «Actualizando...». | ✔ |
| RP6 | Éxito | Aviso verde (`role="status"`) con el mensaje del backend + «Te llevaremos al inicio de sesión»; a los 3 s navega a `/login` con `replace`. El temporizador se cancela si el usuario sale antes. Los campos se bloquean. | ✔ |
| RP7 | Error | Token inválido o vencido: mensaje del backend en `role="alert"`. | ✔ |

---

## 8. Perfil — `/perfil` (Ramos) · protegida

| # | Elemento / acción | Comportamiento actual | Resultado del repaso |
| --- | --- | --- | --- |
| PF1 | Título «Mi Perfil» | Fijo. | ✔ |
| PF2 | Campo «Nombre a mostrar» | Precargado con el nombre actual; se bloquea mientras guarda. | ✔ |
| PF3 | Botón «Guardar Cambios» | Deshabilitado si está guardando, si el nombre está vacío o solo tiene espacios, o si no cambió. Texto «Guardando...». | ✔ |
| PF4 | Éxito | Mensaje verde «¡Nombre actualizado correctamente!» y el nombre cambia en el Header y en `localStorage` al instante. | ✔ |
| PF5 | Error HTTP (400, 401, 403) | «Hubo un error al actualizar el perfil.» (mensaje único, no distingue el código). | ✔ |
| PF6 | Error de red | «Error de conexión con el servidor.» | ✔ |
| PF7 | Petición | `PUT /api/users/me` con `{ name }` y el JWT. | ✔ |

---

## 9. Seguidos — `/seguidos` (Ramos) · protegida

| # | Elemento / acción | Comportamiento actual | Resultado del repaso |
| --- | --- | --- | --- |
| FO1 | Cabecera | «Productos seguidos» + «Te avisamos cuando baje el precio de lo que sigues.» | ✔ |
| FO2 | Estado cargando | Spinner + «Cargando tus seguimientos...». | ✔ |
| FO3 | Estado vacío | «Aún no sigues ningún producto.» + indicación de pulsar «Seguir» en el comparador + botón «Buscar productos» (`/buscar`). | ✔ |
| FO4 | Error 403 | «Solo las cuentas de consumidor pueden seguir productos.» | ✔ |
| FO5 | Error 401 | «Tu sesión expiró.» + enlace «Iniciar sesión». | ✔ |
| FO6 | Otro error | Mensaje del backend (o «No se pudieron cargar tus seguimientos.») + botón «Reintentar». | ✔ |
| FO7 | Tarjeta de seguimiento | Nombre del producto (enlaza a `/comparador?productId=`), insignia «Aviso activo» / «Aviso pausado», «Precio más bajo» (o «Sin precios»), descripción del aviso, «Siguiendo desde {fecha}». | ✔ |
| FO8 | Interruptor «Avisarme» | `role="switch"` con `aria-checked`. Pausa o reactiva **sin abrir el diálogo** y conserva el precio objetivo. Se deshabilita mientras esa tarjeta cambia. | ✔ |
| FO9 | Error al cambiar el aviso | Alerta (`role="alert"`) sobre la lista con el mensaje del backend. | ✔ |
| FO10 | Botón «Ajustar» | Abre el diálogo de seguimiento en modo edición (`aria-haspopup="dialog"`). | ✔ |
| FO11 | Tras guardar en el diálogo | La tarjeta se actualiza sin recargar. | ✔ |
| FO12 | Tras dejar de seguir | La tarjeta desaparece sin recargar. | ✔ |

---

## 10. Seguimiento y avisos (Ramos)

### 10.1 Botón «Seguir» (`FollowButton`, dentro del comparador)

| # | Situación | Comportamiento actual | Resultado del repaso |
| --- | --- | --- | --- |
| FB1 | Visitante | Enlace «Seguir» → `/login` con la ruta de origen (al entrar vuelve al comparador). | ✔ |
| FB2 | Sesión con rol distinto de `USER` | No se muestra. | ✔ |
| FB3 | `USER` que no sigue el producto | Botón «Seguir» (deshabilitado mientras consulta o si falló la consulta). Abre el diálogo. | ✔ |
| FB4 | `USER` que ya lo sigue | Botón «✓ Siguiendo» + línea de estado («Aviso pausado» o la descripción del precio objetivo). Abre el diálogo en modo ajustar. | ✔ |
| FB5 | Aviso de resultado | «Ahora sigues este producto.», «Cambios guardados.» o «Dejaste de seguir este producto.»; se oculta solo a los 4 s; región `aria-live="polite"`. | ✔ |
| FB6 | Error al consultar | «No se pudo consultar tu seguimiento.» + «Reintentar». | ✔ |
| FB7 | Conflicto 409 al crear (ya lo seguía, p. ej. otra pestaña) | Cierra el diálogo y vuelve a consultar el estado. | 🧪 Para probar el conflicto 409 hay que seguir el mismo producto desde dos pestañas. |
| FB8 | Al cambiar de producto | No se muestra el estado del producto anterior. | ✔ |

### 10.2 Diálogo de seguimiento (`FollowDialog`)

| # | Elemento / acción | Comportamiento actual | Resultado del repaso |
| --- | --- | --- | --- |
| FD1 | Título | «Seguir producto» (crear) o «Ajustar seguimiento» (editar); subtítulo = nombre del producto. | ✔ |
| FD2 | Precio objetivo (opcional) | Campo numérico con `$`, placeholder «Ej. 5000». Vacío = aviso ante cualquier reducción. Si hay valor debe ser > 0. | ✔ |
| FD3 | Casilla «Avisarme cuando baje el precio» | **Solo en edición.** | ✔ |
| FD4 | Notas | Crear: «Los avisos te llegan a la campana y, si está activado, a tu correo.» Editar: explica qué pasa al pausar y que al reactivar solo cuentan bajas posteriores. | ✔ |
| FD5 | Validación | «El precio objetivo debe ser mayor que cero». | ✔ |
| FD6 | «Dejar de seguir» (solo edición) | **Doble confirmación**: el primer clic cambia el texto a «Confirmar: dejar de seguir»; el segundo borra. | ✔ |
| FD7 | Botones | «Cancelar» y «Seguir producto» / «Guardar cambios» (texto «Guardando...» y deshabilitado mientras envía). | ✔ |
| FD8 | Error | Mensaje del backend en `role="alert"`; con 401 añade enlace «Iniciar sesión». | ✔ |

### 10.3 Campana de avisos (`NotificationBell`, montada en el Header)

| # | Elemento / acción | Comportamiento actual | Resultado del repaso |
| --- | --- | --- | --- |
| NB1 | Visibilidad | Solo con sesión y rol `USER`. Si el backend responde 403 se oculta. | ✔ |
| NB2 | Contador de no leídos | Insignia sobre la campana (`99+` si pasa de 99). `aria-label` «Avisos, N sin leer». | ✔ |
| NB3 | Consulta periódica | Al montarse, cada 60 s y al volver a la pestaña; **no consulta con la pestaña oculta**. Tras un 401 o 403 deja de consultar. | 🧪 Verificar con la pestaña oculta que no consulta. |
| NB4 | Anuncio a lectores de pantalla | «Tienes N avisos sin leer» solo cuando aparecen avisos nuevos (`aria-live`). | ✔ |
| NB5 | Panel desplegable | Título «Avisos» + «Al día» o «N sin leer». Se refresca al abrir. | ✔ |
| NB6 | Cierre del panel | Esc (devuelve el foco a la campana), clic fuera o ir a un producto. | ~ Funciona igual, pero el panel queda montado ~140 ms para su animación de salida (cambio de Ayner en HT-14). |
| NB7 | Estado cargando | Spinner + «Cargando avisos...». | ✔ |
| NB8 | Error sin datos | Mensaje + «Reintentar». | ✔ |
| NB9 | Estado vacío | «Aún no tienes avisos. Sigue un producto y te avisamos cuando baje de precio.» | ✔ |
| NB10 | Cada aviso | Punto (azul si no leído), nombre del producto (enlace a `/comparador?productId=`), «{tienda}: bajó de $X a $Y», fecha y hora. Los no leídos llevan el texto oculto «No leído:». | ✔ |
| NB11 | «Marcar como leído» | Solo en no leídos; texto «Marcando...» mientras envía; baja el contador **solo si el backend confirma**. | ✔ |
| NB12 | Ir al producto | Cuenta como leerlo (marca el aviso). | ✔ |
| NB13 | Error al marcar | Alerta dentro del panel con el mensaje del backend. | ✔ |

---

## 11. Opiniones (diálogos: Ramos · barra y sección: dentro del comparador de Ayner)

### 11.1 Barra de opiniones de cada tienda (`StoreReviewBar`)

| # | Situación | Comportamiento actual | Resultado del repaso |
| --- | --- | --- | --- |
| SR1 | Resumen de la tienda | Estrellas + promedio + cantidad; es un botón que filtra la sección Opiniones por esa tienda. Estados: «Cargando calificaciones...», «Calificaciones no disponibles», «Sin calificaciones». | ✔ |
| SR2 | Visitante | Enlace «Inicia sesión para opinar» → `/login`. | ✔ |
| SR3 | `USER` sin opinión | Botón «Opinar». | ✔ |
| SR4 | `USER` con opinión | Bloque «Tu opinión» con estrellas y un mensaje según el estado del comentario (ver SR5). | ✔ |
| SR5 | Mensajes de «Tu opinión» | Publicado: «Puedes editarla hasta el {fecha}.» o «Ya no se puede editar.» · Reportado: «Tu comentario fue reportado y no se puede editar.» · Oculto: «Un moderador ocultó tu comentario. Tu calificación se mantiene.» · Eliminado: «Eliminaste tu comentario. Tu calificación se mantiene y no puedes volver a opinar aquí.» | ✔ |
| SR6 | «Editar» | Aparece si la opinión todavía es editable (24 h). | ✔ |
| SR7 | «Eliminar comentario» | Aparece en publicado y reportado; **no** en oculto ni eliminado. | ✔ |
| SR8 | Roles `ADMIN` / `SUPPLIER` | Solo ven el resumen, sin acciones. | ✔ |

### 11.2 Diálogo «Tu opinión» (`ReviewDialog`)

| # | Elemento / acción | Comportamiento actual | Resultado del repaso |
| --- | --- | --- | --- |
| RD1 | Título | «Tu opinión» (crear) o «Editar tu opinión»; subtítulo «{producto} en {tienda}». | ✔ |
| RD2 | Calificación | Selector de 1 a 5 estrellas (radios), con efecto al pasar el ratón y texto «N de 5» / «Elige de 1 a 5 estrellas». | ✔ |
| RD3 | Comentario | Texto de 10 a 500 caracteres; contador `N/500`; recorta espacios antes de validar y enviar. | ✔ |
| RD4 | Nota | Crear: solo una opinión por producto y tienda, editable 24 h. Editar: «Puedes editarla hasta el {fecha y hora}.» | ✔ |
| RD5 | Validación en el cliente | «Elige una calificación de 1 a 5 estrellas» y «El comentario debe tener entre 10 y 500 caracteres». | ✔ |
| RD6 | Botones | «Cancelar» y «Publicar opinión» / «Guardar cambios» (texto «Guardando...»). | ✔ |
| RD7 | Error | Mensaje del backend en `role="alert"`; con 401 añade «Iniciar sesión». | ✔ |

### 11.3 Diálogo «¿Eliminar tu comentario?» (`DeleteCommentDialog`)

| # | Elemento / acción | Comportamiento actual | Resultado del repaso |
| --- | --- | --- | --- |
| DC1 | Advertencia | Se conserva la calificación, pero no se podrá volver a opinar sobre ese producto en esa tienda. | ✔ |
| DC2 | Botones | «Cancelar» y «Eliminar comentario» (estilo de peligro; texto «Eliminando...»). | ✔ |
| DC3 | Error | Mensaje del backend (p. ej. 409 si el comentario está oculto). | ✔ |

### 11.4 Sección «Opiniones» (`ReviewsSection`, ancla `#opiniones`)

| # | Elemento / acción | Comportamiento actual | Resultado del repaso |
| --- | --- | --- | --- |
| RS1 | Chips de tienda | «Todas» + una por cada tienda del comparador; `aria-pressed`. | ✔ |
| RS2 | Lista | Autor, estrellas, tienda (solo en «Todas»), fecha y texto. **El texto se escapa**: un comentario con HTML se ve literal. | ✔ |
| RS3 | Paginación | 10 por página; botón «Ver más opiniones (N)» con «Cargando...»; si falla, «No pudimos cargar más opiniones. Inténtalo de nuevo.» | ✔ |
| RS4 | Estados | «Cargando opiniones...»; error con «Reintentar»; vacío («Aún nadie opinó sobre este producto [en {tienda}]. Usa Opinar en la tarjeta de una tienda...»). | ✔ |
| RS5 | Botón «Reportar» | Solo para `USER` con sesión; abre el diálogo de reporte. | ✔ |

### 11.5 Diálogo «Reportar comentario» (`ReportDialog`)

| # | Elemento / acción | Comportamiento actual | Resultado del repaso |
| --- | --- | --- | --- |
| RE1 | Título | «Reportar comentario»; subtítulo «Opinión de {autor}». | ✔ |
| RE2 | Razón | Radios: Spam, Lenguaje ofensivo, Información falsa, Otro. | ✔ |
| RE3 | Detalle | Máximo 200 caracteres con contador; opcional, salvo con «Otro» (obligatorio, se avisa en el contador). | ✔ |
| RE4 | Validación | «Elige una razón» y «Describe la razón del reporte». | ✔ |
| RE5 | Botones | «Cancelar» y «Enviar reporte» (texto «Enviando...»). | ✔ |
| RE6 | Éxito | Pantalla «Reporte enviado · Gracias por avisar.» + «Cerrar». | ✔ |
| RE7 | Error | Mensaje del backend (p. ej. reportar el propio comentario); con 401 añade «Iniciar sesión». | ✔ |

### 11.6 Calificación del producto (`ProductRating`)

| # | Qué hace hoy | Resultado del repaso |
| --- | --- | --- |
| PR1 | Muestra estrellas, «X de 5» y «(N opiniones)». Mismos estados que el resumen de tienda (cargando, no disponible, sin calificaciones). | ✔ |

---

## 12. Componentes base que usan las pantallas

| # | Componente | Comportamiento que se debe conservar | Resultado del repaso |
| --- | --- | --- | --- |
| B1 | `Dialog` | Usa `<dialog>` nativo con `showModal`: bloquea lo de atrás, atrapa el foco y devuelve el foco al botón que lo abrió. Cierra con Esc, con la «×» (`aria-label="Cerrar"`) y con clic en el fondo. | ✔ |
| B2 | `StarRating` | Modo selector (radios, accesible por teclado) y modo lectura (`role="img"` con `aria-label`, o decorativo). Admite fracciones en lectura. | ✔ |
| B3 | `RatingSummary` | Estados cargando / no disponible / sin calificaciones / con datos; puede ser un botón clicable. | ✔ |
| B4 | Formato | Precios en `es-CO` (`$5.000`), fechas con `formatDate` y `formatDateTime`. | ✔ |
| B5 | Tema claro/oscuro | Todas las pantallas respetan `data-theme`. Las estrellas usan los tokens `--color-star` y `--color-star-stroke`. | 🧪 Recorrer cada pantalla en claro y oscuro. |
| B6 | Diseño adaptable (RNF01) | Computador, tableta y celular. | 🧪 Probar en celular, tableta y computador. |

---

## 13. Hallazgos del primer inventario: cómo quedaron

| # | Hallazgo | Estado | De quién es |
| --- | --- | --- | --- |
| 1 | Enlace roto `/recuperar` en Restablecer contraseña | **Resuelto** (`ResetPassword.jsx`) | Ramos |
| 2 | Rutas sin página: `/terminos`, `/privacidad`, `/sobre-nosotros`, `/como-funciona`, `/contacto` | Sigue abierto. Requiere crear rutas en `App.jsx` | Ayner |
| 3 | `main.jsx` lee `VITE_GOOGLE_CLIENTE_ID` y `.env.example` define `VITE_GOOGLE_CLIENT_ID` | Sigue abierto. Confirmar el nombre en el `.env` real | Ayner (`main.jsx`) |
| 4 | `AuthProvider` envuelve la app dos veces (`main.jsx` y `App.jsx`) | Sigue abierto, no rompe nada | Ayner |
| 5 | `Profile.jsx` hace `fetch` directo a `localhost:8080` y su mensaje de error no distingue el código | Sigue abierto (sin cambio de comportamiento a propósito) | Ramos |
| 6 | URL del backend fija en todas las APIs y en `Search.jsx` | Sigue abierto (hay un TODO en el código) | Equipo |
| 7 | Nombres de sector distintos entre Header y el resto | **Parcial:** el Header ya dice Droguerías y Cuidado facial; Home y Footer siguen con «Farmacia» y «Belleza» | Ayner (`ht15-landing`) |
| 8 | «Tecnología» sin sección real | **Parcial:** salió del Header; sigue en los chips de Home | Ayner |
| 9 | El sector no filtra en el backend | Sigue abierto: `develop` ya trae la capa de API de HT-13, pero `Search` aún no la usa | Ayner y Abisael |
| 10 | Registro dice «Iniciando...» y la pista de 8 caracteres no se valida en el cliente | **Resuelto** el texto («Creando cuenta...»). La validación de 8 caracteres sigue solo en el backend | Ramos |
| 11 | Home: «Encuentra más Opiniones» sin acción, avatar por defecto inexistente, paso «Consulta a la IA» | Sigue abierto | Ayner |
| 12 | Sin comprobación del token al cargar la app | Sigue abierto | Equipo |
| 13 | `ProtectedRoute` no mira el rol | Sigue abierto | Ayner |
| 14 | «Seguidos» abierta por URL para roles que no son `USER` | Sigue abierto: ve el mensaje de 403 | Ayner |

## 14. Lo que falta probar en el navegador

El repaso por código no puede ver estados de ejecución ni el aspecto. Las filas marcadas 🧪 y esta lista son lo que queda:

1. **Por rol:** visitante, `USER` y un `ADMIN` o `SUPPLIER` (confirma que no ven «Seguir», la campana ni «Opinar»).
2. **Estados:** cargando, vacío, con datos, sin conexión (apagar el backend), 401 (borrar el token en `localStorage`) y 403 (entrar con ADMIN a `/seguidos`).
3. **Tema:** cada pantalla en claro y oscuro. Revisar contraste del botón naranja y de las alertas.
4. **Tamaños:** celular (hoja inferior de los diálogos, formulario primero en login), tableta y computador.
5. **Flujos completos:** registrar → entrar → seguir un producto → ajustar el precio objetivo → pausar el aviso → dejar de seguir; opinar → editar → eliminar; reportar un comentario ajeno; recuperar y restablecer contraseña con un token real.
6. **Navegador:** probar el cambio de tema en uno sin `startViewTransition` (por ejemplo Firefox).
