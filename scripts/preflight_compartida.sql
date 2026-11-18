-- Ejecutar después de las migraciones, con procesador y scrapers detenidos.
-- Solo lectura: no copia IDs, mapas, staging ni precios de la base de pruebas.
BEGIN TRANSACTION READ ONLY;

SELECT current_database() AS base, current_user AS usuario,
       inet_server_addr() AS servidor, inet_server_port() AS puerto;

-- 1. Columnas y tablas de las dos migraciones. expected_ean es texto, NO booleano.
SELECT table_name, column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public' AND (
    (table_name = 'scrape_targets' AND column_name IN ('target_url','expected_ean'))
    OR (table_name = 'scraped_offers' AND column_name IN ('run_id','store_sku','sale_weight_kg'))
    OR table_name IN ('scrape_runs','ean_size_overrides','store_product_links','store_sku_ean_map')
    OR (table_name = 'prices' AND column_name = 'last_seen_at')
)
ORDER BY table_name, ordinal_position;

-- 2. Raíces y tiendas: cada tienda debe pertenecer al dominio correcto.
SELECT tienda.id AS store_id, tienda.name, tienda.active,
       categoria.id AS root_id, categoria.name AS dominio,
       categoria.parent_id, categoria.active AS dominio_activo
FROM stores tienda LEFT JOIN categories categoria ON categoria.id = tienda.category_id
ORDER BY categoria.name, tienda.name;

-- 3. Farmatodo/Farmanorte: revisar todo lo devuelto. La validación definitiva de
-- checksum y comparabilidad la hace Java, no esta comprobación de formato.
SELECT objetivo.id, tienda.name AS tienda, objetivo.search_term,
       objetivo.target_url, objetivo.expected_ean, objetivo.unit_base,
       objetivo.min_content, objetivo.max_content
FROM scrape_targets objetivo JOIN stores tienda ON tienda.id = objetivo.store_id
WHERE objetivo.active
  AND (tienda.name ILIKE '%farmatodo%' OR tienda.name ILIKE '%farmanorte%')
  AND (
      nullif(btrim(objetivo.target_url), '') IS NULL
      OR objetivo.expected_ean IS NULL
      OR objetivo.expected_ean !~ '^([0-9]{8}|[0-9]{12,14})$'
      OR objetivo.min_content IS NULL OR objetivo.max_content IS NULL
      OR objetivo.min_content <= 0 OR objetivo.min_content <> objetivo.max_content
  )
ORDER BY tienda.name, objetivo.id;

-- 4. Jerarquía de productos y objetivos: esperado cero filas.
SELECT 'producto' AS tipo, producto.id, producto.name AS nombre
FROM products producto JOIN categories categoria ON categoria.id = producto.category_id
WHERE categoria.parent_id IS NULL
UNION ALL
SELECT 'target', objetivo.id, objetivo.search_term
FROM scrape_targets objetivo JOIN categories categoria ON categoria.id = objetivo.category_id
WHERE categoria.parent_id IS NULL;

-- 5. Revisar fuentes ambiguas de cuidado facial; no aprobar categoría por ingrediente.
SELECT objetivo.id, tienda.name AS tienda, categoria.name AS categoria,
       objetivo.search_term, objetivo.store_category_fq
FROM scrape_targets objetivo
JOIN stores tienda ON tienda.id = objetivo.store_id
JOIN categories categoria ON categoria.id = objetivo.category_id
JOIN categories dominio ON dominio.id = categoria.parent_id
WHERE objetivo.active AND lower(f_unaccent(dominio.name)) = 'cuidado facial'
  AND lower(f_unaccent(objetivo.search_term)) IN
      ('hialuronico','ceramidas','retinol','niacinamida','glicolico')
ORDER BY tienda.name, objetivo.id;

-- 6. Función e índices previos: ddl-auto=validate no comprueba estas reglas.
SELECT to_regprocedure('public.f_unaccent(text)') AS funcion_requerida;
SELECT tablename, indexname, indexdef FROM pg_indexes
WHERE schemaname = 'public' AND tablename IN
    ('brands','products','scrape_targets','scrape_runs','store_product_links','store_sku_ean_map')
ORDER BY tablename, indexname;

-- RLS y permisos efectivos: ejecutar también con el rol real del backend.
SELECT relacion.relname AS tabla, relacion.relrowsecurity AS rls,
       has_table_privilege(current_user, relacion.oid, 'SELECT') AS puede_leer,
       has_table_privilege(current_user, relacion.oid, 'INSERT') AS puede_insertar,
       has_table_privilege(current_user, relacion.oid, 'UPDATE') AS puede_actualizar
FROM pg_class relacion JOIN pg_namespace esquema ON esquema.oid = relacion.relnamespace
WHERE esquema.nspname = 'public' AND relacion.relname IN
    ('scrape_runs','ean_size_overrides','store_product_links','store_sku_ean_map');
SELECT has_function_privilege(current_user, 'public.f_unaccent(text)', 'EXECUTE')
       AS puede_normalizar_texto;

-- 7. Los mapas se curan con IDs reales de la compartida; revisar motivos y unidades.
SELECT enlace.id, tienda.name AS tienda, enlace.store_sku, producto.name AS generico,
       producto.net_content, producto.unit_base, enlace.reason
FROM store_product_links enlace
JOIN stores tienda ON tienda.id = enlace.store_id
JOIN products producto ON producto.id = enlace.product_id;
SELECT equivalencia.id, tienda.name AS tienda, equivalencia.store_sku,
       equivalencia.ean, equivalencia.reason
FROM store_sku_ean_map equivalencia JOIN stores tienda ON tienda.id = equivalencia.store_id;

-- 8. Estado del staging: legado sin UUID se procesa individualmente.
SELECT oferta.status, oferta.run_id, corrida.completed_at, count(*) AS cantidad
FROM scraped_offers oferta LEFT JOIN scrape_runs corrida ON corrida.id = oferta.run_id
GROUP BY oferta.status, oferta.run_id, corrida.completed_at
ORDER BY oferta.status, corrida.completed_at DESC NULLS FIRST;

COMMIT;
