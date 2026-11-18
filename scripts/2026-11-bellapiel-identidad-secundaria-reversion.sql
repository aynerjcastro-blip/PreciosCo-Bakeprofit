-- Detener procesador, exportar equivalencias y restaurar backend anterior antes de revertir.
BEGIN;
DROP TABLE store_sku_ean_map;
COMMIT;
