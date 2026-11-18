-- Una aplicación después de las migraciones anteriores del procesador.
-- Detener scrapers/procesador antes. No amplía ean ni cambia registros existentes.
BEGIN;
ALTER TABLE scraped_offers ADD COLUMN has_ean BOOLEAN;
-- La base puede usar el nombre del script original o el generado por Hibernate.
ALTER TABLE scraped_offers DROP CONSTRAINT IF EXISTS chk_scraped_offers_status;
ALTER TABLE scraped_offers DROP CONSTRAINT IF EXISTS scraped_offers_status_check;
ALTER TABLE scraped_offers ADD CONSTRAINT chk_scraped_offers_status
    CHECK (status IN ('PENDING', 'PROCESSED', 'UNCLASSIFIED', 'CONFLICT'));
COMMIT;
