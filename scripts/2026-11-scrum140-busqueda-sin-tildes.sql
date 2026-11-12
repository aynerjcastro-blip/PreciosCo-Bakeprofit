-- SCRUM-140 · Búsqueda sin tildes ni mayúsculas en SQL: extensión unaccent y función f_unaccent (criterio de SCRUM-304).
-- La búsqueda paginada filtra el nombre con lower(f_unaccent(...)) a los dos lados de la comparación; sin esta función responde 500.
-- f_unaccent queda fuera de la API REST de Supabase (/rpc), como las tablas en SCRUM-297: ni PUBLIC, ni anon, ni authenticated la ejecutan.
-- El backend se conecta como postgres: la función queda a su nombre, y el dueño la ejecuta sin GRANT.
-- CREATE OR REPLACE, ALTER OWNER y REVOKE se pueden repetir sin error, así que el script se puede volver a correr. Reversión: 2026-11-scrum140-busqueda-sin-tildes-reversion.sql
BEGIN;

-- 1. La extensión va en el esquema extensions, como las demás de Supabase. En un PostgreSQL
--    local ese esquema no existe y se crea aquí; en Supabase ya existe y no cambia.
CREATE SCHEMA IF NOT EXISTS extensions;
CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA extensions;

-- 2. Si unaccent ya estaba instalada en otro esquema, el IF NOT EXISTS no la mueve y f_unaccent
--    no la encontraría: se detiene aquí con un mensaje claro en lugar de fallar más adelante.
DO $$
DECLARE
    unaccent_schema name;
BEGIN
    SELECT n.nspname INTO unaccent_schema
    FROM pg_extension e
    JOIN pg_namespace n ON n.oid = e.extnamespace
    WHERE e.extname = 'unaccent';

    IF unaccent_schema <> 'extensions' THEN
        RAISE EXCEPTION 'unaccent está instalada en el esquema %, no en extensions. Avisa al equipo antes de seguir: f_unaccent la busca en extensions.', unaccent_schema;
    END IF;
END
$$;

-- 3. unaccent() es STABLE porque busca su diccionario en el search_path. El envoltorio califica
--    la función y el diccionario con su esquema: no depende del search_path de quien la llame y
--    se puede declarar IMMUTABLE (permite un índice funcional si el catálogo crece).
CREATE OR REPLACE FUNCTION public.f_unaccent(text)
    RETURNS text
    LANGUAGE sql
    IMMUTABLE PARALLEL SAFE STRICT
AS $$
    SELECT extensions.unaccent('extensions.unaccent'::regdictionary, $1)
$$;

-- 4. Dueño explícito: si el script lo corre otro rol (por ejemplo, la CLI de Supabase), postgres
--    sigue siendo el dueño y el backend la sigue ejecutando sin un GRANT.
ALTER FUNCTION public.f_unaccent(text) OWNER TO postgres;

-- 5. Supabase publica por /rpc las funciones de public, y sus privilegios por defecto dan EXECUTE
--    a PUBLIC, anon y authenticated. Se quitan los tres; al dueño (postgres) no lo afecta.
--    En un PostgreSQL local los roles anon y authenticated no existen: ahí solo se revoca PUBLIC.
REVOKE EXECUTE ON FUNCTION public.f_unaccent(text) FROM PUBLIC;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon')
       AND EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE EXECUTE ON FUNCTION public.f_unaccent(text) FROM anon, authenticated;
    ELSE
        RAISE NOTICE 'Sin roles anon y authenticated (PostgreSQL local): solo se revocó PUBLIC.';
    END IF;
END
$$;

COMMIT;

-- Sincronización (ejecutar después).
--
-- Esperado: unaccent | extensions
-- SELECT e.extname, n.nspname AS schema
-- FROM pg_extension e JOIN pg_namespace n ON n.oid = e.extnamespace
-- WHERE e.extname = 'unaccent';
--
-- Esperado: cafe con leche | arroz nono
-- (se escriben con códigos Unicode para no depender de la página de códigos de la consola)
-- SELECT lower(f_unaccent(U&'Caf\00E9 con LECHE')), lower(f_unaccent(U&'Arroz \00D1o\00F1o'));
--
-- Esperado: postgres | true   (el dueño, que es como se conecta el backend, la ejecuta)
-- SELECT pg_get_userbyid(proowner) AS owner,
--        has_function_privilege(pg_get_userbyid(proowner), 'public.f_unaccent(text)', 'EXECUTE') AS owner_execute
-- FROM pg_proc WHERE oid = 'public.f_unaccent(text)'::regprocedure;
--
-- Esperado (solo en Supabase; en un PostgreSQL local no existen anon ni authenticated): false | false | false
-- SELECT has_function_privilege('anon', 'public.f_unaccent(text)', 'EXECUTE') AS anon,
--        has_function_privilege('authenticated', 'public.f_unaccent(text)', 'EXECUTE') AS authenticated,
--        has_function_privilege('public', 'public.f_unaccent(text)', 'EXECUTE') AS public;
