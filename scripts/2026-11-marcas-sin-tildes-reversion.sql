-- Reversión de 2026-11-marcas-sin-tildes.sql
-- Vuelve a uq_brands_name sobre lower(name). No falla por duplicados: el índice anterior era más
-- estricto, así que las marcas que permitía también son únicas con lower(name).
-- Va junto con la reversión del código: BrandRepository debe volver a usar lower(name), o el
-- ON CONFLICT dejará de encontrar el índice y fallará.
BEGIN;

DROP INDEX uq_brands_name;
CREATE UNIQUE INDEX uq_brands_name ON brands (lower(name));

COMMIT;
