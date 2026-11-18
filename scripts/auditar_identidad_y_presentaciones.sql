-- Solo lectura. No corrige equivalencias, productos, precios ni estados.
BEGIN TRANSACTION READ ONLY;

-- 1. Ofertas incompatibles con equivalencias guardadas: comparar ambos lados.
SELECT oferta.id AS offer_id, tienda.name AS tienda,
       oferta.ean AS codigo_leido, oferta.store_sku AS sku_publicado,
       oferta.name AS nombre_leido, oferta.brand_name AS marca_leida,
       oferta.net_content AS contenido_leido, oferta.unit_base AS unidad_leida,
       objetivo.category_id AS categoria_objetivo,
       equivalencia.store_sku AS clave_equivalencia, equivalencia.ean AS ean_asignado,
       producto.name AS producto_asignado, marca.name AS marca_asignada,
       producto.net_content AS contenido_asignado, producto.unit_base AS unidad_asignada,
       producto.category_id AS categoria_asignada, producto.active AS producto_activo,
       oferta.reason, oferta.product_url
FROM scraped_offers oferta
JOIN stores tienda ON tienda.id = oferta.store_id
LEFT JOIN scrape_targets objetivo ON objetivo.id = oferta.scrape_target_id
LEFT JOIN store_sku_ean_map equivalencia
    ON equivalencia.store_id = oferta.store_id
   AND equivalencia.store_sku = CASE
       WHEN oferta.ean ~ '^[0-9]{1,7}$' THEN oferta.ean
       ELSE nullif(oferta.store_sku, '') END
LEFT JOIN products producto ON producto.ean = equivalencia.ean
LEFT JOIN brands marca ON marca.id = producto.brand_id
WHERE oferta.status = 'UNCLASSIFIED'
  AND oferta.reason LIKE 'identidad secundaria: equivalencia guardada incompatible%'
ORDER BY oferta.processed_at DESC, oferta.id DESC;

-- 2. Diagnóstico de candidatos por atributos, ANTES de checksum y similitud Java.
-- Una fila con candidatos no demuestra equivalencia física ni autoriza un mapeo.
SELECT oferta.id AS offer_id, tienda.name AS tienda, oferta.ean,
       oferta.name, oferta.brand_name, oferta.net_content, oferta.unit_base,
       objetivo.category_id, candidatos.cantidad AS candidatos_por_atributos,
       oferta.reason
FROM scraped_offers oferta
JOIN stores tienda ON tienda.id = oferta.store_id
JOIN scrape_targets objetivo ON objetivo.id = oferta.scrape_target_id
JOIN LATERAL (
    SELECT count(*) AS cantidad
    FROM products producto JOIN brands marca ON marca.id = producto.brand_id
    WHERE producto.active AND producto.ean IS NOT NULL
      AND producto.category_id = objetivo.category_id
      AND producto.net_content = oferta.net_content
      AND producto.unit_base = upper(btrim(oferta.unit_base))
      AND lower(f_unaccent(marca.name)) = lower(f_unaccent(btrim(oferta.brand_name)))
) candidatos ON true
WHERE oferta.status = 'UNCLASSIFIED'
  AND oferta.reason LIKE 'identidad secundaria:%'
ORDER BY oferta.processed_at DESC, oferta.id DESC
LIMIT 100;

-- 3. Discrepancias publicadas para los dos EAN investigados; preservar el crudo.
SELECT oferta.id, tienda.name AS tienda, oferta.run_id, oferta.ean,
       oferta.name, oferta.net_content, oferta.unit_base, oferta.price,
       oferta.status, oferta.reason, oferta.product_url, oferta.scraped_at
FROM scraped_offers oferta JOIN stores tienda ON tienda.id = oferta.store_id
WHERE oferta.ean ~ '^([0-9]{8}|[0-9]{12,14})$'
  AND lpad(oferta.ean, 14, '0') IN ('03282771111216', '03337875743563')
ORDER BY lpad(oferta.ean, 14, '0'), tienda.name, oferta.scraped_at DESC;

-- 4. Maestros y precios ya existentes: el parche NO reescribe estas filas.
SELECT producto.id AS product_id, producto.ean, producto.name,
       producto.net_content, producto.unit_base, tienda.name AS tienda,
       precio.id AS price_id, precio.value, precio.registration_date, precio.last_seen_at
FROM products producto
LEFT JOIN prices precio ON precio.product_id = producto.id
LEFT JOIN stores tienda ON tienda.id = precio.store_id
WHERE producto.ean IN ('03282771111216', '03337875743563')
ORDER BY producto.ean, precio.id DESC;

-- 5. Metadatos del staging pendiente: NULL es legado y no se deduplica como corrida.
SELECT oferta.run_id, corrida.completed_at, count(*) AS pendientes
FROM scraped_offers oferta LEFT JOIN scrape_runs corrida ON corrida.id = oferta.run_id
WHERE oferta.status = 'PENDING'
GROUP BY oferta.run_id, corrida.completed_at
ORDER BY corrida.completed_at DESC NULLS FIRST;

COMMIT;
