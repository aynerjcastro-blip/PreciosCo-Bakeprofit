# Contrato de interacción de PreciosCo

## Fuentes

- `DECISIONS.md`: jerarquía de dos niveles, secciones por dominio (ADR-02), filtros
  y paginación de búsqueda. Un producto puede compararse entre dominios.
- `CategoryController` / `StoreController`: GET públicos, modificaciones protegidas.
- `frontend/src/styles/tokens.css`: fuente visual; `DESIGN.md` registra intención.

## Canonical UI Map

| Capability | Canonical owner | Source of truth | Allowed variants | Verification |
| --- | --- | --- | --- | --- |
| Form | Search y componentes search existentes | URL, sesión y contrato API | texto, categoría, tienda y precio | prueba de navegador |

No se modifica selección de tablas, fechas, diálogos ni CRUD en esta entrega.
Los filtros actuales son radios y checkboxes nativos, con etiquetas asociadas.

## Búsqueda por sección

- `sector` en la URL identifica la sección. Los IDs raíz se obtienen del backend;
  no son constantes de una base de datos específica.
- Las categorías disponibles son las hojas activas de esa raíz. Las tiendas son
  las activas cuyo dominio apunta a esa raíz.
- Cambiar sector limpia categoría, tiendas seleccionadas y página; conserva texto
  y precio. La categoría «Todas» conserva el sector.
- URL con tienda ajena elimina ese filtro y reinicia página. Un sector inexistente
  no provoca una consulta global. Repuestos mantiene su estado «Pronto».
- Droguerías y Cuidado facial se habilitan en este buscador cuando existe su raíz
  activa. Si no existe, se muestra indisponibilidad.
- Sin sector, la búsqueda global conserva categorías y tiendas activas de todos
  los dominios. El ahorro y las ofertas siguen considerando las tiendas según ADR-02.
- Peticiones cancelables y resultados asociados al alcance actual: una respuesta
  vieja no puede reemplazar otra sección. Error de filtros ofrece reintento.
- Limpiar texto es inmediato, conserva filtros, devuelve foco y cancela la petición
  anterior. No se consulta durante composición IME.
- Persistencia y paginación conservan el comportamiento de la pantalla existente.

## Estados y verificación

Cargando filtros, filtros vacíos, error/reintento, sección inexistente, resultados
vacíos y respuesta tardía deben probarse. Tab y Espacio operan los controles nativos;
el panel móvil conserva `aria-expanded` y el destino de sus controles.
