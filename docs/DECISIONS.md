# DECISIONS.md — PreciosCo / BakeProfit / ComprasCo

Registro de decisiones de equipo: desviaciones deliberadas del MVP y acuerdos tomados durante el desarrollo, para que no se lean como descuidos en una revisión de código o una evaluación.

**Actualizado el 21-nov-2026.** Este archivo conserva investigaciones fechadas y consolida las decisiones vigentes de la conversación. La visión está en [Alcance](<Alcance del Proyecto__PreciosCo.md>) y los requisitos/historias en [Análisis y especificación](<PreciosCo__Analisis y especificacion (historias, modelo de negocio, requisitos y trazabilidad).md>). Los tres documentos principales bastan para entender la entrega; los informes auxiliares no constituyen dependencias de esta documentación.

La referencia publicada comprobada es `feature/update-scraper@85f463a` (merge de develop). Los arreglos posteriores de Search y desconexiones se validaron como parches; su publicación en la rama y el despliegue compartido no se dan por confirmados. El estado detallado figura en «Revisión operativa consolidada (18-nov-2026)».

## Modelo de datos

- **Jerarquía de categorías**: `categories.parent_id` autorreferenciada. Las raíces representan Supermercado, Droguería, Cuidado facial y, cuando se habilite, Repuestos. `stores.category_id` apunta a raíz; productos y objetivos, a hoja. La existencia de código para una sección no demuestra que sus tiendas/objetivos y datos compartidos estén curados.
- **Jerarquía de dos niveles**: dominio (raíz) y categoría (hoja); `CategoryService` no admite un tercer nivel. Los productos cuelgan de la hoja, por ejemplo Supermercado › Granos y cereales. Se descartó convertir cada producto genérico ("Arroz") en una categoría: el comparador trabaja sobre un solo producto seleccionado, así que no mezcla productos distintos de una misma categoría.
- **Campo "marca"**: se integra al modelo en el Sprint 2 (antes estaba excluido). La exclusión se tomó cuando la marca no se podía obtener por separado; la API VTEX la entrega en un campo propio. Tabla `brands` con nombre único sin distinguir mayúsculas y `products.brand_id`.
- **Identidad del producto (ADR-01)**: los productos comerciales se identifican por GTIN comparable y presentación; los repuestos, por marca + referencia del fabricante. Un SKU no es identidad global: se admite como clave local para una equivalencia verificada o un genérico por peso, bajo las reglas posteriores. La identidad exige evidencia de marca y referencia. Un código OEM cuenta como referencia si identifica la misma pieza y su marca se verifica. Las equivalencias de compatibilidad entre vehículos o piezas quedan fuera del alcance.
- **EAN como GTIN-14**: se acepta con 8, 12, 13 o 14 dígitos y se guarda siempre rellenado con ceros a la izquierda hasta 14 (`products.ean`, único cuando no es nulo), para que un UPC-A y su EAN-13 equivalente coincidan. La clase `Gtin` es la única fuente de la regla: `classify` reconoce el tipo de código (GTIN-8, GTIN-13, medida variable o empaque) y `assess` devuelve un veredicto: INVALID (formato o dígito verificador), PLACEHOLDER (puros ceros), STORE_INTERNAL (circulación restringida: EAN-8 con prefijo 0 o 2, EAN-13 con prefijo 02, 04 o 2), VARIABLE_MEASURE (GTIN-14 con indicador 9: no es identidad global; puede entrar en la ruta curada de peso variable) o COMPARABLE. Solo un código COMPARABLE identifica un producto. Los empaques (GTIN-14 con indicador del 1 al 8) son productos distintos de la unidad suelta, porque su precio también es distinto. La API muestra el código con `toDisplay` (8 o 13 dígitos según su tipo).
- **Referencia del fabricante**: se normaliza a solo letras y números ASCII en mayúsculas (la Ñ y las tildes se eliminan; es aceptable porque las referencias de fabricante son ASCII), con una columna generada (`manufacturer_ref_norm`) y la misma regla en `ManufacturerReference.normalize()`. Una referencia exige marca (CHECK) y la pareja marca + referencia normalizada es única. La búsqueda por referencia (HU-33) normaliza lo que escribe el usuario con la misma regla.
- **Contenido neto y staging (ADR-03)**: cada producto tiene contenido neto (`net_content`, en la unidad base) y unidad base (`unit_base`: G, ML o UN), que van juntos o no van; "Arroz Sonora 500 g" y "Arroz Sonora 1000 g" son productos distintos. El scraper escribe en `scraped_offers` y `scrape_targets` define qué se busca en cada tienda (ver Fase 3). Esta decisión es posterior a ADR-01 y vino del cambio del scraper a la API VTEX.
- **Marcas**: solo `BrandService` las crea, con `INSERT ... ON CONFLICT DO NOTHING` sobre el índice `lower(f_unaccent(name))` y búsqueda con la misma expresión (no `IgnoreCase`, que usa `upper` y no es simétrico en Unicode); así dos creaciones simultáneas obtienen la misma marca, y "Alquería" y "ALQUERIA" son una sola: las tiendas VTEX publican las marcas en mayúsculas y sin tildes, y con `lower(name)` habrían quedado dos y el filtro por marca (HU-35) mostraría ambas (`scripts/2026-11-marcas-sin-tildes.sql`). La marca conserva el nombre con el que se registró primero. El índice y las dos consultas de `BrandRepository` deben tener exactamente la misma expresión, o el `ON CONFLICT` no encuentra el índice y el INSERT falla. No hay endpoints de marcas: su administración es del panel administrativo (EP-09).
- **Pruebas de integración**: las pruebas que necesitan PostgreSQL real llevan `@Tag("integration")`, quedan fuera de `mvn test` y solo corren si `SPRING_DATASOURCE_URL` está definida (si no, se omiten, para no escribir en la base compartida). Se ejecutan contra la base de pruebas con `./mvnw test "-Dgroups=integration" "-Dsurefire.excludedGroups="` (en PowerShell, cada `-D` entre comillas). La primera es `BrandConcurrencyIntegrationTest`, que comprueba que dos creaciones simultáneas de la misma marca obtienen la misma fila. En el procesador, supermercado, droguería y cuidado facial crean marcas solo desde el campo de marca de la tienda; en repuestos solo se emparejan marcas existentes y una desconocida queda sin clasificar. `ProductSearchIntegrationTest` (búsqueda con filtros y paginación) y `PriceCompareIntegrationTest` (tiendas inactivas en el comparador) siguen la misma regla. `StagingProcessorIntegrationTest` solo procesa las ofertas que inserta (`StagingProcessor.processOffers` con sus propios ids) y nunca llama a la corrida completa ni a la retención, para no tocar ofertas ni filas ajenas aunque apunte a la base equivocada.
- **URL e imagen del producto en la tienda**: se siguen guardando en `prices.product_url` y `prices.image_url` (SCRUM-49). No se crea una tabla `store_listings` en el Sprint 2.
- **Esquema por scripts y `ddl-auto=validate`**: Hibernate no crea ni modifica tablas (`spring.jpa.hibernate.ddl-auto=validate` en `application.properties`). Todo cambio de esquema va en un script SQL versionado en `scripts/`, con su script de reversión, y las entidades JPA se mantienen sincronizadas con él. Como `application-local.properties` está en `.gitignore` y sobrescribe a `application.properties`, cada integrante borra de su archivo local la línea `ddl-auto`. `validate` solo detecta tablas o columnas faltantes y cambios de tipo base; longitudes, precisión, nulabilidad, CHECK e índices se comprueban con la consulta de sincronización de cada script. Los CHECK, índices parciales y funcionales y columnas generadas se declaran solo en el script. El script se prueba primero en una base de pruebas (un segundo proyecto de Supabase o PostgreSQL local; el plan Free no incluye ramas) y después se corre en la base compartida, avisando al equipo.
- **Convención de FK**: `fk_tabla_tabla` (por ejemplo `fk_ratings_products`).
- **Jerarquía en la base**: triggers en `products`, `scrape_targets` y `categories` exigen que productos y objetivos de búsqueda cuelguen de una hoja, que no haya tercer nivel y que ningún cambio de padre deje productos, objetivos o tiendas en el nivel equivocado (`scripts/2026-11-jerarquia-categorias-trigger.sql`). Se agregó porque la regla vivía solo en Java y unos datos de prueba insertados directo en la tabla dejaron productos en la raíz y en una hoja equivocada sin que nada lo detuviera. No reemplaza la validación de Java, que sigue dando el mensaje al usuario; el trigger es la última barrera para scripts y cargas manuales y responde con SQLSTATE `23514` (`check_violation`), que Spring traduce a `DataIntegrityViolationException`. Los triggers no revisan las filas existentes: antes de correr el script, sus consultas de sincronización deben dar 0. Se eligieron triggers y no `CHECK` porque un `CHECK` no puede consultar otra tabla.

## Seguridad de la base de datos

- **Acceso a datos desde procesos de servidor (SCRUM-297)**: Supabase publica cada tabla del esquema `public` por su API REST, y la clave `anon` está diseñada para ser pública. Por eso todas las tablas tienen Row Level Security activada sin políticas, los roles `anon` y `authenticated` no tienen permisos sobre tablas ni secuencias, y los privilegios por defecto impiden que las tablas nuevas los reciban (`scripts/2026-11-seguridad-api-supabase.sql`). El backend se conecta como `postgres`, dueño de las tablas, y RLS no le aplica. Ningún componente del proyecto (frontend, scraper ni funciones externas) usa la API REST de Supabase; el scraper se conecta directo a PostgreSQL.
- **Toda tabla nueva** se crea con `ENABLE ROW LEVEL SECURITY` en su script, aunque los privilegios por defecto ya la protejan.
- **Toda función nueva en `public` (SCRUM-304)**: Supabase publica por `/rpc` las funciones del esquema `public`, y sus privilegios por defecto dan `EXECUTE` a `PUBLIC`, `anon` y `authenticated` sobre cada función nueva. Por eso el script que crea una función revoca `EXECUTE` a los tres por nombre (revocarlo solo a `PUBLIC` no basta), y su consulta de sincronización lo verifica con `has_function_privilege`: las tres comprobaciones deben dar `false`. El backend se conecta como `postgres`, dueño de la función, así que no necesita un `GRANT`.

## Fase 3 — Carga de precios y comparadores (Sprint 2)

- **Éxito en pausa (11-nov-2026)**: tras los bloqueos 403 registrados, se conserva la estrategia pero se excluye de la selección inicial d1,jumbo,olimpica. No se esquiva la verificación con proxies, cabeceras de navegador ni automatización de navegador. La reactivación exige comprobar acceso y resultados; no hay un reintento diario automático confirmado en el workflow actual. Si persiste, gestionar acceso/autorización. Los controles de bloqueos evitan insistir indefinidamente.

- **Scraper con la API VTEX**: D1, Éxito, Jumbo y Olímpica usan VTEX y exponen una API pública de catálogo en JSON (`/api/catalog_system/pub/products/search`; Éxito con el prefijo `/io/`; D1 en su dominio nuevo, `www.d1.com.co`). Reemplaza a Selenium: un solo cliente con la URL base de cada tienda, pausas entre peticiones y un User-Agent que identifica al proyecto. Riesgos aceptados: es una API pública sin acuerdo con las tiendas, y el precio puede depender de la región (se verifica contra la web con ubicación en Valledupar antes de confiar en él).

- **Precios regionales (Valledupar)**: la búsqueda de VTEX devuelve el precio del vendedor por defecto, que no siempre es el de Valledupar (en una muestra de 22 productos, 6 tenían otro precio, hasta 6,6 % en Jumbo, y varios marcados como disponibles no se vendían en Valledupar). Por eso, en las tiendas con vendedores por ciudad (Éxito, Jumbo y Olímpica), el scraper busca el vendedor que atiende las coordenadas de Valledupar (`/api/checkout/pub/regions`) y toma el precio de venta y la disponibilidad de una simulación de compra con ese vendedor (`/api/checkout/pub/orderForms/simulation`, una petición por target con todos sus productos). Si ese vendedor no tiene el producto, prueba el vendedor de la búsqueda (marketplace) con entrega en Valledupar; si ninguno lo vende, la oferta no se guarda. D1 no vende en línea en Valledupar (solo atiende
  domicilios en otras ciudades): se usa el precio nacional de la búsqueda. Si una tienda regional no devuelve vendedor para Valledupar, sus targets fallan y se dispara la alerta de cero resultados: no se cae al precio nacional sin avisar.

- **Alerta de cero resultados**: si una tienda no inserta ninguna oferta en toda la corrida, el scraper termina con código 1 y GitHub Actions marca la corrida como fallida. Un target vacío no basta, porque un producto agotado es normal.
- **Tabla de staging**: el scraper en Python solo escribe datos crudos en `scraped_offers`. Un proceso `@Scheduled` en Java normaliza, empareja por EAN, crea productos, inserta precios y conecta el detector de bajas. Resuelve que no hubiera un punto en Java donde enganchar el aviso.
- **Historial de precios por periodos**: cada fila de `prices` es un periodo en el que el precio no cambió: `registration_date` es cuándo empezó y `last_seen_at` es la última vez que la tienda lo confirmó. Si el precio de hoy es igual al último de ese producto en esa tienda, solo se actualiza `last_seen_at` (y la URL y la imagen, solo si la oferta las trae: una oferta sin imagen no borra la que ya funcionaba); si cambió, se inserta una fila nueva. Así el comparador muestra un precio verificado hoy sin perder el historial (antes se movía la fecha y se perdía cuándo empezó cada precio), la tabla crece solo con los cambios y dos filas consecutivas siempre tienen valores distintos, que es lo que compara el detector de bajas (RF27). `value`, `registration_date`, el producto y la tienda nunca se modifican; nunca se usa `ON CONFLICT DO UPDATE` sobre `prices`.
- **`prices.source`**: guarda el nombre del producto tal como lo publica la tienda (por ejemplo, "Arroz Diana blanco x5kg"), para rastrear de qué oferta salió cada precio. Se recorta a la longitud de la columna.
- **Retención del staging**: al final de `runOnce` se borran PROCESSED cuyo processed_at supera 2 días y UNCLASSIFIED cuyo processed_at supera 7 días, configurables. PENDING y CONFLICT no se borran por esa tarea; las corridas tampoco se purgan aquí. Las rechazadas deben revisarse/exportarse antes de su vencimiento; los conflictos quedan retenidos hasta revisión explícita.
- **Procesador de staging: una transacción por oferta (HT-10)**: `StagingProcessor` pagina por (scraped_at, id) y orquesta sin transacción global. Cada ingesta toma primero un advisory lock transaccional global, luego bloquea la oferta y vuelve a verificar PENDING/corrida cerrada. Puede modificar pares relacionados por duplicación o conflicto. Un fallo revierte esa ingesta y se intenta marcar UNCLASSIFIED en otra transacción, sin detener el lote; si también falla el marcado, puede seguir pendiente para otra pasada. Los eventos se publican después del commit. `batch-size=50` es paginación, no tope total ni dry-run; Ctrl+C no revierte lo confirmado. Se mantiene el límite de conexiones de RP-04.
- **Evento de precio nuevo**: por cada fila nueva en `prices` se publica `PriceRegisteredEvent`, donde HT-11 conecta el detector de bajas. Lo publica el orquestador después del commit de la oferta, fuera de toda transacción; el listener usa `@EventListener` con su propia transacción. Se descartaron `BEFORE_COMMIT` (un error en el aviso aborta la transacción y se perdería el precio) y `@Async` (toma una segunda conexión con el pool de 2). Un precio repetido no publica evento. Riesgo aceptado: si la aplicación se detiene entre el commit y la publicación, ese aviso se pierde.
- **Aviso inmediato de bajas (HT-11)**: `PriceDropListener` escucha `PriceRegisteredEvent` y, por cada seguimiento activo del producto, llama a `NotificationService.detectForFollow`, que ya aplica RN-15 y RN-16 en su propia transacción y nunca repite un aviso. Se descartó un evaluador aparte que enviaba la alerta por HTTP a una URL de staging: el procesador vive en el mismo backend, no expone ese endpoint y la regla ya existía en `NotificationService`. El detector programado (`PriceDropDetector`, cada 24 horas) se conserva como respaldo para el evento perdido entre el commit y la publicación; no duplica avisos. Un cambio de tamaño nunca llega como baja (SCRUM-267): el tamaño de un producto no cambia (RN-02) y otro tamaño es otro producto.
- **Producto existente con otro tamaño**: si el EAN ya existe con otro contenido neto o unidad, la oferta queda UNCLASSIFIED; un producto existente nunca se modifica desde el procesador (RN-02).
- **Conflicto de tamaño entre ofertas**: antes de crear un maestro comercial nuevo, `SizeConflictCheck` consulta evidencias PENDING y UNCLASSIFIED del mismo GTIN de corridas cerradas o legado. No se limita al UUID actual. Revalida identidad, precio, objetivo activo y categoría, pero para esa evidencia comercial omite solo la aceptación por rango/unidad del target: una fila de 100 G rechazada por un target ML todavía puede contradecir otra de 100 ML. Usa tamaños efectivos y overrides. Ante discrepancia no crea producto/precio; marca las relacionadas aún PENDING como UNCLASSIFIED y conserva los motivos de las ya rechazadas. No revisa PROCESSED como evidencia ni sanea maestros ya existentes. El estado CONFLICT del scraper facial es otra ruta, descrita en la revisión consolidada.
- **Corrección de un tamaño equivocado**: verificar etiqueta/ficha/fabricante y la identidad física antes de cambiar un maestro. Un override modifica el tamaño efectivo de evaluación, no `products` ni `prices`. Si el maestro ya es incorrecto, preparar saneamiento específico y revisar sus precios y relaciones; no asumir que todos sus precios se conservan válidos. Reabrir solo IDs comprobados todavía retenidos o capturar observaciones nuevas. Se retira el ejemplo de UPDATE por un EAN histórico como receta general: no autoriza limpiar catálogos ni reprocesar en masa.

- **Repuestos en el procesador**: hasta que `scraped_offers` tenga la referencia del fabricante (Sprint 3), toda oferta de repuestos queda UNCLASSIFIED con "identidad por referencia pendiente (Sprint 3)". La regla de marcas existentes y la identidad pendiente son dos propiedades separadas (`existing-brands-only-domains` e `identity-pending-domains`, con el nombre de la categoría raíz); el cambio de `identity-pending-domains` se condiciona a que staging y el procesador soporten y validen la referencia del fabricante. Al arrancar se advierte en el log si un dominio configurado no coincide con ninguna categoría raíz.
- **Activación del procesador**: en la referencia `85f463a` el valor de respaldo es `STAGING_ENABLED=false`; fijarlo expresamente en pruebas y revisar perfiles/variables que puedan sobrescribirlo. Solo una instancia designada lo activa contra la compartida. El scheduler espera `PT2M` al arrancar y `PT8H` desde que termina la pasada anterior; no son tres corridas exactas por día ni un cron del scraper. Las pruebas llaman a `processOffers` con scheduler apagado. La configuración de ejecución no demuestra despliegue ni éxito de datos.
- **`scrape_targets` y filtros anti-basura**: define tienda, categoría, término/fq o URL curada, unidad, límites, identidad esperada y actividad. No clasifica semánticamente por ingredientes. El backend valida unidad/rango en rutas comerciales y secundarias; peso variable exige su genérico y peso publicado con reglas propias. Un target activo y con nombre plausible no prueba que la categoría sea correcta. El preflight verifica jerarquía y datos; no sustituye la curaduría.
- **Tamaño**: los lectores dependen de la tienda. Jumbo/Olímpica priorizan campos estructurados; La Rebaja usa sus especificaciones; las páginas de droguería usan nombre/ficha; facial contrasta fuentes y conserva contradicciones como CONFLICT. Un dato estructurado inválido no se reemplaza por una conjetura y no se convierten G y ML entre sí.
- **Reinicio de datos**: acuerdo histórico del arranque del nuevo modelo cuando no había usuarios reales; no es autorización vigente para borrar productos, precios o relaciones de la base compartida. La entrega actual usa migraciones y curaduría documentada, con respaldo y ensayo aislado. Toda reparación histórica requiere un procedimiento específico.
- **Tiendas inactivas en el comparador**: el comparador (`findRecentPricesByProduct`) y el precio más bajo de Seguidos (`findMinPriceByProduct`) ignoran las tiendas inactivas, igual que la búsqueda. Antes las mostraban, aunque el comentario de `prices.js` decía lo contrario; así la búsqueda, el comparador y Seguidos usan el mismo conjunto de tiendas.
- **Comparación entre dominios**: un producto que se vende en supermercado y en droguería se compara en todas las tiendas que lo venden (RF07). Se descartó ocultar los precios de otro dominio.
- **Secciones por dominio (ADR-02)**: una sección muestra los productos con precio en al menos una tienda de ese dominio. Se descartaron categorías con varios padres (convierte la jerarquía en un grafo) y duplicar tiendas mixtas por dominio (parte el promedio de la tienda). Si se seleccionan tiendas mixtas, `store` pasará a tener varios dominios con una tabla intermedia.
- **Selección de tiendas**: para droguería y cuidado facial se prefieren tiendas sobre VTEX, para reutilizar el mismo cliente.
- **Índices**: `idx_products_category` e `idx_prices_product_store` (este último lo pide ADR-02 y también lo usa la búsqueda por precio vigente; ver "Búsqueda de productos"). No se crea `pg_trgm` por ahora: con 5.000 productos y 60.000 precios la búsqueda tarda unos 35 ms (medido en SCRUM-140). Como `f_unaccent` es `IMMUTABLE`, si el catálogo crece se puede crear un índice GIN de trigramas sobre `lower(f_unaccent(name))` sin cambiar el código.
- **Prueba de datos de repuestos**: antes de construir EP-06 se toman 20 productos por tienda y se mide cuántos tienen marca y referencia extraíbles y cuántas parejas aparecen en 2 tiendas o más. Si menos del 30 % aparece en 2 tiendas o más, EP-06 se replantea como listado con buscador por referencia y no como comparador.
- **Despliegue y rendimiento**: el despliegue se aplaza (el docente no lo exige por ahora). El rendimiento queda como tareas extra; la prueba de carga nunca se corre contra la base compartida de Supabase.
- **Patrón de diseño del scraper**: Strategy, del catálogo GoF.

## Fase 3 — Selección de tiendas de droguería y cuidado facial (SCRUM-168, 10-nov-2026)

**Investigación histórica.** Los resultados y propuestas de este bloque corresponden a su fecha. Las decisiones de páginas curadas, identidad secundaria, SKU y conflictos descritas en las actualizaciones posteriores son las reglas vigentes; las cifras de muestras no acreditan cobertura actual ni autorizaciones de las tiendas.

Evidencia: dos ejecuciones de `verificar_tiendas.py` (robots.txt, API de catálogo, términos), lectura completa de los términos de las
tiendas que los publican y revisión de las páginas públicas. Lo marcado **PENDIENTE** no se pudo comprobar y no se da por
verificado. Este análisis no es asesoría legal.

La investigación del 10-nov y su preselección de cuidado facial se conservan como antecedente. La selección vigente consta en la entrada del 20-nov-2026.

### Plataformas y tipo de tienda

- **VTEX**: Drogas La Rebaja, Farmanorte, Cutis. **Shopify**: Ánimus Lab, Botanikalia. **Aplicación propia** (Angular): Farmatodo
  y Cruz Verde. Droguería La Economía: sin determinar.
- **Mixtas** (venden de más de un dominio): Farmatodo, Cruz Verde, Farmanorte (droguería y dermocosméticos), Drogas La Rebaja
  (droguería y sección "Piel Radiante") y Cutis (cuidado facial y medicamentos). **Especializadas**: Ánimus Lab y Botanikalia
  (marcas propias de cuidado facial).

### Resultado por tienda

**Drogas La Rebaja** (droguería · riesgo bajo-medio)

- Plataforma: **VTEX** (la API responde).
- robots.txt: API de catálogo **permitida**; prohíbe `/busca/*`, cuenta y pago.
- EAN: **sí**.
- Datos útiles: `Producto OTC` (SI/NO), `Presentacionunidadmedida` ("BLISTER X 10"), `Principio activo`, `Kit`; marca.

**Farmatodo - Droguería** (droguería · riesgo medio)

- Plataforma: aplicación propia (Angular).
- robots.txt: páginas `/producto/…` y sitemaps **permitidos**; prohíbe `/buscar*`, `/busqueda*`, cuenta y pago; bloquea por nombre a "Scrapy 2.7.1/2.8.0".
- EAN: **sí**, en el estado de la página (`barcode`); el `gtin` del JSON-LD es el id interno de la URL (corregido el 14-nov, ver "Scrapers de droguería").
- Datos útiles: nombre, marca, precio e imagen en el estado de Angular que trae el HTML; JSON-LD con nombre, marca y precio.

**Farmanorte** (droguería · riesgo medio)

- Plataforma: **VTEX**.
- robots.txt: **API (`/api/`) y búsquedas `map=ft` PROHIBIDAS**; permitidos páginas de producto `/…/p` y sitemap.
- EAN: **sí**, en el campo `mpn` del JSON-LD; el `gtin` es el código interno de la tienda. Marca: solo "Generica" (corregido el 14-nov, ver "Scrapers de droguería").
- Datos útiles: presentación en el nombre ("\* 10 TAB") y `Cantidad de unidades`.

**Cruz Verde** (droguería · riesgo **alto**, ver términos)

- Plataforma: aplicación de una sola página (Angular).
- robots.txt: prohíbe `/search`, bolsa, cuenta y pago; productos y sitemap permitidos.
- EAN: PENDIENTE.
- Datos útiles: el HTML llega vacío sin JavaScript; los datos vienen de una API interna sin documentar.

**Droguería La Economía (Eticos SAS)** (droguería · riesgo **alto**)

- Plataforma: sin determinar (el sitemap vive en `/sitemaps/la_economia/`).
- robots.txt: páginas de producto `/p/…` y sitemap **permitidos**; prohíbe `/search`, cuenta, checkout y pasos del pedido; **`Crawl-delay: 3600`** para todos los agentes (Googlebot tiene acceso libre).
- EAN: PENDIENTE.
- Datos útiles: nombre con presentación y precio en la página de producto.
- Riesgo: una petición por hora hace inviable un ciclo diario (ver hallazgo 13).

**Cutis** (cuidado facial · riesgo bajo)

- Plataforma: **VTEX**.
- robots.txt: API **permitida**; prohíbe `/busca/*`, `/img/*`, cuenta y pago.
- EAN: **sí**.
- Datos útiles: `Tipo de presentación` ("30 ML"), marca, categoría `/Rostro/`.

**Farmatodo - Cuidado facial** (cuidado facial · riesgo medio)

- Igual que Farmatodo - Droguería en plataforma, robots.txt y datos útiles.
- EAN: igual que Farmatodo - Droguería (`barcode`); sin probar en esta tienda.

**Ánimus Lab** (cuidado facial, marca propia · riesgo medio)

- Plataforma: **Shopify**.
- robots.txt: `/products.json` y `/collections/all` permitidos.
- EAN: **no** (`/products.json` no expone código de barras).
- Datos útiles: 44 productos (10 kits); SKU en 43; marca consistente; `compare_at_price` (precio de lista) en 57 variantes; **27 variantes agotadas**.

**Botanikalia** (cuidado facial, marca propia · riesgo medio)

- Plataforma: **Shopify**.
- robots.txt: `/products.json` y `/collections/all` permitidos.
- EAN: **no**.
- Datos útiles: 40 productos (10 kits); **sin SKU**; marca escrita de dos formas; todo disponible; sin precios de lista.

### Hallazgos que cambian el diseño

1. **El EAN solo está en La Rebaja y Cutis.** *(Corregido el 14-nov: Farmatodo y Farmanorte también lo traen en la página del
   producto; ver "Scrapers de droguería".)* ADR-01 (emparejar por EAN) no alcanza a Ánimus Lab ni Botanikalia. Hace falta emparejar por marca normalizada + nombre o principio activo + contenido neto (ADR-03), o la vista de
   equivalentes de ADR-04. En droguería, los productos genéricos del catálogo ya se definen sin marca ("Acetaminofén 500 mg").
2. **Los kits de La Rebaja comparten el EAN de la unidad con otro precio**: el blíster x10 cuesta $2.600 y el kit de caja x100
   ($18.820) trae el mismo EAN. Hay que excluir los listados con `Kit = Refkit`.
3. **Filtro de venta libre**: La Rebaja marca cada producto con `Producto OTC` (SI/NO) y el alcance excluye fórmula médica; se
   usa ese campo. En las demás tiendas el filtro es la lista de productos de `scrape_targets`.
4. **El precio depende de la ciudad o del canal.** La Rebaja lista `Ciudades Exentas` (códigos de municipio; parecen ser exentas de
   IVA); La Economía declara que sus precios difieren de los de las tiendas físicas y varían según la ciudad de entrega; Cruz Verde
   declara que los precios del sitio pueden diferir de los de sus puntos de venta; Farmatodo y Farmanorte piden elegir ciudad.
   La API sin región devuelve un precio por defecto de origen desconocido; verificarlo contra la web con ubicación en Valledupar
   antes de confiar en él. Cruz Verde sí cubre Valledupar en su venta en línea y en el retiro en droguería.
5. **Disponibilidad**: Cruz Verde marca "PRODUCTO SIN STOCK ONLINE" y su disponibilidad varía por localidad; en Ánimus Lab hay
   variantes con `available = false`. Un producto sin stock no se registra como precio vigente.
6. **Descargo de precios**: Cruz Verde declara que no responde por los precios que se muestren en otros sitios o buscadores;
   La Rebaja y La Economía se reservan cambiarlos en cualquier momento. El comparador debe mostrar la fecha de captura del precio
   (`prices.registration_date`) y el enlace a la tienda (`prices.product_url`), sin presentarlos como precio garantizado.
7. **Farmanorte**: su robots.txt prohíbe la API. Respetarlo implica usar solo sitemap y HTML, y aun así no trae marca real:
   aporta poco. *(Corregido el 14-nov: el EAN sí está, en `mpn`.)* Tampoco sirve para cuidado facial en la API consultada (0 resultados de niacinamida y retinol). Decisión del equipo:
   segunda ola solo por HTML, o excluirla.
8. **Farmatodo**: no usar las APIs ni las claves que aparecen en el código de la página (Algolia, `api-transactional`): no están
   documentadas para terceros. La ruta limpia es `sitemap-products.xml` + página de producto (JSON-LD), con ritmo bajo. Que bloquee
   a Scrapy por nombre indica que vigilan a los scrapers; el UA del proyecto no está bloqueado y debe identificarse.
9. **Cruz Verde**: los datos no están en el HTML, así que habría que usar la API interna que consulta la página. Sus términos
   prohíben la ingeniería inversa de cualquier parte del sitio y la reproducción total o parcial del sitio sin permiso escrito (ver
   términos). Se descarta como fuente a menos que haya autorización escrita.
10. **Shopify**: se usa `price` (lo que paga el comprador), no `compare_at_price`, y se descartan las variantes agotadas. Botanikalia
    escribe su marca de dos formas ("botanikaliacol" en 39 productos y "Botanikalia" en 1): normalizarla antes de `BrandService`, o
    se crearían dos marcas. Los kits, rutinas y calendarios (10 en cada tienda) se excluyen. El `agents.md` de las tiendas Shopify
    pide a los clientes automatizados identificarse (Web Bot Auth en peticiones servidor a servidor) y respetar `429` y
    `Retry-After`; `/products.json` no se menciona y robots.txt lo permite.
11. **El contenido neto (ADR-03) casi no viene estructurado en Shopify**: en Ánimus Lab solo 32 de 101 variantes traen ml o g en su
    título y ningún título de producto lo trae; mirando también la descripción, solo 14 de 34 productos sueltos (muchos son
    maquillaje). En Botanikalia, 3 de 30. Sin contenido neto no se puede calcular el precio por ml de forma automática.
12. **Buscar un activo en el texto sobrecuenta.** En Ánimus Lab, 27 de 44 productos mencionan "niacinamida" en el título o la
    descripción, pero solo 12 de los que no son kits la nombran con un porcentaje, y están en sérums, contornos de ojos, una crema
    corporal y esencias, donde no es el activo principal. Ningún título de Ánimus Lab nombra el activo ("Suero AZ Hybrid Clear",
    "Hydra Balance"). En Botanikalia hay exactamente un sérum de niacinamida en el título ("SERUM FACIAL NIACINAMIDA 10%",
    $62.900) y uno de retinol ("SERUM FACIAL BIO RETINOL", $72.900). En Cutis, la búsqueda de texto completo por "niacinamida"
    devuelve resultados útiles (Mela B3 de La Roche-Posay, Endocare Niacinal) mezclados con otros donde no es el activo principal
    (un sérum capilar). Clasificar por activo exige curaduría, no solo buscar la palabra.

13. **Crawl-delay**: solo Droguería La Economía lo declara (3600 s, una petición por hora para todos los agentes salvo
    Googlebot). Es una directiva no estándar que Google ignora, pero los rastreadores respetuosos la acatan. Con los 20
    productos del catálogo, un ciclo tardaría unas 20 horas (21 con el sitemap) y no admite crecer. Se descarta; si el equipo la
    quiere, pedir autorización o una frecuencia mayor a `servicioalcliente@droguerialaeconomia.com`. El scraper debe leer
    `Crawl-delay` de cada robots.txt y respetarlo por tienda.

### Selección propuesta el 10-nov-2026

- **Droguería (RF52, EP-04, HU-28/HU-29)**. Primera ola: **Drogas La Rebaja** (API, EAN, filtro OTC) y **Farmatodo - Droguería**
  (sitemap + HTML). Segunda ola: **Farmanorte** (solo HTML, sin EAN). **Cruz Verde**, solo con autorización escrita.
  **Droguería La Economía**, descartada por su `Crawl-delay` de 3600 s (hallazgo 13).
- **Cuidado facial (propuesta inicial; RF58, EP-05, HU-30/HU-31)**: **Cutis** (única con API y EAN), **Farmatodo - Cuidado facial**, **Ánimus Lab** (con
  autorización) y **Botanikalia**. Cumple el mínimo de 2 tiendas con nombre, presentación, precio y URL.

### Ajuste a ADR-02: Farmatodo como dos tiendas

- **Decisión del equipo**: Farmatodo se registra como **"Farmatodo - Droguería"** y **"Farmatodo - Cuidado facial"**, en lugar de
  una tabla intermedia tienda-dominio. ADR-02 había descartado duplicar tiendas mixtas porque parte el promedio de la tienda; se
  acepta ese costo.
- **Consecuencias**: (1) el promedio de calificaciones de Farmatodo queda partido; (2) los `scrape_targets` de las dos filas deben
  ser disjuntos por categoría, o un mismo producto llegaría con dos precios de "Farmatodo"; (3) la regla vale solo para tiendas que
  se scrapean en dos dominios (hoy, Farmatodo).

### Comparación por ingrediente activo (propuesta; requiere decisión y nuevo ADR-04)

- **Por qué la evidencia la favorece**: solo Cutis expone EAN en cuidado facial; Ánimus Lab y Botanikalia venden productos propios
  que ninguna otra tienda seleccionada vende. Con ADR-01 sus productos quedarían con un solo precio.
- **Idea del equipo**: comparar productos equivalentes de marcas distintas por ingrediente activo (niacinamida, retinol). Reabre la
  decisión de que el comparador trabaja sobre un solo producto y no mezcla productos distintos.
- **Opciones**: (A) solo el mismo producto (ADR-01). (B) vista complementaria "comparar equivalentes": categorías hoja por activo
  bajo Cuidado facial (por ejemplo "Sérum de niacinamida"), con precio por unidad de contenido y la concentración visible.
- **Lo que la evidencia obliga a ajustar en B**: (a) la lista de productos de cada categoría la define el equipo por curaduría
  (unos 5 a 8 por tienda), no una búsqueda automática de la palabra (hallazgo 12); (b) el contenido neto de los productos de
  Shopify se carga a mano para esa lista (hallazgo 11); (c) la concentración se registra por producto y se muestra, porque
  niacinamida 2 % no equivale a 10 %; (d) si falta el contenido neto, se muestra el precio sin precio por ml.
- **Recomendación**: B como complemento de A, empezando con una lista curada y corta.

### Robots.txt y términos de uso

- **robots.txt**: leído en los **8 sitios** (Farmatodo cuenta una vez aunque figure en dos filas; resumen en la tabla). El de Droguería La Economía se obtuvo copiándolo desde el
  navegador porque el script falla con `CERTIFICATE_VERIFY_FAILED` incluso con `certifi` (suele indicar que el sitio no envía su
  cadena de certificados completa). Para el User-Agent del proyecto permite las páginas de producto y el sitemap, prohíbe la
  búsqueda, el checkout, la cuenta y los pasos del pedido, y exige `Crawl-delay: 3600`. No se desactiva la verificación SSL.
- **Cruz Verde** (términos leídos completos, versión del 13-mar-2023): prohíbe "descifrar, descompilar, desensamblar, realizar
  ingeniería inversa o intentar derivar el código fuente o ideas subyacentes o algoritmos de cualquier parte del sitio web, así como
  modificar, traducir o crear trabajos derivados" y declara protegido todo el contenido del sitio, con la reproducción total o
  parcial del sitio prohibida sin permiso previo, expreso y por escrito. Aclara que sus precios pueden diferir de los de las
  droguerías y que no responde por precios publicados en otros sitios o buscadores. Pedir autorización por escrito (PQR en
  `buzondigital.cruzverde.com.co`) antes de cualquier extracción.
- **Farmatodo** (términos leídos completos): no mencionan el acceso automatizado a precios, pero prohíben la ingeniería inversa y
  crear obras derivadas de cualquier parte del sitio, y declaran protegidos el texto y las imágenes. Guardar solo nombre,
  presentación, precio y URL; mostrar las imágenes enlazadas, no copiadas.
- **Ánimus Lab**: prohíbe reproducir, copiar, distribuir, transformar o explotar contenidos sin autorización expresa y se reserva
  restringir, suspender o bloquear el acceso al sitio. Pedir autorización por escrito a `info@animuslb.com` antes de scrapear.
- **Cutis**: sin cláusula sobre acceso automatizado ni copia (texto leído completo). Mantener un ritmo bajo (pausas de 1,5 s o más).
- **Drogas La Rebaja (Copservir Ltda.)**: sin cláusula sobre acceso automatizado ni copia; se reserva el derecho de cambiar
  precios en cualquier momento.
- **Droguería La Economía (Eticos SAS)**: sin cláusula sobre acceso automatizado ni copia en los términos copiados del sitio;
  declara que sus precios difieren de los de las tiendas físicas, varían según la ciudad, y que puede modificarlos en cualquier
  momento.
- **Farmanorte**: su página de "Términos y condiciones" solo reproduce la ley de protección de datos personales (Ley 1581); no
  trae términos de uso del sitio. Sin cláusulas sobre acceso automatizado.
- **Botanikalia**: la página de "Términos y condiciones" no muestra contenido (solo el formulario de devoluciones; puede ser un
  fallo de su sitio). No hay términos legibles que revisar; se rige por su robots.txt y su `agents.md`. La ausencia de términos
  no equivale a una autorización.
- Este análisis no es asesoría legal.

### Verificación de datos mínimos

- Nombre, precio y URL: **visibles en las tiendas seleccionadas** (Cruz Verde y La Economía, solo en el navegador).
- Presentación estructurada: La Rebaja (`Presentacionunidadmedida`), Cutis (`Tipo de presentación`), Farmanorte (nombre y
  `Cantidad de unidades`). En Farmatodo, Ánimus Lab y Botanikalia hay que parsearla del nombre, la variante o la descripción, y
  muchas veces no está (hallazgo 11).
- Marca: La Rebaja, Cutis y Farmatodo la traen; Farmanorte solo "Generica"; en Shopify, `vendor` (consistente en Ánimus Lab; dos
  escrituras en Botanikalia).

### Pendientes antes de cerrar la tarjeta

1. Autorización escrita de Ánimus Lab; decidir si se solicita también a Cruz Verde o se excluye.
2. Cambiar el `UA` de `verificar_tiendas.py` por el User-Agent real del scraper, con contacto del proyecto.
3. Decisión del equipo sobre ADR-04. La parte de Farmanorte (solo HTML o excluir) quedó resuelta en el scraper: entra por HTML, con URL curada (ver "Scrapers de droguería").

## Scrapers de droguería (EP-04, 14-nov-2026)

Estado actualizado: estrategias incluidas en la rama de referencia `85f463a`; objetivos activos y curaduría real deben verificarse por base. Lo siguiente completa y corrige "Selección de tiendas de droguería y cuidado facial".

- **Qué tiendas y cómo**: Drogas La Rebaja por la API de VTEX (búsqueda por término, como los supermercados). Farmatodo - Droguería y Farmanorte por la página de producto, con **una URL curada por `scrape_target`** (`scrape_targets.target_url`), porque sus robots.txt prohíben la búsqueda. Una tienda sin `scrape_targets` activos no se consulta. La columna `target_url` la agrega `scripts/2026-11-droguerias-categorias-tiendas.sql`; si cuidado facial ya la usa, la reversión no debe borrarla.
- **El EAN sí llega de Farmatodo y Farmanorte** (corrige los hallazgos 1 y 7): Farmatodo lo trae en `barcode` del estado de la página (`barcodeList` son ids internos); Farmanorte, en `mpn` del JSON-LD (su `gtin` es un código interno). Verificado con una página de producto de cada tienda. Farmanorte sigue sin marca real: "Generica" se guarda como sin marca, para no crear una marca falsa. Para que haya comparación (HU-28), la URL debe ser del **mismo producto y el mismo tamaño** que ya trae La Rebaja: el procesador empareja por EAN y rechaza la oferta si el tamaño difiere.
- **Venta libre (`EXCLUIR_FORMULA`)**: constante de `scraper_droguerias.py`, actualmente True. La Rebaja excluye Producto RX=SI o categorías de fórmula médica; OTC=NO por sí solo no descarta un artículo. Farmatodo usa requirePrescription. Farmanorte no declara fórmula: su filtro es la lista curada de URLs, que no debe incluir medicamentos de fórmula.
- **Kits**: los listados marcados como kit en La Rebaja se descartan (comparten el EAN de la unidad con otro precio, hallazgo 2).
- **Precio de Farmatodo**: se usa `fullPrice`, no `offerPrice` (en la página probada era un precio "Exclusivo 1ra compra"). Si Valledupar no aparece entre las ciudades del producto, no se guarda y se avisa; no se cae al precio nacional (misma regla que los supermercados). **Sin confirmar**: que el código `VUP` sea Valledupar (la página lista a Valledupar con su teléfono, pero ninguna tabla de la tienda lo confirma).
- **Limitaciones aceptadas**: la página de Farmatodo trae solo los ids de las tiendas con stock, no su ciudad, así que un producto se guarda si hay stock en alguna tienda del país. El precio de Farmanorte es el predeterminado, sin ciudad; verificarlo contra la web con ubicación en Valledupar antes de mostrarlo como precio de Valledupar (DP-38).
- **robots.txt y ritmo**: antes de cada página se consulta el robots.txt de la tienda, con soporte de `*` y `$` (el analizador de Python no entiende `*` dentro de una ruta, y Farmatodo prohíbe `/producto/*/review`). Pausa por defecto de 5 s entre páginas; si el robots.txt declara `Crawl-delay`, se usa ese valor. Una página que cambió de estructura da error visible; un 404 queda como producto retirado. **Pendiente**: las pruebas se hicieron con robots.txt reconstruidos desde este archivo, no con los reales; el primer log real de cada tienda lo confirma.
- **Tamaño**: un lector de presentaciones (`comun/presentacion.py`) interpreta tabletas, ml y g desde el nombre. Si el nombre trae dos tamaños distintos, la oferta queda sin tamaño en vez de adivinar, y el procesador la rechaza ("no trae el contenido neto").
- **Procesador sin cambios**: no se crea una ingesta paralela por droguería: se reutiliza el procesador con EAN esperado, presentación efectiva, cierre/deduplicación y validaciones comunes. En estas fuentes se exige GTIN comparable; la ruta secundaria por SKU solo se habilita para tiendas autorizadas. El nombre de un maestro nuevo procede de la oferta que lo crea, tras comprobar conflictos; no se actualiza por llegar otra tienda.
- **Workflow**: `scrape-precios.yml` tiene matriz de supermercados/droguerias/facial, jobs independientes, fail-fast=false, timeout de 45 minutos y filtros separados. Disparo manual; sin cron ni push automático. Usa grupo scraper-precios y cancel-in-progress=false. El workflow anterior sigue en el árbol con otro grupo y cancel-in-progress=true: no lanzar ambos a la vez, porque no se excluyen entre sí. El botón manual depende de la rama predeterminada y la selección de rama de Actions.
- **Antes de encender las tiendas**: las tres siguen con `stores.active = false` y sus objetivos inactivos hasta cargar y probar las URLs (`SOLO_TIENDAS=farmatodo`, luego `farmanorte`, con un solo objetivo activo). Una corrida sin objetivos activos termina en 0 sin haber probado nada.

## Búsqueda de productos (HU-06a, HU-06b)

- **Todos los filtros en SQL (SCRUM-140)**: reemplaza la decisión anterior de filtrar el nombre en Java. Se filtraba en Java porque un `LIKE` simple no encontraba "café" al escribir "cafe". Con la búsqueda paginada eso ya no sirve: filtrar en Java después de paginar deja páginas cortas o vacías, y paginar en memoria carga todo el catálogo. La regla sin tildes ni mayúsculas se conserva con `lower(f_unaccent(...))` a los dos lados de la comparación (`scripts/2026-11-scrum140-busqueda-sin-tildes.sql`): primero se quitan las tildes y después se pasa a minúsculas, para no depender del `LC_CTYPE` de la base. Java solo parte el texto en palabras y escapa `%`, `_` y `\` (`SearchTerms`); cada palabra es un `LIKE` y deben estar todas, en cualquier orden. Así el criterio de HU-06a "el filtro combinado se aplica en una sola consulta" se cumple literalmente.
- **`f_unaccent`**: envoltorio `IMMUTABLE` de `extensions.unaccent` con el diccionario calificado, para no depender del `search_path`. La extensión va en el esquema `extensions`, como las demás de Supabase. Sigue la regla de funciones nuevas de "Seguridad de la base de datos" (SCRUM-304): ni `PUBLIC`, ni `anon`, ni `authenticated` pueden ejecutarla. Antes de correr el script se verifica en qué esquema está la extensión `unaccent` en cada base. La reversión borra la función, no la extensión.
- **Un método por filtro**: `ProductSpecifications` tiene un método por filtro (activo, nombre, categoría, precio vigente) y `ProductRepository` extiende `JpaSpecificationExecutor`. El filtro por marca (también parte de RF06) se retoma en el Sprint 4 como HU-35 (SCRUM-305), bajo SCRUM-5, después de que el flujo de pagos funcione de punta a punta.
- **Precio vigente**: el periodo más reciente del par producto-tienda, es decir, la fila de `prices` sin otra más nueva del mismo par (por `registration_date` y, si empatan, por `id`). `last_seen_at` no cambia cuál es. El rango se evalúa sobre esa fila y nunca sobre una histórica: si un producto costó 3.000 y hoy cuesta 5.000, no entra en un máximo de 4.000. Límites inclusivos; se puede enviar solo el mínimo, solo el máximo o ninguno.
- **Tiendas del filtro**: solo cuentan las tiendas activas. Sin tiendas seleccionadas, el producto entra si alguna tienda activa tiene su precio vigente en el rango; con una o varias, basta con que una de ellas lo tenga, y nunca cuenta el precio de otra tienda. Sin tienda ni rango no se exige ningún precio: la búsqueda de HU-06a sigue mostrando productos sin precio.
- **Paginación**: `page` desde 0, `size` de 20 por defecto y 50 como máximo (el mismo límite que las opiniones). El orden era por nombre y luego por id; desde HT-13 es por mayor ahorro (ver la sección siguiente). El cliente no lo elige, y los empates van por nombre e id para que las páginas no repitan ni salten productos. La respuesta es un `PagedModel`: `{ content: [...], page: { size, number, totalElements, totalPages } }`. Es un cambio de contrato: antes era un arreglo, y por eso se adaptó `useSavingsDeal` (la tarjeta de ahorro del inicio).
- **Códigos de la búsqueda**: `200` también sin resultados; `400` por un precio negativo o no numérico, o por un mínimo mayor que el máximo; `404` por una categoría inexistente o por una tienda inexistente o inactiva, con los ids en el mensaje. No se usa `409` para una tienda inactiva: un `GET` no modifica nada, así que no hay conflicto de estado, y para un visitante una tienda inactiva no existe. Se eligió `404` y no `400` porque el mismo endpoint ya respondía `404` por una categoría inexistente.
- **Riesgo aceptado**: si una tienda deja de vender un producto activo, su último periodo sigue siendo el vigente, así que el filtro por esa tienda lo sigue mostrando con un precio viejo. Una ventana de vigencia según `last_seen_at` queda en HU-36 (SCRUM-306, backlog, DP-38): tendría que aplicarse igual en la búsqueda y en el comparador, y se retoma cuando el scraper corra a diario de forma automática, porque hoy ninguna ventana se puede verificar y vencería casi todos los precios.
- **Frontend**: los filtros de tienda, precio y página van en la URL de `/buscar` (`tienda` repetible, `min`, `max`, `pagina` desde 1), que es su única fuente de verdad. El texto y la categoría siguen como en HU-06a. Escribir en el buscador solo quita `q` y `pagina` de la URL (antes la vaciaba entera, incluido `sector`). Cualquier cambio de filtro vuelve a la primera página. El rango se aplica con el botón "Aplicar precio" o con Enter, no con cada tecla; el borrador valida lo mismo que el backend antes de pedir. Un `404` por una tienda que ya no está disponible ofrece quitar el filtro de tienda.

## Búsqueda por sección y orden por mayor ahorro (HT-13, SCRUM-309)

- **Contrato**: `GET /api/products/search` suma `sectionId` (id de la categoría raíz; no se reutiliza `sector`, que en el frontend es un texto de navegación como `supermercados`) y `sort`. `sectionId` deja los productos activos con precio en al menos una tienda activa de ese dominio (`store.category`), aunque estén clasificados en otro (DP-26), y se combina con nombre, categoría, tiendas, rango y paginación: deben cumplirse todos. `sort` acepta `SAVINGS` (por defecto) y `NAME`; queda reservado y extensible (un orden nuevo es un valor más del enum `ProductSort`), pero la interfaz no lo expone (DP-36).
- **Ahorro (DP-39)**: `(precio vigente máximo − mínimo) / máximo` entre **todas** las tiendas activas, en porcentaje (se eligió porcentual y no absoluto para no favorecer a los productos caros). Solo cuentan precios vigentes de tiendas activas (DP-34, DP-37). Con un solo precio o sin precio el ahorro es 0 y el producto va al final, sin desaparecer. Con tiendas filtradas el ahorro no cambia: se sigue calculando con todas las activas, porque el ahorro describe el producto, no el filtro. Desempate por nombre y luego por id.
- **Vista `product_savings`** (`scripts/2026-11-ht13-ahorro.sql`): calcula el ahorro en SQL y la búsqueda ordena por ella con un `LEFT JOIN` desde `Product` (`@OneToOne` de solo lectura con `ProductSavings`), así que los filtros y la paginación siguen resueltos en SQL. Se revoca a `PUBLIC`, `anon` y `authenticated` (regla de SCRUM-304). **Orden de despliegue obligatorio**: con `ddl-auto=validate`, un backend con HT-13 no arranca sin la vista, así que el script se corre primero en la base de pruebas (la consulta del final debe dar `false, false`), se avisa al equipo y luego se corre en la compartida, antes de que alguien levante `develop`.
- **Sin filtros: `GET /api/products/best-savings`** (contrato propio): los `limit` productos de mayor ahorro de cada subcategoría (por defecto 5, de 1 a 20), agrupados por sección y subcategoría (`[{ sectionId, sectionName, categoryId, categoryName, products: [{ id, name, minPrice, maxPrice, savingsPct, storeCount }] }]`), con `sectionId` opcional. Entra todo producto activo con precio en al menos una tienda activa: los de ahorro 0 completan la subcategoría al final. Es SQL nativo porque necesita `ROW_NUMBER` por subcategoría. N = 5 es provisional.
- **Códigos (DP-35)**: `200` también vacío; `400` por `sort` desconocido o `limit` fuera de rango o no numérico (`InvalidSearchFilterException` y el manejador de parámetros mal formados); `404` por una sección inexistente o que no es raíz (`CategoryNotFoundException`). Antes de cerrar HT-13 estos dos casos respondían `500`, porque lanzaban `ResponseStatusException` e `IllegalArgumentException`, que el `GlobalExceptionHandler` no maneja.
- **Frontend**: la capa `api/products.js` manda `sort=SAVINGS` en la búsqueda con filtros y expone `findBestSavings` para la vista sin filtros. La traducción del sector de la URL al `sectionId` y su envío desde `Search.jsx` se hicieron en HT-15 (ver "Landing y páginas de sección"); conectar `findBestSavings` (la vista agrupada por subcategoría, sin filtros) a la pantalla sigue pendiente.
- **Riesgo aceptado (DP-38)**: un precio viejo de una tienda que dejó de vender el producto puede inflar el ahorro. Se mitiga mostrando la fecha de la última confirmación; se resuelve con HU-36.

## Landing y páginas de sección (HT-15)

- **Regla de la landing**: toda cifra, tienda, producto, imagen o precio que muestra sale del backend. Si un dato falla o no existe, su bloque se oculta; nunca se rellena con un valor de respaldo. Los textos fijos solo describen funciones que existen hoy, y los que dependen de la cobertura (lista de tiendas, número de tiendas, respuesta sobre qué se compara hoy) se arman con los datos, para que no queden desactualizados cuando entren nuevas tiendas o secciones.
- **`GET /api/landing/stats`** (público; `SecurityConfig` lo permite solo para GET en `/api/landing/**`): `{ totalProducts, totalStores, lastUpdate, sections: [{ id, name, productCount, storeCount }] }`. Una sección es una categoría raíz. `productCount` cuenta los productos activos con precio en al menos una tienda activa de ese dominio (DP-26), una vez cada uno; `totalProducts` cuenta cada producto una sola vez aunque tenga precio en dos dominios; `lastUpdate` es la última vez que una tienda activa confirmó un precio. Es una sola petición y una sola transacción de solo lectura, o sea una conexión del pool (RP-04). Sin caché en el backend por ahora (cuatro consultas por visita): si el catálogo crece, cachear unos minutos.
- **"Próximamente" se calcula, no se escribe**: una sección con `productCount` 0, o cuya categoría raíz no existe en la base, se muestra como "Próximamente", sin enlace a una búsqueda vacía. Si falla `GET /api/landing/stats`, el bloque de secciones desaparece: mostrar las cuatro como "Próximamente" sería falso.
- **Slug de la sección**: las URLs usan `/secciones/:slug` y `?sector=<slug>`. `config/section.js` mantiene textos/slugs de presentación y las pestañas deben ser coherentes con ellos. La página de sección resuelve estadísticas por nombre normalizado; Search usa rootName y el catálogo de raíces activas. Los IDs numéricos vienen del backend. Renombrar una raíz exige revisar esas correspondencias; no se añade una columna slug en esta entrega.
- **Un solo componente de presentación**: `SectionPresentation` sirve a las cuatro secciones (`/secciones/:slug`) y cubre los estados cargando, error con reintento, sección inexistente, "Próximamente" y disponible. Muestra los avisos de negocio de cada sección: precios informativos (RN-04) en todas, orientativo y sin sustituir a un profesional de la salud (RN-17) en droguería y cuidado facial, y sin verificar compatibilidad (DP-20) en repuestos. La fecha de "última actualización" es la global, no la de cada sección: hay que devolverla por sección cuando entren las otras.
- **Contrato con el buscador**: «Buscar y comparar» abre `/buscar?sector=<slug>`. `useSearchCatalog` obtiene raíces activas, resuelve el ID y carga categorías/tiendas de esa raíz antes de buscar. Search envía sectionId, conserva la sección al elegir «Todas», sincroniza las pestañas con la URL y limpia categoría/tiendas/página al cambiarla. Un sector desconocido muestra indisponibilidad y un fallo del catálogo ofrece reintento, sin consulta global de respaldo. Las estadísticas de la portada no sustituyen este catálogo. Repuestos mantiene su guarda «Pronto».
- **`useSections` / `useSectionBySlug`**: comparten estadísticas de landing/páginas de sección mediante caché de módulo de 5 minutos, sin cachear fallos. El buscador usa `useSearchCatalog` y endpoints de catálogo; no comparte automáticamente esa petición de estadísticas.
- **Cliente HTTP compartido**: `api/http.js` centraliza URL de origen y manejo de errores para los consumidores que lo usan. `api/products.js` y `api/categories.js` todavía contienen origen localhost:8080; unificarlo con VITE_API_URL sigue pendiente y no se presenta como resuelto por la corrección de Search.
- **Contenido de la landing**: hero con las fotos y los precios de los productos de mayor ahorro (la búsqueda sin filtros, ya ordenada por ahorro, de la que se descartan los de una sola tienda o con el mismo precio en todas; se piden 12 candidatos y se muestran 4); comparador de ejemplo con el precio y la fecha de confirmación de cada tienda, y su titular ("$X de diferencia.") calculado con los precios del producto elegido; cifras reales y tiendas activas; secciones; beneficios; cómo funciona; quiénes somos; preguntas frecuentes y llamada a la acción. **Se quitó** lo que no existe o no tiene respaldo: los testimonios (no hay opiniones reales que mostrar en la portada), el paso "Consulta a la IA" (Fase 4), las categorías Belleza y Tecnología y las cifras sin respaldo ("miles de productos", "al instante"). Del boceto de Stitch no se incluyeron la calculadora de ahorro mensual ni la comparación contra otros comparadores (afirmaciones sin respaldo), ni "kilo y litro" ni WCAG AAA (no está construido ni certificado).
- **Logos de tienda**: `StoreLogo`. Los archivos oficiales (kit de prensa de cada marca) van en `public/assets/stores/` (`d1`, `exito`, `jumbo` y `olimpica`, en `.png`) y se reconocen por un fragmento del nombre de la tienda, sin tildes ni mayúsculas. Si falta el archivo o no carga, se muestran las iniciales. Solo se muestran las tiendas activas de la base, con el aviso "marcas registradas de sus titulares; PreciosCo no está afiliada". Cada tienda nueva exige su archivo y una línea en la lista `LOGOS`; una columna `logo_url` en `stores` queda como mejora si el número de tiendas crece.
- **Imágenes de producto**: se enlazan desde la tienda (`referrerPolicy="no-referrer"`), no se copian ni se guardan, igual que ya pedía la nota de los términos de las tiendas; si no cargan se muestra un marcador.
- **Modo oscuro de la portada**: las variables `--h-*` de `Home.module.css` toman los tokens del tema (`--bg-surface`, `--bg-main`, `--text-primary`, `--color-primary`...), así que siguen el mismo mecanismo del resto de la aplicación. El texto sobre el ámbar de la marca es siempre oscuro (`#0f172a`), como en el header, y la banda de cifras y tiendas es oscura en los dos modos. Antes la portada mezclaba tokens con fondos propios y en modo oscuro quedaba texto claro sobre fondo claro.
- **Header de una sola fila**: logo, menú de secciones en píldora (centro) y acciones (derecha). El menú sale de `SECTIONS` y enlaza a `/secciones/:slug`; queda marcado en esa ruta o en `/buscar?sector=`. El buscador de texto del header se reemplazó por un atajo a `/buscar` (oculto en `/`, `/buscar`, login y registro), porque con el menú en la misma fila no cabe sin abrir una segunda. Por debajo de 960 px el menú pasa al botón hamburguesa. `Header.module.css` se reescribió: tenía bloques completos repetidos que se pisaban entre sí (por ejemplo, el botón "Ingresa" del panel perdía su estilo).
- **Riesgos y límites aceptados**: el conteo de tiendas incluye las activas aunque su scraper esté en pausa (Éxito desde el 11-nov), y su precio muestra una fecha vieja, no una vigencia (DP-38, HU-36). Los enlaces del pie (Quiénes somos, Cómo funciona, Contáctanos) y "Contáctanos" sin destino no son de HT-15.
- **Pendiente de reflejar en el documento de análisis**: reconciliado en esta actualización: RF50/HU-03 describen los bloques actuales y DP-29 deja de exigir ocho secciones de contenido con testimonios. RF09 distingue el orden de precios del comparador del orden por ahorro de los productos buscados. Se conservan los avisos RN-04, RN-17 y DP-20.

## Fase 2 — Calificaciones y comentarios

- **Modelo**: se adoptó el modelo del bot original (no el plan inicial de la Fase 2). Cada calificación lleva **producto y tienda**, ambos obligatorios; es única por (producto, tienda, usuario). Ya no existe la calificación directa de una tienda: su promedio se deriva de las calificaciones de sus productos.
- **Calificar y comentar son una sola acción**: se registran juntos, en una sola transacción (se guardan las dos filas o ninguna), pero siguen siendo dos entidades y dos tablas independientes (no una tabla unificada), para poder consultar el promedio sin cargar el texto del comentario.
- **Relación entre las dos tablas**: `comments` apunta a `ratings` con una FK única y obligatoria (`comments.rating_id`, relación 1 a 1). El usuario, el producto y la tienda del comentario se leen de la calificación, sin duplicarlos: así la base de datos impide un comentario de un par distinto al de su calificación y un segundo comentario por calificación. Se descartó repetir usuario, producto y tienda en `comments`, y se corrigió el checklist de la Fase 2 en consecuencia. Restricciones con nombre: FK `fk_ratings_users`, `fk_ratings_products`, `fk_ratings_stores`, `fk_comments_ratings`, `fk_comment_reports_comments` y `fk_comment_reports_users`; únicas `ratings_product_id_store_id_user_id_key`, `comments_rating_id_key` y `comment_reports_comment_id_user_id_key`; check `chk_ratings_score`.
- **Cuándo se puede opinar**: el producto y la tienda deben existir y estar activos, y debe existir al menos un precio registrado de ese producto en esa tienda.
- **Comentario**: mínimo 10 caracteres (confirmado), máximo 500. Uno solo por producto y tienda.
- **Quién puede opinar**: solo el rol USER califica, comenta y reporta (ADMIN y SUPPLIER no). La regla vive en el service y responde con un mensaje claro; `SecurityConfig` solo exige sesión en esas rutas.
- **Edición**: la calificación y el comentario se pueden editar durante **1 día** (24 horas) desde el registro — valor corregido de la propuesta inicial de 5 minutos. Pasado ese plazo, quedan definitivos. Solo se puede editar mientras el comentario siga en estado **PUBLICADO**; uno reportado, oculto o eliminado no se reescribe. El plazo es configurable con la propiedad `app.reviews.edit-window-hours` (24 por defecto).
- **Eliminación de comentario**: borrado lógico (estado `ELIMINADO`), no físico. Después de eliminar, ese usuario no puede volver a comentar ese producto en esa tienda (la fila del comentario se conserva con ese estado). Un comentario `OCULTO` por el administrador **no** puede ser eliminado por el autor (permanece `OCULTO`); el intento responde `409`.
- **Promedio y moderación**: el estado del comentario (incluido `OCULTO`) **no** retira la calificación del promedio del producto ni del establecimiento. Ocultar un comentario es una decisión sobre el texto, no sobre el puntaje. Retirar una calificación exigiría un criterio de fraude propio de la calificación (cuentas duplicadas, patrones repetidos, etc.), que todavía no está definido — queda como decisión pendiente.
- **Reportes**: el campo se llama **razón**, no "tipo". Catálogo provisional: `SPAM`, `LENGUAJE_OFENSIVO`, `INFORMACION_FALSA`, `OTRO` (con detalle obligatorio y máximo 200 caracteres si la razón es `OTRO`). Cada reporte tiene su propio estado: `PENDIENTE` o `RESUELTO`, y se guarda en su propia tabla (`comment_reports`) con usuario, comentario, razón, detalle y estado; en la Fase 2 todos quedan `PENDIENTE`, porque la moderación es manual.
- **Reglas del reporte**: un reporte por usuario y comentario (repetirlo no crea otro); no se puede reportar el propio comentario; un reporte nuevo pasa el comentario a `REPORTADO` (o lo reabre si estaba `CONSERVADO`) y sigue visible; un comentario `OCULTO` o `ELIMINADO` no se puede reportar.
- **Visibilidad y listado**: se listan los comentarios `PUBLICADO`, `REPORTADO` y `CONSERVADO`; `OCULTO` y `ELIMINADO` no se muestran. El listado es paginado (10 por página por defecto, máximo 50), de más reciente a más antiguo, y el cliente no elige el orden. La respuesta pública no incluye el estado ni el correo del autor.
- **Rutas de la API**: todo bajo `/api/reviews`: `POST` (calificar y comentar), `PUT /{id}` (editar), `DELETE /{id}/comment` (eliminar el comentario), `GET` (lista), `GET /summary` (promedio y cantidad de un producto, una tienda o un producto en una tienda), `GET /me` (mi opinión) y `POST /{id}/report`. El id de una opinión es el de su calificación. La lista y el resumen son públicos; el resto exige sesión. Los controllers obtienen el usuario autenticado con `@CurrentUser` (Ayner); el usuario temporal fue transitorio y no se mergea a main.
- **Códigos de estado** _(por confirmar con el equipo; las pruebas automáticas y la colección de Postman se escriben contra esto)_: `201` al crear; `400` por datos inválidos, por razón `OTRO` sin detalle o por reportar el propio comentario; `401` sin sesión; `403` por rol no permitido o por no ser el autor; `404` por producto, tienda u opinión inexistentes, por un par sin precio o por reportar un comentario oculto o eliminado; `409` por opinión repetida (también después de eliminar el comentario), producto o tienda inactivos, plazo de edición vencido, comentario no editable o eliminación de un comentario `OCULTO`; `204` al eliminar el comentario, al reportar (repetir cualquiera de los dos también responde `204`) y en `GET /me` cuando aún no hay opinión.
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
- **Scroll al navegar**: `MainLayout` lleva la ventana arriba cada vez que cambia la ruta. No se usa `<ScrollRestoration />` porque solo funciona con los routers de datos (`createBrowserRouter`) y la aplicación usa `BrowserRouter`. Filtrar o paginar en `/buscar` no salta al inicio, y Atrás y Adelante conservan la posición. Antes, entrar al comparador desde la mitad de los resultados abría la página a la misma altura, en la sección de opiniones.
- Los diálogos usan el elemento `<dialog>` nativo (foco y cierre con Esc del navegador) a través de un componente `Dialog` reutilizable.
- El código nuevo sigue las convenciones ya establecidas del frontend: `react-router`, CSS Modules, capa `api/`.
- **Regla de lint**: con `eslint-plugin-react-hooks` 7 no se llama a `setState` de forma síncrona dentro de un efecto; lo que se puede calcular a partir de otro estado se calcula al renderizar (se corrigió así `Search.jsx`). `PriceRangeFilter` sigue el mismo patrón: cuando la URL cambia, ajusta su borrador durante el render.

## Alcance y fases

- La moderación de comentarios y el bloqueo de cuentas del panel administrativo **no** pasan a la Fase 2: se quedan en fases posteriores (manual en la BD hasta entonces).
- **Comparador de repuestos**: el alcance inicial de Fase 3 incluía vehículos y motos. El acuerdo del 20-nov-2026 limita RF55 a automóviles y mantiene la comparación en backlog. La identidad es marca y referencia o código del fabricante (RN-32). La compatibilidad con un modelo de automóvil está fuera del alcance.
- **Comparador de cuidado facial**: se separa de RF52 como RF58, para que cada épica (EP-04 droguerías, EP-05 cuidado facial) tenga su propio requisito.
- **Búsqueda por referencia**: HU-33 amplía RF05 (los repuestos también se buscan por su referencia o código del fabricante); no se crea un requisito nuevo.
- **Inicio de sesión con Google**: se agrega como RF56 en la épica Cuenta de usuario (Sprint 2), junto con RF03 y RF04. El backend valida la cuenta de Google y emite su propio JWT; solo crea o abre cuentas con rol USER.
- **Enlace a la tienda**: se documenta como RF57 (HU-27) una funcionalidad implementada el 17-oct-2026 sin requisito asociado. Junto a cada precio se muestra un enlace a la página del producto en la tienda (`prices.product_url`, que guarda el scraper); un precio sin URL se muestra sin enlace.

## Documentación (Ingeniería de Software I)

- Los requisitos funcionales y no funcionales se renumeran de forma consecutiva, sin saltos bruscos — hasta RF48. A partir de **RF49**, los IDs ya no se renumeran: lo nuevo se agrega al final del módulo correspondiente, aunque quede fuera de orden en la página.
- RNF11 (pruebas) y RNF12 (manual y documentación) salen de los requisitos no funcionales porque son entregables del proyecto, no cualidades del sistema.
- RNF06 se elimina por duplicar RF10; el contenido mínimo de un precio (valor, fecha, fuente) queda como regla de negocio (RN-01).
- Se mantiene la marca en los RF (ahora integrada al modelo, DP-11) y los requisitos RF04 y RF40 sin una regla de negocio directa asociada.
- El documento de análisis deja para más adelante las fichas de requisito, el caso de uso, los casos de prueba y la sección de fuentes y método (sin tiempo para entrevistas por ahora).
- **Documento de análisis en el repositorio**: Alcance, Análisis y especificación y DECISIONS de docs/ son las referencias principales consolidadas al 18-nov-2026. Los cambios de comportamiento actualizan requisitos, aceptación y decisiones en la misma rama. No se depende de una copia en una conversación o artifact externo para reconstruir los acuerdos vigentes; los informes de pruebas auxiliares son evidencia, no una especificación paralela.
- Pendiente de reflejar en el documento de análisis lo decidido en la Fase 2: la regla RN-30 debe decir "al registrar y al editar" (eliminar el comentario deja una calificación sin texto visible), los códigos HTTP en HU-10, HU-16 y RNF08, y las decisiones nuevas como decisiones pendientes con su valor provisional.

## Autores en el historial de Git

- `luminuos09` → Ramos
- `Andresbtm` → Darwin
- `kstrodeveloper` y `kstroDev09` → Ayner

## Intención de este archivo

Las decisiones de equipo se toman y registran a nivel de equipo, no individualmente — una distinción importante en evaluaciones o revisiones de código. Este archivo deja constancia de esas decisiones para que una exclusión deliberada no se confunda con un vacío frente al MVP original.

## Ajustes del scraper y procesador (16-nov-2026)

Implementación solicitada a partir de los dos adjuntos de revisión, sobre `feature/update-scraper` (`eed7d5c`). Detalles operativos y orden de despliegue en `scrapers/README.md`.

- **Contenido publicado primero**: Olímpica y Jumbo leen sus especificaciones de contenido antes del nombre. En Jumbo, el multiplicador de `*_un` está en kg/l incluso cuando la etiqueta dice gr/ml. Datos estructurados inválidos no se sustituyen por una conjetura del nombre. Sin ficha, peso seguido de `X N Unds` se considera ambiguo salvo pack explícito; `sixpack` significa seis.
- **Identidad esperada de un objetivo de página**: `expected_ean` se comprueba en `OfferScreening` con `Gtin`, no en Python. Solo con identidad verificada y rango exacto se puede completar una presentación ausente; no se sobrescribe una presentación publicada contradictoria. El tamaño efectivo viaja en `Accepted`, sin modificar el staging crudo.
- **Correcciones manuales**: `ean_size_overrides` guarda GTIN-14, contenido, unidad y justificación. Se conserva el bloqueo de conflictos no resueltos y la prohibición de modificar productos existentes. Un override no sirve para reutilización real del EAN en presentaciones diferentes.
- **Corrida explícita en lugar de una ventana de una hora**: `scrape_runs` y `scraped_offers.run_id` separan ejecuciones y excluyen lotes todavía abiertos. Por tienda/identidad/corrida se registra el menor precio válido (desempate por ID); el resto queda PROCESSED con motivo. Filas anteriores sin corrida siguen individuales hasta que se identifiquen sus lotes reales. Bloqueo transaccional global antes de tomar filas para evitar interbloqueos entre workers; mantiene el pool de dos conexiones y los eventos posteriores al commit.
- **Peso variable aprobado para esta entrega**: `store_product_links` relaciona una tienda y SKU con un genérico curado activo, sin EAN/marca/referencia, de 1000 G y misma categoría del objetivo. Los códigos internos o de medida variable no son identidad global. El SKU y peso de venta publicado permiten derivar COP/kg, conservando el precio original. Sin enlace o peso válido se registra un motivo de rechazo. Los nombres del genérico deben indicar “precio por kg”; su unidad es 1 kg. No implica aprobar equivalencias automáticas por ingrediente activo en droguería (ADR-04 sigue pendiente).
- **Corrección de los hallazgos 1 y 7**: la implementación de páginas ya obtiene `barcode` del estado de Farmatodo y `mpn` del JSON-LD de Farmanorte; el campo `gtin` de esa fuente no se promueve a EAN cuando están presentes. Farmanorte sigue por HTML, con marca nula si solo publica “Generica”. No se consultan sus búsquedas ni su API prohibida. El filtro de La Rebaja excluye kits y productos RX, no todo producto con OTC=NO.
- **Actions**: `scrape-precios.yml` ejecuta los tres dominios de forma independiente, filtros separados, `fail-fast: false` y sin cancelar una corrida por iniciar otra. No se habilita programación ni push automático. El botón depende de que el archivo exista en la rama predeterminada.
- **Esquema**: `scripts/2026-11-scraper-procesador.sql` y su reversión, RLS y revocaciones para tablas nuevas. No se ejecuta ninguna limpieza histórica ni se activan targets a partir de IDs contenidos en conversaciones previas.

## Identidad secundaria de Bella Piel (17-nov-2026)

Se implementan las soluciones 2 y 3 del PDF aportado por el usuario, sobre el commit local `408288c`. El documento muestra ofertas con códigos de cuatro dígitos en el campo EAN; su explicación de los datos de VTEX es contexto aportado, no una verificación nueva contra las tiendas.

- **Ampliación controlada de ADR-01**: solo para tiendas autorizadas por configuración (por defecto `bellapiel`), una oferta legada con EAN ausente o numérico de 1–7 dígitos, o una fila nueva marcada has_ean=false, puede vincularse a un producto ya existente. No se transforma un código local en GTIN rellenándolo con ceros. Un GTIN comercial publicado sigue la ruta exacta; un SKU marcado has_ean=false conserva su ruta local aunque tenga forma de GTIN. El legado mantiene la evaluación anterior de checksum, placeholders y medida variable.
- **Solución 3 primero**: `store_sku_ean_map` guarda tienda, código local y GTIN-14 del catálogo, con fecha y justificación. En legado (has_ean=NULL), la clave usa el código corto publicado en EAN y, si falta, el SKU real. En filas nuevas has_ean=false, la clave es el SKU real; el código publicado anterior queda en CSV para revisar mapas cuya clave era distinta. Nunca se infiere desde URL ni nombre. El procesador verifica que el producto siga activo, con marca, tamaño, categoría y nombre compatibles. Una equivalencia incompatible requiere revisión, sin reemplazo automático silencioso.
- **Solución 2 como respaldo**: la consulta exige marca exacta normalizada como el índice de marcas, contenido y unidad exactos, categoría idéntica y producto activo con GTIN comparable. Luego se compara el nombre mediante Levenshtein sobre tokens normalizados. Se preservan números, SPF/concentración y variantes críticas como día/noche, gel/fluido o refill. Umbral inicial 0,90 y margen respecto al segundo candidato 0,08, configurables. Se rechazan empates, demasiados candidatos y datos incompletos; nunca se crea un producto desde esta ruta.
- **Auditoría y transacción**: se conserva el identificador que escribió el scraper: en filas nuevas sin EAN comercial es el SKU con has_ean=false, y el código publicado anterior queda en CSV. No se reescribe por el EAN canónico al procesar. La identidad elegida viaja en `Accepted` y el precio se registra contra el producto existente. El mapeo se guarda dentro de la transacción de ingesta, después del screening y se revierte si falla el registro. Screening y comprobación de conflictos no escriben equivalencias.
- **Persistencia no significa certeza absoluta**: la similitud produce una inferencia, susceptible de error. Por eso una equivalencia aprendida vuelve a comprobarse y puede corregirse por SQL. No se implementa el enriquecimiento del scraper de la solución 1.
- **Despliegue**: aplicar `2026-11-bellapiel-identidad-secundaria.sql` después de la migración anterior y antes de arrancar backend. Nueva tabla con RLS y revocaciones; reversión separada. No se ejecuta contra la base compartida desde esta tarea.


## Revisión operativa consolidada (18-nov-2026)

### Estados del staging y responsabilidades

| Estado | Quién lo asigna | Consecuencia |
| --- | --- | --- |
| PENDING | Scraper al capturar una oferta evaluable; operador al autorizar un reintento concreto | Java solo selecciona este estado, con corrida cerrada o legado sin run_id. |
| PROCESSED | Procesador | Precio registrado/refrescado o duplicado válido; `reason` identifica duplicados. |
| UNCLASSIFIED | Procesador | Rechazo por identidad, presentación, objetivo, mapa, conflicto entre ofertas o error técnico marcado después del rollback. |
| CONFLICT | Scraper facial | Revisión explícita de contradicciones internas o identidad local no utilizable; no entra a la ingesta y no lo elimina la retención automática. |

`NEW_PRICE`, `SAME_PRICE`, `SKIPPED` y `FAILED` son resultados de llamadas del resumen, no estados adicionales en la tabla. Una ingesta puede marcar varios duplicados/conflictos; sus conteos no equivalen a un GROUP BY final de todas las filas modificadas. BUILD SUCCESS no certifica que todos los datos hayan sido aceptados.

### Esquema mínimo elegido para SKU y conflicto

Se agrega únicamente `scraped_offers.has_ean BOOLEAN`, nullable y sin default, y CONFLICT al CHECK/enum existentes. Se conserva `ean VARCHAR(20)` y se reutiliza `reason`; no se agregan columnas de fuente de identidad, código original o JSON de conflicto.

- `has_ean=true`: en facial, ean contiene un EAN comercial publicado, comprobado por formato, checksum y clasificación. Java vuelve a evaluar su comparabilidad.
- `has_ean=false`: el SKU real se conserva en store_sku y se copia a ean; aunque tenga longitud/checksum de GTIN, no se trata como identidad comercial.
- `has_ean=NULL`: legado y estrategias que no informan el booleano mantienen la interpretación anterior. No se rellena todo como false ni se cambia el EAN canónico de products.

El código original publicado se entrega en `local_code` del CSV, separado del SKU real. Las equivalencias antiguas no se borran. Si un mapa usa un código corto diferente del SKU nuevo, se requiere resolución secundaria suficiente o una nueva asociación curada; no se copia ni sobrescribe a ciegas.

Si falta SKU o excede los 20 caracteres de ean, no se amplía ni se trunca la columna: se conserva el SKU utilizable en store_sku/CSV, ean queda nulo y se asigna CONFLICT con reason. Es una excepción para revisión; el caso habitual de BellaPiel usa SKU corto en ambos campos.

### Conflictos internos y archivos del scraper facial

El lector contrasta nombre, título, nombres de SKU, especificaciones, URL y medidas etiquetadas de descripción. Un título y una URL pueden derivar del mismo texto desactualizado; no son prueba independiente. Si hay distintas parejas contenido/unidad, no votan por mayoría: tamaño y unidad quedan nulos, el scraper inserta CONFLICT y reason y exporta la evidencia por fuente. Los packs detectados conservan la exclusión de esta integración.

- `sku_offers.csv`: inventario de ofertas sin EAN comercial publicado; algunas ya tienen equivalencia y no requieren intervención.
- `conflictos_tamanos.csv`: una fila por fuente/medida de una oferta contradictoria.
- Se conservan descartados, sin tamaño y diagnóstico EAN. Los dos CSV nuevos se añaden después de confirmar el lote; no representan filas de una inserción revertida.
- Se generan con cabecera aunque estén vacíos. Las rutas se configuran con SKU_OFFERS_PATH y CONFLICTOS_TAMANOS_PATH; por defecto son relativas al directorio de ejecución. Ambos workflows publican los nuevos CSV con retención de artefactos de 7 días.
- Claves comunes de revisión: corrida, tienda, target, SKU, código local, nombre, marca, presentación, precio, URL, estado y motivo; el de conflicto añade fuente, texto y medidas detectadas.

El procesador no completa CONFLICT mediante target exacto u override. Cambiar mapas/targets no reabre filas automáticamente. Conservar la evidencia, corregir el caso y solo después autorizar IDs concretos o capturar una observación nueva; no cambiar masivamente CONFLICT/UNCLASSIFIED a PENDING.

### Presentación efectiva, identidad esperada y evidencia

`expected_ean` es un código, no un campo booleano de «EAN requerido». Farmatodo/Farmanorte necesitan objetivos de página con el EAN real esperado: el scraper debe leerlo de la fuente y Java comprobarlo. El EAN esperado no rellena uno ausente. Un tamaño exacto del target solo completa datos faltantes bajo identidad verificada y datos parciales compatibles.

`ean_size_overrides` usa GTIN-14, contenido, unidad y reason documentado. No modifica el dato crudo ni el maestro existente y no es una equivalencia global entre gramos y mililitros. La ruta secundaria exige sus propios atributos; no se usa un override para inventarle la presentación ausente.

La auditoría de identidad secundaria distingue falta de candidatos por atributos, falta de compatibles, puntaje insuficiente, ambigüedad y mapa incompatible. Los parámetros iniciales siguen siendo mínimo 0,90, margen 0,08 y máximo 100 candidatos. La inferencia no acredita por sí sola el empaque físico. No se bajan umbrales para hacer pasar un caso ni se promueven nombres de imagen o referencias genéricas a EAN comercial.

### Genéricos por peso variable

`store_product_links` y `store_sku_ean_map` tienen fines distintos:

| Tabla | Identidad destino | Carga |
| --- | --- | --- |
| store_product_links | product_id de un genérico de 1000 G sin EAN, marca ni referencia | Manual, con justificación de equivalencia física. |
| store_sku_ean_map | EAN/GTIN-14 de un producto comercial existente | Curaduría o aprendizaje secundario conservador, revalidado en cada oferta. |
| ean_size_overrides | Presentación efectiva de un GTIN comercial | Corrección manual justificada; no sanea el maestro. |
| scrape_runs | UUID y fechas de inicio/cierre de una ejecución | Scraper; no equivale a estado de éxito por tienda. |

La ruta variable exige código interno/de medida variable reconocido por Gtin, enlace exacto tienda/SKU, genérico activo de 1000 G de la misma categoría hoja y peso publicado positivo. Un enlace solo no habilita cualquier EAN ausente/inválido. `expected_ean` queda nulo en estos objetivos.

El peso procede de measurementUnit=kg/unitMultiplier del SKU; Olímpica admite contenido estructurado en G/1000. No se estima desde nombre o precio. El backend divide el total publicado por sale_weight_kg y redondea a 2 decimales HALF_UP; conserva el total en staging y publica COP/kg. Si la fuente ya publica precio por kg, esa división sería incorrecta: la base de precio debe comprobarse antes de enlazar.

Carnes son el objetivo inicial; frutas, verduras, tubérculos, pescados, mariscos, quesos/embutidos cortados y otros graneles pueden incorporarse cuando realmente se vendan por peso. Una bolsa fija con EAN o un aguacate vendido por unidad mantiene su presentación. Separar corte/especie, hueso/piel, fresco/congelado, variedad y demás atributos relevantes; no crear un genérico amplio «Carne» para productos diferentes.

### Corridas, deduplicación y recuperación

El scraper crea UUID y confirma cada lote del target. Al terminar el recorrido normal cierra la corrida aunque algunos objetivos fallen; cerrada no significa exitosa. Un corte abrupto deja corrida abierta y las filas PENDING fuera del procesador. Relanzar crea otro UUID y no cierra automáticamente el anterior: revisar que ya no exista un escritor antes de recuperar esa ejecución.

Se deduplica solo dentro de una tienda, identidad y UUID cerrado: menor precio efectivo válido, luego menor ID. En peso variable compiten los precios por kg de los SKU asociados al mismo genérico. No se agrupan tiendas distintas ni filas legadas por una ventana de una hora. Los registros anteriores sin run_id siguen individuales; no se les asigna una corrida inventada.

Los lectores tampoco tienen el mismo alcance: el VTEX común consulta hasta 50 productos por objetivo y el primer item; facial pagina por MAX_PAGINAS (4 por defecto, 50 por página). No se ha implementado extracción exhaustiva de todas las variantes. El lector común tiene reintentos de red/429/5xx; facial tiene su control de bloqueos y pausas propio.

### Límites de fuente y clasificación pendientes

- VTEX regional común usa simulación para Valledupar; D1 usa catálogo nacional. Facial no tiene esa simulación regional.
- Farmatodo usa fullPrice de VUP y stock publicado con alcance nacional; no prueba inventario de una sede local. Farmanorte usa precio HTML predeterminado, sin confirmar ciudad.
- Páginas/sitemaps comprueban reglas de robots y pausas con el lector común. Facial usa robots para Crawl-delay y no verifica can_fetch de la ruta de catálogo: es una limitación conocida, no una política uniforme de todos los scrapers.
- Los targets por ingredientes (retinol, ceramidas, hialurónico, etc.) no prueban que el producto sea sérum. Los rescates textuales no cambian category_id. Revisar categorías de targets y maestros antes de ampliar captura o equivalencias.
- Se revisaron discrepancias de unidad/presentación entre Cutis/Medipiel y mapas de BellaPiel con categoría distinta. Esto no certifica una corrección del catálogo compartido. No establecer tamaños definitivos por un slug ni por una muestra de conversación.

### Descubrimiento de URLs y curaduría de droguería

`buscar_urls.py` lee CSV o staging de La Rebaja y sitemaps públicos, con caché, pausa y propuestas por similitud de palabras/números/tamaño. Respeta falta_en para buscar solo tiendas pendientes y permite sustituir los IDs de Farmatodo/Farmanorte de su salida. Sus candidatas no verifican un EAN ni crean ofertas ficticias.

Circuito vigente: `consulta_faltantes.sql` → propuestas/revisión de página → `crear_targets_droguerias.sql` con expected_ean explícito → captura → `verificar_targets.sql`. Confirmar IDs de tienda, categoría, presentación y restricción de términos antes de ejecutar ejemplos SQL. Una URL retirada, distinta o sin identidad verificable se corrige/desactiva; no se asigna el EAN esperado como si lo hubiera publicado.

### Búsqueda por sección y robustez de navegación

Los endpoints públicos de catálogo son `GET /api/categories/roots`, `GET /api/categories/active?sectionId=…` y `GET /api/stores/active?sectionId=…`. Sin filtro mantienen su consulta global. La raíz solicitada debe existir, estar activa y ser raíz. No se agrega una tabla de secciones ni se fija un ID numérico en React.

El catálogo y los resultados se vinculan al alcance actual, se cancelan peticiones obsoletas y se evita que una respuesta tardía restaure filtros de otra sección. Un enlace con tienda ajena limpia ese filtro/página antes de buscar. El error de catálogo tiene recuperación visible y la opción Todas mantiene el sector.

La resolución del merge introdujo dos errores locales en Search: knownSectorId sin definir y StoreLogo sin importar. El parche 08 los corrige usando selectedSectorId y el componente de logo existente, preservando useSearchCatalog, navegación de develop y filtros acotados. No sustituye toda la página por una versión antigua.

React StrictMode se conserva. En desarrollo puede ejecutar efecto → limpieza → efecto; los hooks abortan la primera petición y la siguiente puede terminar en 200. Se reprodujo para categories/roots y para search?size=12&sort=SAVINGS (ejemplos de Home). En build de producción esa duplicación de efectos no se reprodujo; navegar puede cancelar peticiones igualmente. Un canceled con sustituta 200 no es un HTTP 500.

El parche 09 consulta DisconnectedClientHelper de Spring en GlobalExceptionHandler: una desconexión reconocida deja DEBUG breve sin stack trace y no intenta escribir otro JSON sobre la conexión cerrada. Las excepciones reales conservan ERROR/500; las pruebas cubren expresamente aplicación, lectura de archivo, base de datos y API externa. La clasificación no consiste en silenciar todo IOException ni quitar AbortController/StrictMode.

### Migraciones, preflight y pruebas

Orden de las ampliaciones de esta entrega, sobre el esquema base y sus dependencias:

1. `scripts/2026-11-scraper-procesador.sql`.
2. `scripts/2026-11-bellapiel-identidad-secundaria.sql` (requiere f_unaccent).
3. `scripts/2026-11-facial-sku-conflict-minimo.sql`.

Detener escritores, respaldar, ensayar primero en una base aislada y desplegar backend/scrapers compatibles después del esquema. Son migraciones de una aplicación, no seeds repetibles. Las tablas nuevas conservan RLS y revocaciones. Preflight debe revisar también índices, CHECK, funciones, jerarquía y permisos: ddl-auto=validate no prueba todo eso.

`scripts/preflight_compartida.sql`, `scripts/auditar_identidad_y_presentaciones.sql` y `scripts/verificar_targets.sql` permiten revisar el estado; leer sus consultas y el destino antes de ejecutarlos. Conectarse a la base correcta: `postgres` no es necesariamente la base del proyecto. Si psql aborta una transacción por un error, ROLLBACK antes de continuar. Cero incidencias no prueba cobertura si no existen objetivos de esa tienda.

Las reversiones van en orden inverso, con exportación previa de curaduría/evidencia y procesos detenidos. La reversión mínima se niega si quedan CONFLICT o has_ean=false: no eliminar la protección de esas filas sin resolver su destino. Revertir esquema no deshace productos/precios/avisos ya confirmados. Los ejemplos históricos de DELETE/UPDATE no son un procedimiento de integración compartida.

Pruebas mínimas reproducibles, desde la raíz y con dependencias instaladas:

```powershell
$env:PYTHONPATH = 'scrapers'
python -m pytest -q scrapers/tests

$env:STAGING_ENABLED = 'false'
$env:PRECIOSCO_STAGING_ENABLED = 'false'
$env:MAIL_ENABLED = 'false'
$env:APP_NOTIFICATIONS_MAIL_ENABLED = 'false'
Set-Location backend
# Definir SPRING_DATASOURCE_URL/USERNAME/PASSWORD y JWT_SECRET de una base aislada.
.\mvnw.cmd test '-Dsurefire.excludedGroups='
```

`mvn test` normal excluye el grupo integration. Vaciar surefire.excludedGroups lo incluye, pero la variable de entorno SPRING_DATASOURCE_URL debe existir en la misma terminal: la conexión en application-local.properties no cumple @EnabledIfEnvironmentVariable. Verificar cero omitidas en surefire-reports; un BUILD SUCCESS con 50 Skipped no verificó integración.

Las pruebas Python usan fixtures y las de navegador una API simulada; no certifican fuentes actuales ni datos compartidos. Para navegador, iniciar Vite y ejecutar `python -m unittest discover -s frontend/tests -p 'test_*.py' -v`; FRONTEND_TEST_URL debe coincidir con el servidor. ERR_CONNECTION_REFUSED indica que ese frontend no está disponible. Las ocho pruebas de desconexiones se ejecutan con `mvnw test -Dtest=GlobalExceptionHandlerTest` y no requieren PostgreSQL.

### Estado de integración y trabajo pendiente

| Elemento | Evidencia disponible | Pendiente que no debe darse por completado |
| --- | --- | --- |
| Corridas, overrides, peso variable, identidad secundaria, auditoría y filtros por sección | Código/scripts presentes en 85f463a tras PR #68/#69 y merge de develop. | Aplicación/curaduría por base y autorización de la instancia procesadora. |
| Esquema mínimo has_ean/CONFLICT y CSV | Presente en 85f463a; 34 pruebas Python y 247 Java aprobadas en la validación previa al merge con develop. | Confirmar migración de cada entorno y revisar las ofertas reales/artefactos. |
| Arreglo de Search tras el merge | Archivo local del usuario corregido y funcionamiento confirmado; lint/build y 10 pruebas de navegador aprobados. | Publicar el arreglo local en la rama/PR; no se verificó como commit remoto en este corte. |
| Desconexiones del cliente | Parche 09 validado sobre 85f463a: 8 pruebas específicas y suite de 210 unitarias aprobadas. | Integrar/publicar y reiniciar backend; no se volvió a ejecutar integración con PostgreSQL para ese ajuste. |
| Genéricos y equivalencias | Diseño y consultas/cargas manuales preparados; no se inventaron asociaciones. | Elegir productos reales, comprobar peso/base de precio y cargar mapas justificados. |
| Ayudantes SQL de genéricos y guía extensa de flujo | Entregas auxiliares fuera de los commits de referencia. | Versionarlos si el equipo decide mantenerlos; estos documentos no dependen de ellos. |
| Base compartida | Se identificaron comprobaciones de esquema y categorías y el usuario informó algunas correcciones. | Ejecutar/revisar preflight actual y documentar saneamientos; no se certifica desde el repositorio. |
| Operación automática | Workflow nuevo manual, estrategias y scheduler configurables. | Acordar cron/operación, reactivar fuentes bloqueadas solo con verificación y decidir retiro del workflow anterior. |

La actualización documental no crea maestros, mapas, objetivos ni migraciones adicionales, no ejecuta SQL y no constituye aprobación de un despliegue o limpieza histórica.

## Sprint 4 — Acuerdos del equipo del 20-nov-2026

El equipo prioriza IA especializada, planes de suscripción y el primer panel administrativo. Abisael retoma HU-35 (SCRUM-305) bajo SCRUM-5 después de que el flujo de pagos funcione de punta a punta. La publicación de la plataforma (HT-06, SCRUM-37) y la vigencia del precio (HU-36, SCRUM-306) quedan en backlog.

### Repuestos de automóviles y selección de cuidado facial

- **Repuestos (Darwin, SCRUM-156 / EP-06).** Las historias HU-32, HU-33 y HU-34 (SCRUM-161, SCRUM-162 y SCRUM-163) quedan en backlog. El Sprint 4 conserva HT-28, scraper de repuestos de automóviles, y SCRUM-295, prueba de datos en curso. Los repuestos para motocicletas quedan fuera del alcance actual.
- **Fuentes y criterio de viabilidad.** SCRUM-295 usa Importadoras Asociadas, Autoplanet y Zona Repuestera. Se revisan 20 productos por tienda y la coincidencia exacta de marca y referencia del fabricante en dos o más tiendas. Un código OEM sirve para medir coincidencias si identifica la misma pieza y se verifica su marca. Si menos del 30 % de los repuestos muestreados aparece en dos o más tiendas, el equipo replanteará EP-06 como listado con buscador por referencia. La prueba y su resultado preceden a la decisión de activar el comparador.
- **Ingesta.** Darwin revisará robots.txt y términos de las tres fuentes, construirá el cliente y cargará los objetivos autorizados en `scrape_targets`. Antes de habilitar publicación automática se verificará la configuración vigente de `identity-pending-domains` y se mantendrá la guarda de Repuestos hasta validar la identidad por marca y referencia del fabricante en staging y en el procesador.
- **Cuidado facial (SCRUM-155 / EP-05).** La selección vigente es Cutis, Medipiel y BellaPiel. Se cambió durante el desarrollo para facilitar la captura sobre VTEX. Farmatodo–Cuidado facial, Ánimus Lab y Botanikalia fueron propuestas que nunca se implementaron. La identidad secundaria de BellaPiel se mantiene. La revisión de discrepancias de presentación entre Cutis y Medipiel consta en el registro del 18-nov-2026. La selección queda confirmada sin una tarea adicional de verificación en el Sprint 4.

### IA especializada (Ayner)

- HT-20 define un cliente y un modelo compartido, con prompts y catálogo por sección. El cliente compartido habilita HU-37 a HU-40. Se reutilizan RF29 a RF32 para supermercados, recetas, droguería y cuidado facial. Las respuestas se basan en productos y precios del catálogo.
- HT-21 cubre los avatares flotantes en el lateral derecho. El avatar de la sección abre un chat temporal y el historial se mantiene en memoria de React hasta cerrar el chat o recargar la página. HT-21 utiliza el contador por cuenta de HT-23. El consumo se registra con independencia del historial. En móvil los avatares se reúnen en un único botón flotante.
- El servicio de IA de Droguería excluye los medicamentos que requieren fórmula médica antes de enviarlos al modelo, con independencia del prompt y de la configuración del scraper. Todas las respuestas muestran el aviso de que el asistente no diagnostica, prescribe ni sustituye a un profesional de la salud. La fuente del atributo de venta bajo fórmula queda por definir. Las respuestas de cuidado facial mantienen el aviso orientativo.

### Planes y pasarela de suscripción (Abisael)

- **Origen.** La pasarela y el modelo freemium provienen de una exigencia de Programación Web, incorporada al alcance integrado del proyecto. El pago habilita funciones de PreciosCo. La compra directa de productos, los pagos a tiendas y los domicilios permanecen fuera del alcance.
- **Acceso.** La búsqueda y la comparación son gratuitas incluso sin sesión. Usar IA, seguir productos, comentar o calificar exige una cuenta de consumidor con sesión. El plan Gratuito admite hasta 4 consultas de IA por cuenta al mes, hasta 200 caracteres por mensaje completo, incluida la lista entera, y hasta 5 productos seguidos simultáneamente. El contador de consultas se reinicia mensualmente. Al intentar exceder un tope se bloquea esa acción y se ofrece Premium. La suscripción confirmada amplía esos límites. Según la planificación, ninguna cuenta supera actualmente los 5 seguimientos y el sprint no incluye una migración de excedentes.
- **HT-22, selección.** Se elige **Mercado Pago Colombia** para la integración de prueba de Premium. La justificación y el flujo de pagos se registran abajo. [HT-22 (SCRUM-328)](https://abisaelfreylealmenarez.atlassian.net/browse/SCRUM-328)
- **HT-23, modelo y cuotas.** Definir el modelo de planes, suscripciones y contadores e implementar el reinicio mensual. El precio de suscripción y los límites ampliados de Premium quedan pendientes de decisión.
  - **Reinicio del contador de IA (HT-23, 23-nov-2026).** `ai_queries_reset_at` es por cuenta, no un cron global: cada usuario tiene su propia fecha de reinicio, que avanza +1 mes sobre la fecha anterior (nunca sobre `now()`, para no acumular drift). Para las cuentas existentes al migrar, se inicializa en `now() + 1 mes`, porque no hay una fecha de alta confiable para todas. Un `@Scheduled` (initialDelay/fixedDelay, sin cron, igual que `PriceDropDetector`) resetea por lote las cuentas cuya `ai_queries_reset_at` ya venció.
- **HT-24, integración.** Checkout Pro vía Orders API confirma el pago en el backend antes de activar Premium. El flujo y su verificación están descritos abajo. [HT-24 (SCRUM-330)](https://abisaelfreylealmenarez.atlassian.net/browse/SCRUM-330)
- **HT-25, validación de límites.** Usar el modelo de HT-23 para aplicar el control de acceso y los topes desde el backend, tanto para IA como para el seguimiento existente.

#### HT-22 Elección de la pasarela (21-nov-2026)

**Decisión.** Usar Mercado Pago Colombia en sandbox para probar el pago que habilita Premium. La comparación de Wompi, PayU, Stripe y Mercado Pago consideró SDK, notificaciones y disponibilidad de pruebas. Wompi era la primera opción, pero durante la evaluación se reportó que el alta de nuevas cuentas estaba pausada. Mercado Pago ofrece un SDK para Java, webhooks firmados y credenciales de prueba. Evidencia de la evaluación en `PasarelaDePagoHT-22.pdf`, pp. 2–4. [SDK de Java](https://github.com/mercadopago/sdk-java) · [Pruebas de pago en Checkout Pro Orders](https://www.mercadopago.com.co/developers/es/docs/checkout-pro-orders/integration-test/test-purchase-with-card?scope=prod)

#### HT-24 Checkout Pro vía Orders API

**Inicio del checkout (RF62 y RN-37).** `POST /api/subscriptions/checkout` exige sesión con rol `USER`. Guarda un intento `CREATED` con un UUID propio en `external_reference` y el importe y la moneda configurados en `app.premium.price` y `app.premium.currency`. Crea la Order en `POST /v1/orders`, conserva su ID en `external_order_id` y devuelve `201` con `externalReference` y `checkoutUrl`. Si falla la creación en Mercado Pago, devuelve `502` y conserva el intento. Las URLs de retorno apuntan al frontend y se configuran en `config.online`. [Crear una Order](https://www.mercadopago.com.co/developers/es/docs/checkout-pro-orders/create-order)

**Confirmación del pago (RF62, RN-37 y RNF14).** `POST /api/payments/webhook/mercadopago` recibe el tópico `order`. Valida `x-signature`, `x-request-id` y el `data.id` de la URL con el secreto de la aplicación. Una firma inválida obtiene `401`. El backend consulta `GET /v1/orders/{id}` con el Access Token y verifica la referencia, el importe y la moneda. Solo `processed/accredited`, con el total acreditado esperado, crea una suscripción `ACTIVE` y cambia el plan a `PREMIUM`. El bloqueo del intento y la comprobación de `subscription_id` en la misma transacción evitan duplicar la activación. [Notificaciones de Orders](https://www.mercadopago.com.co/developers/es/docs/checkout-pro-orders/notifications) · [Obtener Order por ID](https://www.mercadopago.com.co/developers/es/reference/online-payments/checkout-pro/get-order/get)

| Estado de la Order | Qué registra y hace PreciosCo |
| --- | --- |
| `created`, `processing` o `action_required/waiting_capture` | Registra el estado y conserva el plan vigente. |
| `action_required/waiting_retry` o `failed` | Registra el estado de la Order y, cuando está disponible, el detalle del último pago fallido en `transactions.payments`. Conserva el plan vigente. |
| `processed/accredited` | Activa Premium una sola vez tras comprobar el importe acreditado. Un aviso posterior de un estado previo a la acreditación no revierte el plan. |
| `canceled` o `expired` | Registra la cancelación o el vencimiento y conserva el plan vigente. |
| `processed/partially_refunded` o `refunded` | Registra el reembolso parcial o total y conserva el plan vigente. |

Las órdenes locales en `canceled`, `expired` o `refunded` se omiten antes de consultar al proveedor. La revocación de acceso por reembolso o contracargo, la renovación automática y la gestión de mediaciones quedan pendientes. [Estados de Orders](https://www.mercadopago.com.co/developers/es/docs/checkout-pro-orders/payment-management/status/order-status?scope=prod)

**Configuración.** Para Docker, define las variables de `backend/.env.example` en `backend/.env`. En **Tus integraciones → Webhooks → Prueba**, registra la URL HTTPS del túnel ngrok con el sufijo `/api/payments/webhook/mercadopago` y selecciona **Order (Mercado Pago)**. Actualiza la URL en el panel si cambia el túnel y configura `MERCADO_PAGO_WEBHOOK_SECRET` con la firma de esa aplicación. Aplica el ajuste de `external_order_id` y `external_reference` de la [migración HT-23](../scripts/2026-11-ht23-planes-suscripciones.sql) antes de arrancar este backend. [Configuración de Webhooks](https://www.mercadopago.com.co/developers/es/docs/checkout-pro-orders/notifications)

**Prueba manual en sandbox.** Inicia un checkout por escenario con un usuario de prueba en `FREE`. Usa la tarjeta oficial y el titular `APRO` para aprobación, `FUND` para rechazo por fondos insuficientes o `CONT` para pendiente. Comprueba en ngrok que el webhook de la misma Order recibió `200`. El aprobado debe enlazar una sola suscripción y dejar al comprador en `PREMIUM`. El rechazo y el pendiente deben conservar `FREE`. Para probar un duplicado, selecciona la entrega original en **Inicio → Webhooks → Prueba** y usa **Reenviar notificaciones**. Verifica que el mismo `data.id` vuelve a recibir `200` y conserva el mismo `subscription_id`. [Tarjetas de prueba de Colombia](https://www.mercadopago.com.co/developers/es/docs/checkout-pro-orders/integration-test/test-purchase-with-card?scope=prod) · [Monitoreo y reenvío de notificaciones](https://www.mercadopago.com.co/developers/es/docs/checkout-pro-orders/notifications)

**Prueba automatizada.** Desde `backend`, con JDK 25, usa una base PostgreSQL de pruebas con HT-23 aplicado y define `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` en la misma terminal. El test simula el `GET` a Mercado Pago y usa un pool de dos conexiones para probar concurrencia.

```powershell
.\mvnw.cmd test '-Dtest=OrderWebhookIntegrationTest' '-Dsurefire.excludedGroups='
```

El resultado esperado es `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`. La suite cubre firma inválida, acreditación, duplicados, rechazo, pendiente, importe o moneda incorrectos, referencia inexistente y entrega concurrente.

### Panel administrativo (Ramos)

- El panel y sus endpoints se protegen con el rol ADMIN. La cuenta inicial de administración se habilita asignándole el rol en la base de datos. Los cambios administrativos conservan fecha y responsable.
- El Sprint 4 incluye listado general y CRUD de clientes (HU-44), gráficas de clientes activos en tiempo real y promedios (HU-45), rankings de los productos más seguidos y más buscados (HU-46), CRUD de marcas, productos, tiendas y categorías (HT-26) y series históricas de precios de productos (HT-27). Las definiciones de actividad, periodos y actualización de métricas quedan por acordar.
- Antes de construir el CRUD de catálogo, HT-26 evaluará durante cuatro horas si una herramienta integrable como AdminJS o Retool cubre las operaciones requeridas con Spring y PostgreSQL, el control por rol y la auditoría. HT-27 requiere una consulta de series de tiempo sobre `prices`.
- Quedan en backlog el seguimiento administrativo de planes, la gestión de `scrape_targets` y `scraped_offers`, la moderación de comentarios y reportes, la gestión de seguimientos y calificaciones y las vistas con nombres legibles para `store_sku_ean_map` (equivalencias SKU–EAN), `store_product_links` (vínculos SKU–producto) y `ean_size_overrides` (correcciones de presentación por EAN). La carga manual de precios desde el panel queda por definir.
