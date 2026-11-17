-- Solo lectura. Requiere 2026-11-scraper-procesador.sql.
-- La validación definitiva de checksum y comparabilidad sigue en Gtin/OfferScreening.
SELECT t.id AS target_id, s.name AS tienda, t.active, t.target_url, t.expected_ean,
       o.id AS offer_id, o.ean AS ean_leido, o.price, o.net_content, o.unit_base,
       o.status, o.reason,
       CASE WHEN t.expected_ean IS NULL THEN 'SIN EAN ESPERADO'
            WHEN o.id IS NULL THEN 'SIN OFERTA'
            WHEN o.ean IS NULL THEN 'LA PAGINA NO TRAE EAN'
            WHEN o.ean !~ '^([0-9]{8}|[0-9]{12,14})$' THEN 'EAN INVALIDO'
            WHEN lpad(o.ean, 14, '0') = lpad(t.expected_ean, 14, '0') THEN 'IGUAL'
            ELSE 'DISTINTO' END AS resultado
FROM scrape_targets t
JOIN stores s ON s.id = t.store_id
LEFT JOIN LATERAL (
    SELECT * FROM scraped_offers x WHERE x.scrape_target_id = t.id
    ORDER BY x.scraped_at DESC, x.id DESC LIMIT 1
) o ON true
WHERE t.target_url IS NOT NULL
ORDER BY s.name, t.id;

-- IGUAL:
-- Coinciden el EAN leído y el esperado después de normalizarlos.
-- Conservar el objetivo si la ficha y la presentación son correctas.
-- Revisar también status y reason: esta comparación SQL no valida
-- el dígito verificador ni garantiza que el procesador acepte la oferta.

-- DISTINTO / EAN INVALIDO / LA PAGINA NO TRAE EAN:
-- La identidad no está verificada. Revisar la URL y la extracción.
-- Corregir el objetivo o desactivarlo mientras se revisa:
-- UPDATE scrape_targets SET active = false WHERE id IN (...);
-- No sustituir el EAN leído por expected_ean para forzar una coincidencia.

-- SIN OFERTA:
-- Revisar si el target se ejecutó, los logs, el stock, el precio y la URL.
-- La ausencia de oferta no demuestra que el EAN sea incorrecto.
-- Desactivar si se confirma que el objetivo no sirve.

-- SIN EAN ESPERADO:
-- Completar expected_ean con una referencia verificada.
-- Escribir el EAN únicamente en search_term no configura la validación.

-- IGUAL PERO SIN CONTENIDO O UNIDAD:
-- El procesador puede completar los datos faltantes usando el target
-- cuando el EAN es comparable, coincide con expected_ean,
-- min_content = max_content > 0 y los datos presentes son compatibles.
-- El respaldo no modifica scraped_offers ni reemplaza un tamaño
-- publicado distinto por el tamaño del objetivo.

-- FARMANORTE:
-- La corrección para presentaciones como "50 GRX" ya está incluida.
-- Comprobar nuevamente los resultados; no asumir que siguen siendo cinco.
-- Las presentaciones en UN requieren confirmar la cantidad en la ficha.

-- La identidad secundaria configurada para Bella Piel no se aplica
-- por defecto a Farmatodo ni a Farmanorte.