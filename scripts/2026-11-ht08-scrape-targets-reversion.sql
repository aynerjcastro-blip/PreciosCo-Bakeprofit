-- Reversión de HT-08 (SCRUM-164): borra las filas de scrape_targets cargadas por
-- 2026-11-ht08-scrape-targets.sql (las 4 tiendas del scraper).
-- Si alguna fila ya tiene ofertas en scraped_offers, la FK fk_scraped_offers_scrape_targets
-- hace fallar el borrado: en ese caso hay que borrar o reasignar esas ofertas primero.
SET client_encoding TO 'UTF8';
BEGIN;

DELETE FROM scrape_targets
WHERE store_id IN (SELECT id FROM stores WHERE name IN ('D1', 'Éxito', 'Jumbo', 'Olímpica'));

COMMIT;
