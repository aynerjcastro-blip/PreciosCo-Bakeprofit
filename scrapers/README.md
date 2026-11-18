# Scrapers y procesador de PreciosCo

Python **3.12**, Java **25** y PostgreSQL. Base de estos cambios:
`feature/update-scraper`, commit `eed7d5c`.

## Qué se implementó

| Hallazgo de los adjuntos | Comportamiento |
| --- | --- |
| Piezas confundidas con packs / atún drenado | Olímpica lee `measurement_unit` y `unit_multiplier`; Jumbo lee `measurement_unit_un` y `unit_multiplier_un` (multiplicador en kg/l). Prioridad a especificaciones; nombre/descripción solo si faltan ambas. Un campo estructurado inválido queda sin tamaño. Sin ficha, `500 G X16 Unds` es ambiguo y `SIXPACK` multiplica por seis. |
| EAN de una URL distinto del esperado | `scrape_targets.expected_ean`, validado/normalizado por `Gtin` en el procesador; rechazo con motivo. La migración recupera etiquetas `[EAN]` de targets con URL. Nuevos targets deben llenar explícitamente la columna. |
| Página sin presentación | Solo se usa el tamaño exacto del target (`min_content = max_content > 0`) con EAN esperado verificado. Un dato parcial contradictorio no se reemplaza. |
| Conflictos de contenido | `ean_size_overrides` guarda correcciones documentadas por GTIN-14. El tamaño efectivo se usa en screening, conflictos y resolución; el staging conserva el dato original y los productos existentes no cambian. |
| Más de un precio por corrida | UUID por ejecución y `scrape_runs.completed_at`. Una tienda/identidad obtiene el menor precio válido, desempate por menor ID; otras ofertas quedan PROCESSED con `duplicada en la corrida`. Dos SKU asignados al mismo genérico compiten por precio/kg. |
| Peso variable | Asociación manual `(store_id, store_sku) → product_id`. Genérico activo, sin EAN, marca ni referencia, de 1000 G y de la misma categoría hoja del target. Se divide el precio del empaque por `sale_weight_kg`; sin asociación/peso hay rechazo explícito. |
| Actions falla en cadena | Matriz de tres dominios con `fail-fast: false`, selectores separados y `cancel-in-progress: false`. Éxito continúa fuera del valor por defecto. |

La corrección de `GRX` y `buscar_urls.py` con tamaño y `falta_en` ya existían en la base; se conservaron. Farmatodo lee su estado de página, Farmanorte su JSON-LD; no se agregan búsquedas prohibidas, APIs privadas ni equivalencias por nombre.

## Despliegue

1. Mantener `STAGING_ENABLED=false` y detener ejecuciones de scrapers durante el cambio.
2. Probar `scripts/2026-11-scraper-procesador.sql` en una base de pruebas, revisar sus consultas finales y aplicarlo después a la compartida. El script agrega esquema; no crea asociaciones ni corrige productos/precios existentes. Es una migración de una sola aplicación, no un seed repetible.
3. Desplegar juntos backend y los tres scrapers. Hibernate usa `ddl-auto=validate`; necesita la migración primero.
4. Revisar `scripts/verificar_targets.sql` y cargar únicamente overrides/asociaciones comprobadas. No ejecutar los DELETE ni los IDs históricos de los adjuntos: no describen necesariamente la base actual.
5. Ejecutar el scraper elegido y después activar una única instancia del procesador. Revisar UNCLASSIFIED y sus motivos.

Reversión: detener ambos procesos, exportar `ean_size_overrides` y `store_product_links`, restaurar el código anterior y aplicar `2026-11-scraper-procesador-reversion.sql`. Se pierde esa configuración y los metadatos de corrida, no el catálogo ni sus precios. `target_url` se conserva porque puede preexistir.

## Corridas y concurrencia

Cada ejecución cierra su UUID tras recorrer los objetivos, aunque alguna tienda/target haya fallado: solo se procesan las ofertas que sí se guardaron. Un proceso interrumpido abruptamente deja su corrida abierta y sus filas PENDING fuera del procesador. Revisar logs y reejecutar; no marcarla completa automáticamente si puede seguir escribiendo.

Las ofertas antiguas sin `run_id` conservan el procesamiento individual. **No se deduplican por una ventana supuesta de una hora**: podría mezclar dos ejecuciones reales. Para procesar un lote antiguo como corrida, el operador debe identificar sus IDs/timestamps a partir del log y asignarles un UUID cerrado en una transacción antes de habilitar el procesador.

Un advisory lock transaccional serializa la ingesta antes de bloquear filas, para evitar interbloqueos al examinar el mismo EAN desde dos workers. Se conserva una transacción/conexión por ingesta y la publicación del evento de precio después del commit. Se prioriza consistencia sobre paralelismo, acorde al pool de dos conexiones del proyecto.

## Curaduría de peso variable y tamaños

Consultar primero los datos reales:

```sql
SELECT o.id, s.name AS tienda, o.store_id, o.store_sku, o.ean,
       o.name, o.price, o.sale_weight_kg, t.category_id, o.reason
FROM scraped_offers o JOIN stores s ON s.id = o.store_id
JOIN scrape_targets t ON t.id = o.scrape_target_id
WHERE o.reason LIKE 'peso variable%' ORDER BY o.id;
```

Crear manualmente cada genérico con `ean`, `brand_id`, `manufacturer_ref` nulos,
`net_content=1000`, `unit_base='G'`, `unit='1 kg'` y un nombre explícito como
`Pechuga de pollo sin piel (precio por kg)`. No se incluyen 20–30 productos inventados:
el corte, la preparación y qué marcas son equivalentes requieren curaduría real.

Los targets de peso variable dejan `expected_ean` nulo: esos códigos son locales, no GTIN comparables entre tiendas.

Después insertar en `store_product_links` la tienda, su SKU exacto, el ID del genérico
y un `reason` que justifique la comparación. Para un tamaño corregido, insertar en
`ean_size_overrides` el GTIN-14 normalizado, contenido, unidad y evidencia en `reason`.
No usar un override global si el mismo EAN se reutiliza para una lata y un pack:
esos listados deben corregirse o desactivarse individualmente.

El peso de Jumbo sale de `items[0].measurementUnit='kg'` y `unitMultiplier`.
En Olímpica se admite además contenido estructurado en gramos / 1000.
No se deduce peso del nombre ni del precio de lista. El precio original permanece
en `scraped_offers.price`; `prices.value` corresponde a 1 kg para esos genéricos.
Al crear un enlace, revisar las unidades publicadas en la tienda, especialmente
si el precio de la API ya estuviera expresado por kg.

## Uso local y Actions

```bash
python -m venv .venv
# Activar el entorno según el sistema operativo
pip install -r scrapers/requirements.txt
cd scrapers/supermercados
SOLO_TIENDAS=d1,jumbo,olimpica python scraper_supermercados.py
```

Definir `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, opcionalmente `DB_PORT` y
`DB_SSLMODE=require`. Para droguerías: `scrapers/droguerias/scraper_droguerias.py`.
Para facial: `scrapers/facial/scraper_cuidado_facial.py`.

Actions usa `.github/workflows/scrape-precios.yml`: `dominios` y filtros por dominio.
El botón manual aparece cuando el workflow existe en la rama predeterminada; no se
agrega un trigger de push que escriba automáticamente en la base compartida.

Para buscar URLs pendientes: ejecutar `scripts/consulta_faltantes.sql`, exportar
CSV y usar `python scrapers/buscar_urls.py --csv rebaja.csv`. El script solo propone
candidatas. Revisar marca/presentación, crear o corregir objetivos con `expected_ean`,
correr el scraper y consultar `scripts/verificar_targets.sql`. Mantener etiquetas
`término [EAN]` cuando exista la restricción única `(store_id, lower(search_term), unit_base)`.
Una candidata no verifica un EAN y nunca debe insertarse como una oferta ficticia.

## Pruebas

```bash
pip install pytest
PYTHONPATH=scrapers pytest -q scrapers/tests
cd backend
bash mvnw test
# Solo contra una base de pruebas con el esquema completo y la migración:
bash mvnw test -Dgroups=integration -Dsurefire.excludedGroups= \
  -Dtest=StagingProcessorIntegrationTest,BakeprofitApplicationTests
```

Las pruebas de integración requieren `SPRING_DATASOURCE_URL`, usuario/contraseña
y `JWT_SECRET` local; el procesador programado permanece apagado. Solo procesan y
eliminan las filas que crean. Las pruebas del scraper usan fixtures: no confirman
precios actuales, robots.txt actuales ni la estructura actual de las tiendas reales.

### Verificación de esta entrega

Se ejecutaron 18 pruebas Python, 178 pruebas unitarias Java y 13 pruebas de integración
(incluido arranque con `ddl-auto=validate`) en PostgreSQL 17 local, con Java 25 y Python 3.12.
La migración, reversión, consultas de revisión y revocaciones de permisos se probaron
sobre la base aislada. `actionlint` validó el workflow. No se ejecutaron peticiones de
scraping a tiendas reales ni escrituras en la base compartida.

## Bella Piel: soluciones 2 y 3 (17-nov-2026)

Esta extensión requiere el paquete anterior (`408288c`) y agrega la migración
`scripts/2026-11-bellapiel-identidad-secundaria.sql`. Primero probarla en PostgreSQL
local, después aplicarla a la compartida y desplegar el backend actualizado.
El scraper facial ahora conserva `itemId` como `store_sku`; no modifica el EAN publicado.

El procesador sigue este orden para Bella Piel cuando el EAN es corto o falta:

1. Consultar la equivalencia de tienda/código local en `store_sku_ean_map`.
2. Si no existe, buscar productos existentes de marca, presentación y categoría exactas.
3. Elegir solo un nombre con similitud suficiente y ventaja clara frente al segundo.
4. Validar rango y EAN esperado del objetivo, registrar precio y equivalencia en una
   misma transacción. El código original queda en staging.

Si la equivalencia guardada ya no es compatible o el resultado es ambiguo,
la oferta queda UNCLASSIFIED con motivo. Revisar esos motivos antes de reintentar.
La tabla comienza vacía: no se insertan los EAN de ejemplo del PDF.

Configuración del backend:

| Variable | Valor inicial | Propósito |
| --- | --- | --- |
| `SECONDARY_IDENTITY_STORES` | `bellapiel` | Nombres exactos de tiendas autorizadas, separados por coma. Ajustar a `Bella Piel` si ese es el nombre real en la base. Vacío deshabilita el respaldo. |
| `SECONDARY_IDENTITY_MINIMUM_SCORE` | `0.90` | Similitud mínima del nombre. |
| `SECONDARY_IDENTITY_MINIMUM_MARGIN` | `0.08` | Diferencia mínima respecto al segundo candidato. |
| `SECONDARY_IDENTITY_CANDIDATE_LIMIT` | `100` | Más candidatos requieren revisión; no se elige de una muestra incompleta. |

SPF, concentración y números deben coincidir; las variantes protegidas se
centralizan en el comparador. No se cruza por similitud una presentación distinta.
La similitud es una inferencia conservadora, no prueba de equivalencia física.
Si falta la marca o el tamaño, no se aprende un mapeo. Los candidatos deben estar
ya procesados en el catálogo: correr primero las tiendas con GTIN válido y revisar
las ofertas sin clasificar después de procesarlas. La mera presencia de una oferta
PENDING de Cutis/Medipiel no constituye un producto maestro.

Consulta de revisión (solo lectura):

```sql
SELECT mapping.id, store.name AS tienda, mapping.store_sku, mapping.ean,
       product.name AS producto, mapping.reason, mapping.created_at
FROM store_sku_ean_map mapping
JOIN stores store ON store.id = mapping.store_id
LEFT JOIN products product ON product.ean = mapping.ean
ORDER BY mapping.created_at DESC;
```

Una corrección manual debe justificar el vínculo en `reason`. El backend vuelve a
validar marca, tamaño, categoría y nombre incluso para vínculos manuales.
Antes de revertir, exportar la tabla y detener el procesador; el script de reversión
borra equivalencias, pero conserva los precios ya registrados. No modifica precios
históricos ni reclasifica ofertas procesadas de forma automática.

Diseño: `SecondaryIdentityResolver` coordina el caso de uso; repositorios específicos
encapsulan la persistencia y `ProductNameSimilarity` permite sustituir la métrica sin
cambiar la ingesta. Las reglas se centralizan en configuración y constantes.

Verificación de esta extensión: 188 pruebas unitarias Java, 17 pruebas de integración
con PostgreSQL local (incluido el arranque con `ddl-auto=validate`) y 21 pruebas Python.
Se verificaron además migración, reversión y revocaciones de la tabla de equivalencias.
No se consultaron tiendas reales ni se modificó la base compartida.
