-- SCRUM-325 · Cuota mensual de recetas del asistente de IA.
-- Las recetas tienen su propio contador, separado de ai_queries_used: un usuario FREE puede pedir
-- N recetas al mes (app.ai-quota.free-monthly-recipe-limit, 10 por defecto) sin gastar sus consultas.
-- Se reinicia junto con ai_queries_used (mismo ai_queries_reset_at, ver UserRepository.resetDueAiQuotas).
-- Requiere HT-23 aplicado (2026-11-ht23-planes-suscripciones.sql).
-- Ejecutar ANTES de desplegar el backend con SCRUM-325: con ddl-auto=validate, Hibernate no arranca
-- si la columna no existe.
BEGIN;

ALTER TABLE public.users
    ADD COLUMN ai_recipes_used INT NOT NULL DEFAULT 0
        CONSTRAINT chk_users_ai_recipes_used CHECK (ai_recipes_used >= 0);

COMMIT;

-- Sincronización (ejecutar después).
-- Esperado: 1 fila → ai_recipes_used | integer | NO | 0
-- SELECT column_name, data_type, is_nullable, column_default
-- FROM information_schema.columns
-- WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'ai_recipes_used';
-- Esperado: 1 fila → CHECK ((ai_recipes_used >= 0))
-- SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'chk_users_ai_recipes_used';
