-- Requiere 2026-11-scraper-procesador.sql y f_unaccent de SCRUM-140.
-- Aplicar antes del backend actualizado. No inserta equivalencias inventadas.
BEGIN;
CREATE TABLE store_sku_ean_map (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES stores(id),
    store_sku VARCHAR(100) NOT NULL CHECK (length(trim(store_sku)) > 0),
    ean VARCHAR(14) NOT NULL CHECK (ean ~ '^[0-9]{14}$'),
    reason TEXT NOT NULL CHECK (length(trim(reason)) > 0),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_store_sku_ean_map UNIQUE (store_id, store_sku)
);
ALTER TABLE store_sku_ean_map ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON store_sku_ean_map FROM PUBLIC;
REVOKE ALL ON SEQUENCE store_sku_ean_map_id_seq FROM PUBLIC;
DO $$
DECLARE role_name text;
BEGIN
    FOREACH role_name IN ARRAY ARRAY['anon', 'authenticated'] LOOP
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = role_name) THEN
            EXECUTE format('REVOKE ALL ON store_sku_ean_map FROM %I', role_name);
            EXECUTE format('REVOKE ALL ON SEQUENCE store_sku_ean_map_id_seq FROM %I', role_name);
        END IF;
    END LOOP;
END $$;
COMMIT;
SELECT relname, relrowsecurity FROM pg_class WHERE oid = 'store_sku_ean_map'::regclass;
SELECT indexdef FROM pg_indexes WHERE tablename = 'store_sku_ean_map';
SELECT column_name, data_type, is_nullable FROM information_schema.columns
WHERE table_name = 'store_sku_ean_map' ORDER BY ordinal_position;
