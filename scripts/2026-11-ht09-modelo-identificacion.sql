-- HT-09 · Modelo de identificación de productos (SCRUM-165)
-- Los productos comunes se identifican por EAN; los repuestos, por marca + referencia del fabricante.
-- Crea además las tablas del procesador de staging (HT-10).
-- Debe quedar sincronizado con las entidades: la aplicación arranca con ddl-auto=validate.
BEGIN;

-- 1. Marcas
CREATE TABLE brands (
    id                BIGSERIAL    PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    registration_date TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_brands_name ON brands (lower(name));

-- 2. Identidad del producto (todas opcionales)
ALTER TABLE products
    ADD COLUMN brand_id         BIGINT,
    ADD COLUMN ean              VARCHAR(14),
    ADD COLUMN net_content      NUMERIC(10,2),
    ADD COLUMN unit_base        VARCHAR(2),
    ADD COLUMN manufacturer_ref VARCHAR(60);

-- Solo letras y números ASCII, en mayúsculas. Debe replicar exactamente ManufacturerReference.normalize()
ALTER TABLE products
    ADD COLUMN manufacturer_ref_norm VARCHAR(60)
        GENERATED ALWAYS AS (upper(regexp_replace(manufacturer_ref, '[^A-Za-z0-9]', '', 'g'))) STORED;

ALTER TABLE products
    ADD CONSTRAINT fk_products_brands FOREIGN KEY (brand_id) REFERENCES brands (id);

-- 3. Reglas de identidad. El EAN se guarda como GTIN-14 (Gtin.normalize rellena con ceros);
--    el dígito verificador lo valida Java
ALTER TABLE products
    ADD CONSTRAINT chk_products_ean
        CHECK (ean IS NULL OR ean ~ '^[0-9]{14}$'),
    ADD CONSTRAINT chk_products_unit_base
        CHECK (unit_base IS NULL OR unit_base IN ('G', 'ML', 'UN')),
    ADD CONSTRAINT chk_products_content_pair
        CHECK ((net_content IS NULL) = (unit_base IS NULL)),
    ADD CONSTRAINT chk_products_net_content
        CHECK (net_content IS NULL OR net_content > 0),
    ADD CONSTRAINT chk_products_ref_brand
        CHECK (manufacturer_ref IS NULL OR brand_id IS NOT NULL);

-- 4. Índices
CREATE UNIQUE INDEX uq_products_ean
    ON products (ean) WHERE ean IS NOT NULL;

CREATE UNIQUE INDEX uq_products_brand_ref
    ON products (brand_id, manufacturer_ref_norm) WHERE manufacturer_ref IS NOT NULL;

CREATE INDEX idx_products_category ON products (category_id);

CREATE INDEX idx_prices_product_store ON prices (product_id, store_id);

-- 5. Qué buscar en cada tienda (reemplaza el diccionario PRODUCTOS del scraper)
CREATE TABLE scrape_targets (
    id                BIGSERIAL     PRIMARY KEY,
    store_id          BIGINT        NOT NULL,
    category_id       BIGINT        NOT NULL,
    search_term       VARCHAR(100)  NOT NULL,
    store_category_fq VARCHAR(200),
    unit_base         VARCHAR(2)    NOT NULL,
    min_content       NUMERIC(10,2),
    max_content       NUMERIC(10,2),
    active            BOOLEAN       NOT NULL DEFAULT true,
    CONSTRAINT fk_scrape_targets_stores
        FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_scrape_targets_categories
        FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT chk_scrape_targets_unit_base
        CHECK (unit_base IN ('G', 'ML', 'UN')),
    CONSTRAINT chk_scrape_targets_range
        CHECK (min_content IS NULL OR max_content IS NULL OR min_content <= max_content)
);

-- 6. Staging crudo del scraper: EAN tal como llega (sin normalizar ni CHECK) y unidad sin CHECK;
--    validar y normalizar es trabajo del procesador
CREATE TABLE scraped_offers (
    id               BIGSERIAL     PRIMARY KEY,
    store_id         BIGINT        NOT NULL,
    scrape_target_id BIGINT,
    ean              VARCHAR(20),
    name             TEXT          NOT NULL,
    brand_name       VARCHAR(100),
    price            NUMERIC(12,2) NOT NULL,
    list_price       NUMERIC(12,2),
    product_url      TEXT,
    image_url        TEXT,
    net_content      NUMERIC(10,2),
    unit_base        VARCHAR(2),
    scraped_at       TIMESTAMP     NOT NULL DEFAULT now(),
    status           VARCHAR(15)   NOT NULL DEFAULT 'PENDING',
    reason           TEXT,
    processed_at     TIMESTAMP,
    CONSTRAINT fk_scraped_offers_stores
        FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_scraped_offers_scrape_targets
        FOREIGN KEY (scrape_target_id) REFERENCES scrape_targets (id),
    CONSTRAINT chk_scraped_offers_status
        CHECK (status IN ('PENDING', 'PROCESSED', 'UNCLASSIFIED'))
);

CREATE INDEX idx_scraped_offers_status ON scraped_offers (status);

COMMIT;