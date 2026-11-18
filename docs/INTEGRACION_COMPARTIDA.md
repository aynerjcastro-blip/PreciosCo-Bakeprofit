# Integración de scraper, procesador y buscador

## Código y esquema

El atributo implementado es `scrape_targets.expected_ean VARCHAR(14)`, no un booleano
`ean_required`. Farmatodo y Farmanorte necesitan el EAN real esperado por CADA URL
curada. No se asigna true ni se cambia `products.ean` para rellenar datos desconocidos.
La migración recupera etiquetas [EAN] de targets previos; los targets nuevos deben
guardar la columna explícitamente (`scripts/crear_targets_droguerias.sql`).

El filtro de secciones utiliza la jerarquía y relaciones existentes, sin otra tabla:
`categories.parent_id` relaciona hoja con raíz; `stores.category_id` apunta a la raíz.
No hay migración nueva para el buscador. IDs de pruebas y compartida pueden diferir.

## Orden de integración

1. Integrar y probar los cambios de scraper/procesador y los tres parches correctivos.
2. Integrar backend de filtros (parámetro sectionId opcional y GET categories/roots).
   Los consumidores sin sectionId conservan la respuesta global anterior.
3. Integrar frontend; consume esos endpoints y envía sectionId también al buscar.
4. Mantener STAGING_ENABLED y PRECIOSCO_STAGING_ENABLED en false en TODAS las instancias.
   Detener Actions/scrapers durante la transición y respaldar la base compartida.
5. Aplicar solo migraciones pendientes, primero 2026-11-scraper-procesador.sql y después
   2026-11-bellapiel-identidad-secundaria.sql. No son seeds para ejecutar repetidamente.
   Conservar los requisitos previos: last_seen_at, f_unaccent, índices y triggers de jerarquía.
6. Ejecutar scripts/preflight_compartida.sql y verificar resultados contra los datos
   reales. Arrancar con ddl-auto=validate; no usar update/create para ocultar diferencias.

## Ajustes de datos además de ejecutar migraciones

- Raíces activas con los nombres del proyecto: Supermercado, Droguería y Cuidado facial.
  Cada tienda debe apuntar a su raíz; cada categoría hoja, producto y target al nivel correcto.
- Farmatodo/Farmanorte: URL curada, expected_ean comparable y presentación comprobada.
  min_content = max_content > 0 habilita respaldo exacto; G/ML/UN deben ser reales.
  Un objetivo con rango puede aceptar ofertas completas, pero no permite ese respaldo.
- Targets de facial: revisar las búsquedas por ingredientes. El parche de conflictos
  NO resuelve la asignación de categoría por primer target. No habilitar creación
  automática desde objetivos ambiguos sin curaduría o una regla de clasificación validada.
- No trasladar el maestro sospechoso de Aderma ni precios del ensayo. Cutis y Medipiel
  discrepan; confirmar etiqueta/fabricante antes de definir una corrección.
- CeraVe 8214 y Endocare 6558: revisar categoría de maestro y targets; no borrar los
  vínculos correctos por encontrarse también en búsquedas de otra categoría.
- ean_size_overrides: solo correcciones documentadas de una única presentación por EAN.
  store_product_links: genéricos reales de 1000 G y SKU/peso publicados. No usar IDs del ensayo.
  store_sku_ean_map: verificar tienda/código/EAN y evidencia; no importar equivalencias de ejemplo.
- Revisar PENDING legado sin run_id: no agrupar distintas horas como una corrida para
  elegir el precio mínimo. Nuevas corridas deben registrar su UUID y cierre.

## Configuración y comprobaciones

SECONDARY_IDENTITY_STORES contiene nombres exactos normalizados (Bellapiel y Bella Piel
no son iguales por el espacio). Conservar score 0.90, margen 0.08 y límite 100 durante
la validación. Revisar que reglas de dominios no nombren Repuestos si aún no existe.
No crear una categoría ficticia solo para quitar una advertencia.

Backend: Java 25, PostgreSQL aislado completo y `mvnw clean verify -Dsurefire.excludedGroups=`.
Frontend: npm ci, npm run build, eslint de los archivos cambiados y flujo en navegador.
Scrapers: fixtures Python y una corrida controlada en el clon, no en la compartida.
Antes de habilitar procesamiento compartido, cerrar las discrepancias de datos y repetir
el ensayo desde un respaldo previo; una suite verde no verifica identidad física.

Al habilitar, una sola instancia procesa y se revisan los rechazos y mapas aprendidos.
El cambio de filtros no limita la ingesta del procesador; solo el buscador del visitante.
