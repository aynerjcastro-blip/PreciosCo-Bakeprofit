# DECISIONS.md — PreciosCo / BakeProfit / ComprasCo

Registro de decisiones de equipo: desviaciones deliberadas del MVP y acuerdos tomados durante el desarrollo, para que no se lean como descuidos en una revisión de código o una evaluación.

## Modelo de datos

- **Zona horaria**: todas las fechas son hora de Colombia (America/Bogota, UTC-5 sin horario de verano) en columnas TIMESTAMP sin zona. Se fija en tres lugares porque cada uno tiene su propio reloj: la base (`scripts/2026-11-zona-horaria-bogota.sql`, para los `DEFAULT now()`), el backend (`TimeZone.setDefault` en `BakeprofitApplication`) y el scraper (`ahora_colombia()`, con UTC-5 fijo para no depender de `tzdata` en Windows). Antes, Supabase y GitHub Actions guardaban UTC y el backend la hora local, con 5 horas de diferencia entre filas de la misma corrida.

- **Jerarquía de categorías**: `category.parent_id` autorreferenciada. Las filas raíz son dominios (Supermercado hoy; Droguería, Cuidado facial y Repuestos más adelante). `store` apunta a una categoría raíz; `product` apunta a una categoría hoja. Reemplaza la idea de una tabla `establishment_type` separada.
- **Jerarquía de dos niveles**: dominio (raíz) y categoría (hoja); `CategoryService` no admite un tercer nivel. Los productos cuelgan de la hoja, por ejemplo Supermercado › Granos y cereales. Se descartó convertir cada producto genérico ("Arroz") en una categoría: el comparador trabaja sobre un solo producto seleccionado, así que no mezcla productos distintos de una misma categoría.
- **Campo "marca"**: se integra al modelo en el Sprint 2 (antes estaba excluido). La exclusión se tomó cuando la marca no se podía obtener por separado; la API VTEX la entrega en un campo propio. Tabla `brands` con nombre único sin distinguir mayúsculas y `products.brand_id`.
- **Identidad del producto (ADR-01)**: los productos comunes se identifican por su EAN y los repuestos por la pareja marca + referencia del fabricante. Se descartaron el SKU (cambia en cada tienda) y el número OEM (compara equivalencias entre marcas, cercano a la compatibilidad, que está fuera del alcance).
- **EAN como GTIN-14**: se acepta con 8, 12, 13 o 14 dígitos y se guarda siempre rellenado con ceros a la izquierda hasta 14 (`products.ean`, único cuando no es nulo), para que un UPC-A y su EAN-13 equivalente coincidan. La clase `Gtin` es la única fuente de la regla: `classify` reconoce el tipo de código (GTIN-8, GTIN-13, medida variable o empaque) y `assess` devuelve un veredicto: INVALID (formato o dígito verificador), PLACEHOLDER (puros ceros), STORE_INTERNAL (circulación restringida: EAN-8 con prefijo 0 o 2, EAN-13 con prefijo 02, 04 o 2), VARIABLE_MEASURE (GTIN-14 con indicador 9: su precio no es comparable entre tiendas) o COMPARABLE. Solo un código COMPARABLE identifica un producto. Los empaques (GTIN-14 con indicador del 1 al 8) son productos distintos de la unidad suelta, porque su precio también es distinto. La API muestra el código con `toDisplay` (8 o 13 dígitos según su tipo).
- **Referencia del fabricante**: se normaliza a solo letras y números ASCII en mayúsculas (la Ñ y las tildes se eliminan; es aceptable porque las referencias de fabricante son ASCII), con una columna generada (`manufacturer_ref_norm`) y la misma regla en `ManufacturerReference.normalize()`. Una referencia exige marca (CHECK) y la pareja marca + referencia normalizada es única. La búsqueda por referencia (HU-33) normaliza lo que escribe el usuario con la misma regla.
- **Contenido neto y staging (ADR-03)**: cada producto tiene contenido neto (`net_content`, en la unidad base) y unidad base (`unit_base`: G, ML o UN), que van juntos o no van; "Arroz Sonora 500 g" y "Arroz Sonora 1000 g" son productos distintos. El scraper escribe en `scraped_offers` y `scrape_targets` define qué se busca en cada tienda (ver Fase 3). Esta decisión es posterior a ADR-01 y vino del cambio del scraper a la API VTEX.
- **Marcas**: solo `BrandService` las crea, con `INSERT ... ON CONFLICT DO NOTHING` sobre el índice `lower(name)` y búsqueda con `lower(name)` (no `IgnoreCase`, que usa `upper` y no es simétrico en Unicode); así dos creaciones simultáneas obtienen la misma marca. No hay endpoints de marcas: su administración es del panel administrativo (EP-09).
- **Pruebas de integración**: las pruebas que necesitan PostgreSQL real llevan `@Tag("integration")`, quedan fuera de `mvn test` y solo corren si `SPRING_DATASOURCE_URL` está definida (si no, se omiten, para no escribir en la base compartida). Se ejecutan contra la base de pruebas con `./mvnw test "-Dgroups=integration" "-Dsurefire.excludedGroups="` (en PowerShell, cada `-D` entre comillas). La primera es `BrandConcurrencyIntegrationTest`, que comprueba que dos creaciones simultáneas de la misma marca obtienen la misma fila. En el procesador, supermercado, droguería y cuidado facial crean marcas solo desde el campo de marca de la tienda; en repuestos solo se emparejan marcas existentes y una desconocida queda sin clasificar. `ProductSearchIntegrationTest` (búsqueda con filtros y paginación) y `PriceCompareIntegrationTest` (tiendas inactivas en el comparador) siguen la misma regla. `StagingProcessorIntegrationTest` solo procesa las ofertas que inserta (`StagingProcessor.processOffers` con sus propios ids) y nunca llama a la corrida completa ni a la retención, para no tocar ofertas ni filas ajenas aunque apunte a la base equivocada.
- **URL e imagen del producto en la tienda**: se siguen guardando en `prices.product_url` y `prices.image_url` (SCRUM-49). No se crea una tabla `store_listings` en el Sprint 2.
- **Esquema por scripts y `ddl-auto=validate`**: Hibernate no crea ni modifica tablas (`spring.jpa.hibernate.ddl-auto=validate` en `application.properties`). Todo cambio de esquema va en un script SQL versionado en `scripts/`, con su script de reversión, y las entidades JPA se mantienen sincronizadas con él. Como `application-local.properties` está en `.gitignore` y sobrescribe a `application.properties`, cada integrante borra de su archivo local la línea `ddl-auto`. `validate` solo detecta tablas o columnas faltantes y cambios de tipo base; longitudes, precisión, nulabilidad, CHECK e índices se comprueban con la consulta de sincronización de cada script. Los CHECK, índices parciales y funcionales y columnas generadas se declaran solo en el script. El script se prueba primero en una base de pruebas (un segundo proyecto de Supabase o PostgreSQL local; el plan Free no incluye ramas) y después se corre en la base compartida, avisando al equipo.
- **Convención de FK**: `fk_tabla_tabla` (por ejemplo `fk_ratings_products`).

## Seguridad de la base de datos

- **Solo el backend accede a los datos (SCRUM-297)**: Supabase publica cada tabla del esquema `public` por su API REST, y la clave `anon` está diseñada para ser pública. Por eso todas las tablas tienen Row Level Security activada sin políticas, los roles `anon` y `authenticated` no tienen permisos sobre tablas ni secuencias, y los privilegios por defecto impiden que las tablas nuevas los reciban (`scripts/2026-11-seguridad-api-supabase.sql`). El backend se conecta como `postgres`, dueño de las tablas, y RLS no le aplica. Ningún componente del proyecto (frontend, scraper ni funciones externas) usa la API REST de Supabase; el scraper se conecta directo a PostgreSQL.
- **Toda tabla nueva** se crea con `ENABLE ROW LEVEL SECURITY` en su script, aunque los privilegios por defecto ya la protejan.
- **Toda función nueva en `public` (SCRUM-304)**: Supabase publica por `/rpc` las funciones del esquema `public`, y sus privilegios por defecto dan `EXECUTE` a `PUBLIC`, `anon` y `authenticated` sobre cada función nueva. Por eso el script que crea una función revoca `EXECUTE` a los tres por nombre (revocarlo solo a `PUBLIC` no basta), y su consulta de sincronización lo verifica con `has_function_privilege`: las tres comprobaciones deben dar `false`. El backend se conecta como `postgres`, dueño de la función, así que no necesita un `GRANT`.

## Fase 3 — Carga de precios y comparadores (Sprint 2)

- **Scraper con la API VTEX**: D1, Éxito, Jumbo y Olímpica usan VTEX y exponen una API pública de catálogo en JSON (`/api/catalog_system/pub/products/search`; Éxito con el prefijo `/io/`; D1 en su dominio nuevo, `www.d1.com.co`). Reemplaza a Selenium: un solo cliente con la URL base de cada tienda, pausas entre peticiones y un User-Agent que identifica al proyecto. Riesgos aceptados: es una API pública sin acuerdo con las tiendas, y el precio puede depender de la región (se verifica contra la web con ubicación en Valledupar antes de confiar en él).
- **Tabla de staging**: el scraper en Python solo escribe datos crudos en `scraped_offers`. Un proceso `@Scheduled` en Java normaliza, empareja por EAN, crea productos, inserta precios y conecta el detector de bajas. Resuelve que no hubiera un punto en Java donde enganchar el aviso.
- **Historial de precios por periodos**: cada fila de `prices` es un periodo en el que el precio no cambió: `registration_date` es cuándo empezó y `last_seen_at` es la última vez que la tienda lo confirmó. Si el precio de hoy es igual al último de ese producto en esa tienda, solo se actualiza `last_seen_at` (y la URL y la imagen, solo si la oferta las trae: una oferta sin imagen no borra la que ya funcionaba); si cambió, se inserta una fila nueva. Así el comparador muestra un precio verificado hoy sin perder el historial (antes se movía la fecha y se perdía cuándo empezó cada precio), la tabla crece solo con los cambios y dos filas consecutivas siempre tienen valores distintos, que es lo que compara el detector de bajas (RF27). `value`, `registration_date`, el producto y la tienda nunca se modifican; nunca se usa `ON CONFLICT DO UPDATE` sobre `prices`.
- **`prices.source`**: guarda el nombre del producto tal como lo publica la tienda (por ejemplo, "Arroz Diana blanco x5kg"), para rastrear de qué oferta salió cada precio. Se recorta a la longitud de la columna.
- **Retención del staging**: el procesador borra al final de cada corrida las ofertas PROCESSED con más de 2 días y las UNCLASSIFIED con más de 7 (configurables); las PENDING nunca se borran. Se eligieron 7 días, y no 5, porque los sprints son semanales: una revisión semanal alcanza a ver todo lo rechazado desde la anterior. Las ofertas sin clasificar se revisan dentro de esos 7 días.
- **Procesador de staging: una transacción por oferta (HT-10)**: `StagingProcessor` no es transaccional a propósito. Cada oferta se procesa en su propia transacción (`OfferIngestionService`), cargada con `SELECT ... FOR UPDATE` y omitida si ya no está PENDING, así que dos instancias no la procesan dos veces. Si una oferta lanza una excepción, se marca UNCLASSIFIED en otra transacción, separada y nunca anidada, y el lote sigue. Así nunca hay dos conexiones a la vez (pool de 2, RP-04) y un error no aborta las demás ofertas (en PostgreSQL, una sentencia fallida invalida toda su transacción). Las ofertas PENDING se recorren por clave (`scraped_at`, `id`), no por número de página, porque cada oferta procesada sale de PENDING y se saltarían filas. No se agrega `@Transactional` al orquestador.
- **Evento de precio nuevo**: por cada fila nueva en `prices` se publica `PriceRegisteredEvent`, donde HT-11 conecta el detector de bajas. Lo publica el orquestador después del commit de la oferta, fuera de toda transacción; el listener usa `@EventListener` con su propia transacción. Se descartaron `BEFORE_COMMIT` (un error en el aviso aborta la transacción y se perdería el precio) y `@Async` (toma una segunda conexión con el pool de 2). Un precio repetido no publica evento. Riesgo aceptado: si la aplicación se detiene entre el commit y la publicación, ese aviso se pierde.
- **Producto existente con otro tamaño**: si el EAN ya existe con otro contenido neto o unidad, la oferta queda UNCLASSIFIED; un producto existente nunca se modifica desde el procesador (RN-02).
- **Repuestos en el procesador**: hasta que `scraped_offers` tenga la referencia del fabricante (Sprint 3), toda oferta de repuestos queda UNCLASSIFIED con "identidad por referencia pendiente (Sprint 3)". La regla de marcas existentes y la identidad pendiente son dos propiedades separadas (`existing-brands-only-domains` e `identity-pending-domains`, con el nombre de la categoría raíz); en el Sprint 3 basta con quitar Repuestos de la segunda. Al arrancar se advierte en el log si un dominio configurado no coincide con ninguna categoría raíz.
- **Activación del procesador**: apagado por defecto (`STAGING_ENABLED=false`) y siempre apagado en la prueba de integración, que llama a `processOffers` directamente. Contra la base compartida lo activa una sola persona del equipo a la vez. Por ahora corre 3 veces al día: una corrida poco después de arrancar la aplicación (`initial-delay` de 1 minuto) y luego cada 8 horas (`fixed-delay` de 8 horas, configurable). Se prefirió un intervalo fijo a horas fijas (cron) porque, mientras no haya despliegue, el backend corre en el equipo de un integrante y no está encendido a horas fijas: con el intervalo, cada arranque procesa lo pendiente. Al desplegar se revisa si conviene pasar a horas fijas, alineadas con las corridas del scraper.
- **`scrape_targets` y filtros anti-basura**: el diccionario `PRODUCTOS` pasa a una tabla (término, categoría de la tienda, unidad esperada, rango de tamaño, activo). Un producto entra al catálogo solo si viene de la categoría correcta de la tienda, tiene la unidad esperada, su tamaño está en el rango y su precio es mayor que 0; lo demás queda "sin clasificar".
- **Tamaño**: se lee de las especificaciones de la tienda y, si no vienen, del nombre; se compara como número en la unidad base, no como texto (`"500g" in "2500g"` era verdadero).
- **Reinicio de datos**: como todavía no hay usuarios reales, en lugar de migrar los productos genéricos se reinician los datos del catálogo y sus dependientes (productos, precios, opiniones, reportes, seguimientos y avisos) cuando el nuevo modelo esté listo, y se cargan de nuevo ya corregidos. Usuarios, tiendas y categorías se conservan.
- **Tiendas inactivas en el comparador**: el comparador (`findRecentPricesByProduct`) y el precio más bajo de Seguidos (`findMinPriceByProduct`) ignoran las tiendas inactivas, igual que la búsqueda. Antes las mostraban, aunque el comentario de `prices.js` decía lo contrario; así la búsqueda, el comparador y Seguidos usan el mismo conjunto de tiendas.
- **Comparación entre dominios**: un producto que se vende en supermercado y en droguería se compara en todas las tiendas que lo venden (RF07). Se descartó ocultar los precios de otro dominio.
- **Secciones por dominio (ADR-02)**: una sección muestra los productos con precio en al menos una tienda de ese dominio. Se descartaron categorías con varios padres (convierte la jerarquía en un grafo) y duplicar tiendas mixtas por dominio (parte el promedio de la tienda). Si se seleccionan tiendas mixtas, `store` pasará a tener varios dominios con una tabla intermedia.
- **Selección de tiendas**: para droguería y cuidado facial se prefieren tiendas sobre VTEX, para reutilizar el mismo cliente.
- **Índices**: `idx_products_category` e `idx_prices_product_store` (este último lo pide ADR-02 y también lo usa la búsqueda por precio vigente; ver "Búsqueda de productos"). No se crea `pg_trgm` por ahora: con 5.000 productos y 60.000 precios la búsqueda tarda unos 35 ms (medido en SCRUM-140). Como `f_unaccent` es `IMMUTABLE`, si el catálogo crece se puede crear un índice GIN de trigramas sobre `lower(f_unaccent(name))` sin cambiar el código.
- **Prueba de datos de repuestos**: antes de construir EP-06 se toman 20 productos por tienda y se mide cuántos tienen marca y referencia extraíbles y cuántas parejas aparecen en 2 tiendas o más. Si menos del 30 % aparece en 2 tiendas o más, EP-06 se replantea como listado con buscador por referencia y no como comparador.
- **Despliegue y rendimiento**: el despliegue se aplaza (el docente no lo exige por ahora). El rendimiento queda como tareas extra; la prueba de carga nunca se corre contra la base compartida de Supabase.
- **Patrón de diseño del scraper**: Strategy, del catálogo GoF.

## Fase 3 — Selección de tiendas de droguería y cuidado facial (SCRUM-168, 10-nov-2026)

Evidencia: dos ejecuciones de `verificar_tiendas.py` (robots.txt, API de catálogo, términos), lectura completa de los términos de las
tiendas que los publican y revisión de las páginas públicas. Lo marcado **PENDIENTE** no se pudo comprobar y no se da por
verificado. Este análisis no es asesoría legal.

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
- EAN: **no** (el `gtin` del JSON-LD es el id interno de la URL).
- Datos útiles: JSON-LD con nombre, marca y precio.

**Farmanorte** (droguería · riesgo medio)
- Plataforma: **VTEX**.
- robots.txt: **API (`/api/`) y búsquedas `map=ft` PROHIBIDAS**; permitidos páginas de producto `/…/p` y sitemap.
- EAN: **no** (vacío; marca "Generica").
- Datos útiles: presentación en el nombre ("* 10 TAB") y `Cantidad de unidades`.

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
- EAN: **no**.

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

1. **El EAN solo está en La Rebaja y Cutis.** ADR-01 (emparejar por EAN) no alcanza a Farmanorte, Farmatodo, Ánimus Lab ni
   Botanikalia. Hace falta emparejar por marca normalizada + nombre o principio activo + contenido neto (ADR-03), o la vista de
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
7. **Farmanorte**: su robots.txt prohíbe la API. Respetarlo implica usar solo sitemap y HTML, y aun así no trae EAN ni marca real:
   aporta poco. Tampoco sirve para cuidado facial en la API consultada (0 resultados de niacinamida y retinol). Decisión del equipo:
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

### Selección propuesta (a confirmar con el equipo)

- **Droguería (RF52, EP-04, HU-28/HU-29)**. Primera ola: **Drogas La Rebaja** (API, EAN, filtro OTC) y **Farmatodo - Droguería**
  (sitemap + HTML). Segunda ola: **Farmanorte** (solo HTML, sin EAN). **Cruz Verde**, solo con autorización escrita.
  **Droguería La Economía**, descartada por su `Crawl-delay` de 3600 s (hallazgo 13).
- **Cuidado facial (RF58, EP-05, HU-30/HU-31)**: **Cutis** (única con API y EAN), **Farmatodo - Cuidado facial**, **Ánimus Lab** (con
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
3. Decisión del equipo sobre Farmanorte (solo HTML o excluir) y sobre ADR-04.

## Búsqueda de productos (HU-06a, HU-06b)

- **Todos los filtros en SQL (SCRUM-140)**: reemplaza la decisión anterior de filtrar el nombre en Java. Se filtraba en Java porque un `LIKE` simple no encontraba "café" al escribir "cafe". Con la búsqueda paginada eso ya no sirve: filtrar en Java después de paginar deja páginas cortas o vacías, y paginar en memoria carga todo el catálogo. La regla sin tildes ni mayúsculas se conserva con `lower(f_unaccent(...))` a los dos lados de la comparación (`scripts/2026-11-scrum140-busqueda-sin-tildes.sql`): primero se quitan las tildes y después se pasa a minúsculas, para no depender del `LC_CTYPE` de la base. Java solo parte el texto en palabras y escapa `%`, `_` y `\` (`SearchTerms`); cada palabra es un `LIKE` y deben estar todas, en cualquier orden. Así el criterio de HU-06a "el filtro combinado se aplica en una sola consulta" se cumple literalmente.
- **`f_unaccent`**: envoltorio `IMMUTABLE` de `extensions.unaccent` con el diccionario calificado, para no depender del `search_path`. La extensión va en el esquema `extensions`, como las demás de Supabase. Sigue la regla de funciones nuevas de "Seguridad de la base de datos" (SCRUM-304): ni `PUBLIC`, ni `anon`, ni `authenticated` pueden ejecutarla. Antes de correr el script se verifica en qué esquema está la extensión `unaccent` en cada base. La reversión borra la función, no la extensión.
- **Un método por filtro**: `ProductSpecifications` tiene un método por filtro (activo, nombre, categoría, precio vigente) y `ProductRepository` extiende `JpaSpecificationExecutor`. El filtro por marca (también parte de RF06) queda para el Sprint 3 en HU-35 (SCRUM-305), como un método más.
- **Precio vigente**: el periodo más reciente del par producto-tienda, es decir, la fila de `prices` sin otra más nueva del mismo par (por `registration_date` y, si empatan, por `id`). `last_seen_at` no cambia cuál es. El rango se evalúa sobre esa fila y nunca sobre una histórica: si un producto costó 3.000 y hoy cuesta 5.000, no entra en un máximo de 4.000. Límites inclusivos; se puede enviar solo el mínimo, solo el máximo o ninguno.
- **Tiendas del filtro**: solo cuentan las tiendas activas. Sin tiendas seleccionadas, el producto entra si alguna tienda activa tiene su precio vigente en el rango; con una o varias, basta con que una de ellas lo tenga, y nunca cuenta el precio de otra tienda. Sin tienda ni rango no se exige ningún precio: la búsqueda de HU-06a sigue mostrando productos sin precio.
- **Paginación**: `page` desde 0, `size` de 20 por defecto y 50 como máximo (el mismo límite que las opiniones). El orden es por nombre y luego por id, y el cliente no lo elige, para que las páginas no repitan ni salten productos. La respuesta es un `PagedModel`: `{ content: [...], page: { size, number, totalElements, totalPages } }`. Es un cambio de contrato: antes era un arreglo, y por eso se adaptó `useSavingsDeal` (la tarjeta de ahorro del inicio).
- **Códigos de la búsqueda**: `200` también sin resultados; `400` por un precio negativo o no numérico, o por un mínimo mayor que el máximo; `404` por una categoría inexistente o por una tienda inexistente o inactiva, con los ids en el mensaje. No se usa `409` para una tienda inactiva: un `GET` no modifica nada, así que no hay conflicto de estado, y para un visitante una tienda inactiva no existe. Se eligió `404` y no `400` porque el mismo endpoint ya respondía `404` por una categoría inexistente.
- **Riesgo aceptado**: si una tienda deja de vender un producto activo, su último periodo sigue siendo el vigente, así que el filtro por esa tienda lo sigue mostrando con un precio viejo. Una ventana de vigencia según `last_seen_at` queda en HU-36 (SCRUM-306, backlog, DP-38): tendría que aplicarse igual en la búsqueda y en el comparador, y se retoma cuando el scraper corra a diario de forma automática, porque hoy ninguna ventana se puede verificar y vencería casi todos los precios.
- **Frontend**: los filtros de tienda, precio y página van en la URL de `/buscar` (`tienda` repetible, `min`, `max`, `pagina` desde 1), que es su única fuente de verdad. El texto y la categoría siguen como en HU-06a. Escribir en el buscador solo quita `q` y `pagina` de la URL (antes la vaciaba entera, incluido `sector`). Cualquier cambio de filtro vuelve a la primera página. El rango se aplica con el botón "Aplicar precio" o con Enter, no con cada tecla; el borrador valida lo mismo que el backend antes de pedir. Un `404` por una tienda que ya no está disponible ofrece quitar el filtro de tienda.

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
- **Códigos de estado** *(por confirmar con el equipo; las pruebas automáticas y la colección de Postman se escriben contra esto)*: `201` al crear; `400` por datos inválidos, por razón `OTRO` sin detalle o por reportar el propio comentario; `401` sin sesión; `403` por rol no permitido o por no ser el autor; `404` por producto, tienda u opinión inexistentes, por un par sin precio o por reportar un comentario oculto o eliminado; `409` por opinión repetida (también después de eliminar el comentario), producto o tienda inactivos, plazo de edición vencido, comentario no editable o eliminación de un comentario `OCULTO`; `204` al eliminar el comentario, al reportar (repetir cualquiera de los dos también responde `204`) y en `GET /me` cuando aún no hay opinión.
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
- **Comparador de repuestos**: se agrega a la Fase 3 un comparador de repuestos para vehículos y motos (RF55). Fuentes preseleccionadas para el scraper: Importadoras Asociadas, Autoplanet y Zona Repuestera. Los repuestos se comparan por marca y referencia o código del fabricante (RN-32); la compatibilidad con un modelo de vehículo queda fuera del alcance.
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
- **Documento de análisis en el repositorio**: la copia del repositorio quedó atrás (29 de octubre, hasta DP-24) mientras la del equipo seguía avanzando. Se sincroniza en un commit propio, sin otros cambios, y desde entonces cada PR que cambie una decisión actualiza el documento en la misma rama, partiendo de la versión que ya está en `develop`.
- Pendiente de reflejar en el documento de análisis lo decidido en la Fase 2: la regla RN-30 debe decir "al registrar y al editar" (eliminar el comentario deja una calificación sin texto visible), los códigos HTTP en HU-10, HU-16 y RNF08, y las decisiones nuevas como decisiones pendientes con su valor provisional.

## Autores en el historial de Git

- `luminuos09` → Ramos
- `Andresbtm` → Darwin
- `kstrodeveloper` y `kstroDev09` → Ayner

## Intención de este archivo

Las decisiones de equipo se toman y registran a nivel de equipo, no individualmente — una distinción importante en evaluaciones o revisiones de código. Este archivo deja constancia de esas decisiones para que una exclusión deliberada no se confunda con un vacío frente al MVP original.