-- Aplicar ANTES de desplegar scraper/backend. No modifica el catálogo existente.
BEGIN;
    ALTER TABLE scrape_targets ADD COLUMN IF NOT EXISTS target_url TEXT;
    ALTER TABLE scrape_targets ADD COLUMN expected_ean VARCHAR(14);
    ALTER TABLE scrape_targets ADD CONSTRAINT chk_targets_expected_ean
        CHECK (expected_ean IS NULL OR expected_ean ~ '^([0-9]{8}|[0-9]{12,14})$');
    -- Solo etiquetas inequívocas de objetivos de página; Java valida también el checksum.
    UPDATE scrape_targets SET expected_ean = substring(search_term FROM '\[([0-9]{8}|[0-9]{12,14})\]')
    WHERE target_url IS NOT NULL;
    
    CREATE TABLE scrape_runs (
        id UUID PRIMARY KEY,
        started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
        completed_at TIMESTAMPTZ
    );

    ALTER TABLE scrape_runs ENABLE ROW LEVEL SECURITY;
    ALTER TABLE scraped_offers ADD COLUMN run_id UUID REFERENCES scrape_runs(id),
        ADD COLUMN store_sku VARCHAR(100), ADD COLUMN sale_weight_kg NUMERIC(12,6);

    CREATE INDEX idx_offers_run_store ON scraped_offers(run_id, store_id);
    CREATE INDEX idx_offers_pending_gtin ON scraped_offers ((lpad(ean, 14, '0')))
        WHERE status = 'PENDING' AND ean ~ '^([0-9]{8}|[0-9]{12,14})$';

    CREATE TABLE ean_size_overrides (
        ean VARCHAR(14) PRIMARY KEY CHECK (ean ~ '^[0-9]{14}$'),
        net_content NUMERIC(10,2) NOT NULL CHECK (net_content > 0),
        unit_base VARCHAR(2) NOT NULL CHECK (unit_base IN ('G','ML','UN')),
        reason TEXT NOT NULL CHECK (length(trim(reason)) > 0)
    );

    ALTER TABLE ean_size_overrides ENABLE ROW LEVEL SECURITY;

    CREATE TABLE store_product_links (
        id BIGSERIAL PRIMARY KEY,
        store_id BIGINT NOT NULL REFERENCES stores(id),
        store_sku VARCHAR(100) NOT NULL CHECK (length(trim(store_sku)) > 0),
        product_id BIGINT NOT NULL REFERENCES products(id),
        reason TEXT NOT NULL CHECK (length(trim(reason)) > 0),
        UNIQUE (store_id, store_sku)
    );

    ALTER TABLE store_product_links ENABLE ROW LEVEL SECURITY;
    
    CREATE INDEX idx_links_store_product ON store_product_links(store_id, product_id);
    
    REVOKE ALL ON scrape_runs, ean_size_overrides, store_product_links FROM PUBLIC;
    REVOKE ALL ON SEQUENCE store_product_links_id_seq FROM PUBLIC;

    DO $$
    DECLARE role_name text;
    BEGIN
        FOREACH role_name IN ARRAY ARRAY['anon', 'authenticated'] LOOP
            IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = role_name) THEN
                EXECUTE format('REVOKE ALL ON scrape_runs, ean_size_overrides, store_product_links FROM %I', role_name);
                EXECUTE format('REVOKE ALL ON SEQUENCE store_product_links_id_seq FROM %I', role_name);
            END IF;
        END LOOP;
    END $$;

-- Los enlaces son manuales. El backend exige producto sin EAN/referencia, 1000 G,
-- categoría hoja activa y peso publicado por SKU. No se crean equivalencias por nombre.
COMMIT;
-- Sincronización: comprobar columnas, PK/FK/CHECK y RLS antes de habilitar STAGING_ENABLED.
SELECT table_name, column_name, data_type FROM information_schema.columns
WHERE table_name IN ('scrape_runs','ean_size_overrides','store_product_links')
   OR (table_name = 'scrape_targets' AND column_name = 'expected_ean')
   OR (table_name = 'scraped_offers' AND column_name IN ('run_id','store_sku','sale_weight_kg'));
SELECT relname, relrowsecurity FROM pg_class
WHERE oid IN ('scrape_runs'::regclass,'ean_size_overrides'::regclass,'store_product_links'::regclass);
