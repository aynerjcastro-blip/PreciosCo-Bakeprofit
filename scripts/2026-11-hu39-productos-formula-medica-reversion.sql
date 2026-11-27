-- HU-39 · Reversión de 2026-11-hu39-productos-formula-medica.sql: borra la marca de fórmula médica.
-- Antes de ejecutarla, desplegar un backend sin HU-39: Product mapea requires_prescription y, con
-- ddl-auto=validate, Hibernate no arranca sin la columna.
-- Las marcas hechas a mano se pierden; la lista de principios activos del backend no depende de esta columna.
BEGIN;

ALTER TABLE public.products
    DROP COLUMN IF EXISTS requires_prescription;

COMMIT;

-- Sincronización (ejecutar después).
-- Esperado: 0 filas
-- SELECT column_name FROM information_schema.columns
-- WHERE table_schema = 'public' AND table_name = 'products' AND column_name = 'requires_prescription';
