-- Marcas sin tildes: "Alquería" y "ALQUERIA" son la misma marca
-- uq_brands_name pasa de lower(name) a lower(f_unaccent(name)), la misma regla de la búsqueda
-- (SCRUM-140): primero se quitan las tildes y después se pasa a minúsculas. BrandRepository usa
-- la misma expresión en el ON CONFLICT y en la búsqueda; si no coinciden, el INSERT no infiere el índice.
-- Requiere 2026-11-scrum140-busqueda-sin-tildes.sql (crea public.f_unaccent, que es IMMUTABLE y
-- por eso sirve en un índice).
-- Si ya hay dos marcas que solo se diferencian por las tildes, se detiene y las lista: hay que
-- fusionarlas a mano (mover products.brand_id a una y borrar la otra) y volver a correrlo.
-- Reversión: 2026-11-marcas-sin-tildes-reversion.sql
SET client_encoding TO 'UTF8';
BEGIN;

DO $$
DECLARE
    v_duplicates text;
BEGIN
    IF to_regprocedure('public.f_unaccent(text)') IS NULL THEN
        RAISE EXCEPTION 'Falta public.f_unaccent: correr antes 2026-11-scrum140-busqueda-sin-tildes.sql';
    END IF;

    SELECT string_agg(names, '; ') INTO v_duplicates
    FROM (
        SELECT string_agg(format('%s (id %s)', name, id), ', ' ORDER BY id) AS names
        FROM brands
        GROUP BY lower(public.f_unaccent(name))
        HAVING count(*) > 1
    ) d;

    IF v_duplicates IS NOT NULL THEN
        RAISE EXCEPTION 'Marcas que solo se diferencian por las tildes; fusionarlas antes de seguir: %', v_duplicates;
    END IF;
END
$$;

DROP INDEX uq_brands_name;
CREATE UNIQUE INDEX uq_brands_name ON brands (lower(public.f_unaccent(name)));

COMMIT;

-- Sincronización (ejecutar después).
-- Esperado: CREATE UNIQUE INDEX uq_brands_name ON public.brands USING btree (lower(f_unaccent((name)::text)))
-- SELECT indexdef FROM pg_indexes WHERE indexname = 'uq_brands_name';
