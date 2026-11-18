-- Solo lectura. Exportar a CSV y pasar a scrapers/buscar_urls.py --csv rebaja.csv.
-- Un target inactivo o con EAN distinto NO cuenta como cobertura.
-- SIN OFERTA tampoco se considera verificado: revisar/corregir el target existente antes
-- de insertar otra URL para ese EAN (la etiqueta término [EAN] sigue siendo única).
WITH rebaja AS (
    SELECT DISTINCT ON (lpad(o.ean, 14, '0')) lpad(o.ean, 14, '0') AS ean,
           o.name, o.brand_name, o.net_content, o.unit_base
    FROM scraped_offers o JOIN stores s ON s.id = o.store_id
    WHERE s.name ILIKE '%rebaja%' AND o.ean ~ '^([0-9]{8}|[0-9]{12,14})$'
      AND o.net_content > 0 AND o.unit_base IN ('G','ML','UN')
    ORDER BY lpad(o.ean, 14, '0'), o.scraped_at DESC, o.id DESC
), destinos AS (
    SELECT s.id, CASE WHEN s.name ILIKE '%farmatodo%' THEN 'farmatodo' ELSE 'farmanorte' END AS clave
    FROM stores s JOIN categories c ON c.id = s.category_id
    WHERE s.active AND c.name ILIKE 'Droguería'
      AND (s.name ILIKE '%farmatodo%' OR s.name ILIKE '%farmanorte%')
)
SELECT r.ean, r.name, r.brand_name, r.net_content, r.unit_base,
       string_agg(d.clave, ',' ORDER BY d.clave) AS falta_en
FROM rebaja r CROSS JOIN destinos d
WHERE NOT EXISTS (
    SELECT 1 FROM scrape_targets t
    JOIN LATERAL (SELECT * FROM scraped_offers o WHERE o.scrape_target_id = t.id
                  ORDER BY o.scraped_at DESC, o.id DESC LIMIT 1) o ON true
    WHERE t.store_id = d.id AND t.active AND t.target_url IS NOT NULL
      AND lpad(t.expected_ean, 14, '0') = r.ean
      AND o.ean ~ '^([0-9]{8}|[0-9]{12,14})$' AND lpad(o.ean, 14, '0') = r.ean
      AND o.net_content = r.net_content AND o.unit_base = r.unit_base
)
AND NOT EXISTS (
    SELECT 1 FROM products p JOIN prices pr ON pr.product_id = p.id
    WHERE p.ean = r.ean AND p.active AND pr.store_id = d.id
      AND pr.last_seen_at >= (now() AT TIME ZONE 'America/Bogota') - interval '7 days'
)
GROUP BY r.ean, r.name, r.brand_name, r.net_content, r.unit_base
ORDER BY r.ean;
-- Para exportar por páginas: LIMIT 100 OFFSET 0, luego OFFSET 100, etc.
