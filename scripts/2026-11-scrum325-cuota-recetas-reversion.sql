-- SCRUM-325 · Reversión de 2026-11-scrum325-cuota-recetas.sql: borra el contador de recetas.
-- Antes de ejecutarla, desplegar un backend sin SCRUM-325: User mapea ai_recipes_used y, con
-- ddl-auto=validate, Hibernate no arranca sin la columna.
-- DROP COLUMN se lleva también la restricción chk_users_ai_recipes_used.
BEGIN;

ALTER TABLE public.users
    DROP COLUMN IF EXISTS ai_recipes_used;

COMMIT;

-- Sincronización (ejecutar después).
-- Esperado: 0 filas
-- SELECT column_name FROM information_schema.columns
-- WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'ai_recipes_used';
