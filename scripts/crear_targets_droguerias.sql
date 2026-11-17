BEGIN;

WITH configuracion AS (
    SELECT
        10::BIGINT AS store_id_origen,
        100 AS longitud_maxima_termino
),
candidatas(store_id, ean, url) AS (
    VALUES
        (
            11::BIGINT,
            '7702057100377',
            'https://www.farmatodo.com.co/producto/1061634-clotrimazol-mk-2-crema-vaginal-tubo-x-20-gr'
        )
        -- Añade aquí las demás filas:
        -- , (ID_TIENDA, 'EAN', 'URL')
),
normalizadas AS (
    SELECT DISTINCT
        store_id,
        lpad(btrim(ean), 14, '0') AS ean,
        btrim(url) AS url
    FROM candidatas
    WHERE btrim(ean) ~ '^([0-9]{8}|[0-9]{12,14})$'
      AND btrim(url) <> ''
)
INSERT INTO scrape_targets (
    store_id,
    category_id,
    search_term,
    target_url,
    expected_ean,
    unit_base,
    min_content,
    max_content,
    active
)
SELECT
    candidata.store_id,
    origen.category_id,
    left(
        origen.search_term,
        configuracion.longitud_maxima_termino
            - length(' [' || candidata.ean || ']')
    ) || ' [' || candidata.ean || ']',
    candidata.url,
    candidata.ean,
    referencia.unit_base,
    referencia.net_content,
    referencia.net_content,
    true
FROM normalizadas candidata
CROSS JOIN configuracion
JOIN LATERAL (
    SELECT
        oferta.scrape_target_id,
        oferta.unit_base,
        oferta.net_content
    FROM scraped_offers oferta
    WHERE oferta.store_id = configuracion.store_id_origen
      AND oferta.ean ~ '^([0-9]{8}|[0-9]{12,14})$'
      AND lpad(oferta.ean, 14, '0') = candidata.ean
      AND oferta.unit_base IN ('G', 'ML', 'UN')
      AND oferta.net_content > 0
      AND (
          oferta.run_id IS NULL
          OR EXISTS (
              SELECT 1
              FROM scrape_runs corrida
              WHERE corrida.id = oferta.run_id
                AND corrida.completed_at IS NOT NULL
          )
      )
    ORDER BY oferta.scraped_at DESC, oferta.id DESC
    LIMIT 1
) referencia ON true
JOIN scrape_targets origen
    ON origen.id = referencia.scrape_target_id
WHERE NOT EXISTS (
    SELECT 1
    FROM scrape_targets existente
    WHERE existente.store_id = candidata.store_id
      AND (
          existente.target_url = candidata.url
          OR lpad(existente.expected_ean, 14, '0') = candidata.ean
      )
)
ON CONFLICT DO NOTHING
RETURNING
    id,
    store_id,
    search_term,
    target_url,
    expected_ean,
    unit_base,
    min_content,
    max_content,
    active;

COMMIT;