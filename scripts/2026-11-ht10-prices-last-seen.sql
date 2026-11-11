-- HT-10 · Procesador de staging (SCRUM-166): historial de precios por periodos
-- Cada fila de prices es un periodo con el mismo precio: registration_date es cuándo empezó
-- y last_seen_at cuándo se vio por última vez. Un precio igual al último solo refresca
-- last_seen_at y los enlaces; un precio distinto abre una fila nueva.
-- Reversión: 2026-11-ht10-prices-last-seen-rollback.sql
-- Debe quedar sincronizado con Price.lastSeenAt: la aplicación arranca con ddl-auto=validate.
BEGIN;

-- 1. Columna nula primero: con DEFAULT now() las filas existentes quedarían con la fecha
--    de este script y parecerían vistas hoy
ALTER TABLE prices ADD COLUMN last_seen_at TIMESTAMP;

-- 2. Un precio existente se vio por última vez cuando se registró
UPDATE prices SET last_seen_at = registration_date;

-- 3. El DEFAULT cubre cualquier INSERT que todavía no conozca la columna (el scraper anterior)
ALTER TABLE prices ALTER COLUMN last_seen_at SET DEFAULT now();
ALTER TABLE prices ALTER COLUMN last_seen_at SET NOT NULL;

-- prices ya tiene RLS activado (scripts/2026-11-seguridad-api-supabase.sql); una columna nueva no lo cambia

COMMIT;

-- Sincronización (ejecutar después; ddl-auto=validate no revisa nulabilidad, default ni longitudes).
-- Esperado: prices   | last_seen_at | timestamp without time zone | NO | now() | (null)
--           prices   | source       | character varying           | YES | (null) | 255  ← Price.SOURCE_MAX_LENGTH
--           products | name         | character varying           | NO | (null) | 255  ← Product.NAME_MAX_LENGTH
-- y relrowsecurity = true.
-- SELECT table_name, column_name, data_type, is_nullable, column_default, character_maximum_length
-- FROM information_schema.columns
-- WHERE (table_name = 'prices' AND column_name IN ('last_seen_at', 'source'))
--    OR (table_name = 'products' AND column_name = 'name')
-- ORDER BY table_name, column_name;
-- SELECT relrowsecurity FROM pg_class WHERE relname = 'prices';