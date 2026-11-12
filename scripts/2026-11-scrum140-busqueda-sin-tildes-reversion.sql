-- SCRUM-140 · Reversión de 2026-11-scrum140-busqueda-sin-tildes.sql: borra la función f_unaccent.
-- Antes de ejecutarla, desplegar un backend sin SCRUM-140: ProductSpecifications llama a f_unaccent y la búsqueda por nombre responde 500 sin ella.
-- DROP FUNCTION se lleva sus permisos: no hay que devolver ningún GRANT, y anon y authenticated siguen sin acceso a nada nuevo.
-- La extensión unaccent NO se borra: pudo existir antes del script o usarla otra cosa. Si se instaló solo para SCRUM-140 y nada
-- depende de ella, se borra a mano con DROP EXTENSION IF EXISTS unaccent; (RESTRICT por defecto: falla si algo depende de ella).
-- DROP FUNCTION IF EXISTS no da error si la función ya no está, así que la reversión se puede volver a correr.
BEGIN;

DROP FUNCTION IF EXISTS public.f_unaccent(text);

COMMIT;

-- Sincronización (ejecutar después).
-- Esperado: 0 filas (la función ya no existe)
-- SELECT proname FROM pg_proc WHERE proname = 'f_unaccent' AND pronamespace = 'public'::regnamespace;
-- Esperado: unaccent sigue instalada si existía antes
-- SELECT extname FROM pg_extension WHERE extname = 'unaccent';
