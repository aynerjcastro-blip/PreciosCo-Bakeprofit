-- Reversión de 2026-11-ht10-prices-last-seen.sql (HT-10, SCRUM-166)
-- Antes de ejecutarlo, desplegar un backend sin Price.lastSeenAt: con ddl-auto=validate,
-- la versión de HT-10 no arranca sin la columna.
-- Se pierde cuándo se vio por última vez cada precio; los precios en sí no cambian.
BEGIN;

ALTER TABLE prices DROP COLUMN last_seen_at;

COMMIT;