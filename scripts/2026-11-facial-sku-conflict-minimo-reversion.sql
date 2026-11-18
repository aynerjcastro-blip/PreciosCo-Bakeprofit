-- Exportar la revisión y detener procesos antes de restaurar código anterior.
-- No elimina la protección de ofertas que todavía necesitan revisión.
BEGIN;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM scraped_offers WHERE status = 'CONFLICT' OR has_ean = false) THEN
        RAISE EXCEPTION 'Hay CONFLICT o identidades SKU nuevas: resolver/exportar antes de revertir';
    END IF;
END $$;
ALTER TABLE scraped_offers DROP CONSTRAINT chk_scraped_offers_status;
ALTER TABLE scraped_offers ADD CONSTRAINT chk_scraped_offers_status
    CHECK (status IN ('PENDING', 'PROCESSED', 'UNCLASSIFIED'));
ALTER TABLE scraped_offers DROP COLUMN has_ean;
COMMIT;
