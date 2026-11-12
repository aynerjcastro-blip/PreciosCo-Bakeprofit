-- Reversión de HT-09 (SCRUM-165): deshace 2026-11-ht09-modelo-identificacion.sql en orden inverso.
-- ATENCIÓN: borra las marcas, los objetivos de scraping, el staging y la identidad de los productos.
BEGIN;

-- 6 y 5. Tablas de staging (sus índices, FK y CHECK caen con ellas)
DROP TABLE IF EXISTS scraped_offers;
DROP TABLE IF EXISTS scrape_targets;

-- 4. Índices
DROP INDEX IF EXISTS idx_prices_product_store;
DROP INDEX IF EXISTS idx_products_category;
DROP INDEX IF EXISTS uq_products_brand_ref;
DROP INDEX IF EXISTS uq_products_ean;

-- 3. Reglas de identidad
ALTER TABLE products
    DROP CONSTRAINT IF EXISTS chk_products_ref_brand,
    DROP CONSTRAINT IF EXISTS chk_products_net_content,
    DROP CONSTRAINT IF EXISTS chk_products_content_pair,
    DROP CONSTRAINT IF EXISTS chk_products_unit_base,
    DROP CONSTRAINT IF EXISTS chk_products_ean;

-- 2. Columnas de identidad (la generada primero, porque depende de manufacturer_ref)
ALTER TABLE products DROP CONSTRAINT IF EXISTS fk_products_brands;
ALTER TABLE products DROP COLUMN IF EXISTS manufacturer_ref_norm;
ALTER TABLE products
    DROP COLUMN IF EXISTS manufacturer_ref,
    DROP COLUMN IF EXISTS unit_base,
    DROP COLUMN IF EXISTS net_content,
    DROP COLUMN IF EXISTS ean,
    DROP COLUMN IF EXISTS brand_id;

-- 1. Marcas (uq_brands_name cae con la tabla)
DROP TABLE IF EXISTS brands;

COMMIT;