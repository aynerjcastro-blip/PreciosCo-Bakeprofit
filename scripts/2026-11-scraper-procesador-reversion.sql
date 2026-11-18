-- Detener scraper y procesador, restaurar código anterior y exportar curaduría antes de revertir.
BEGIN;
    DROP TABLE store_product_links;
    DROP TABLE ean_size_overrides;
    DROP INDEX idx_offers_pending_gtin;
    ALTER TABLE scraped_offers DROP COLUMN run_id, DROP COLUMN store_sku, DROP COLUMN sale_weight_kg;
    DROP TABLE scrape_runs;
    ALTER TABLE scrape_targets DROP COLUMN expected_ean;
    -- target_url se conserva: puede existir desde el despliegue de droguerías.
COMMIT;
