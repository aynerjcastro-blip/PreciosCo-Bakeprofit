-- HU-39 · RF63 · RN-33 · Marca de "requiere fórmula médica" en el catálogo.
-- El asistente de IA de droguería excluye, antes de enviar datos al modelo, los productos con
-- requires_prescription = true. Es una de las dos fuentes del filtro (DP-56); la otra es la lista de
-- principios activos de fórmula del backend (ai/rx-active-ingredients.txt). Basta con que una lo marque.
-- La columna es independiente del scraper: se marca por script o desde administración.
-- Ejecutar ANTES de desplegar el backend con HU-39: con ddl-auto=validate, Hibernate no arranca
-- si la columna no existe.
BEGIN;

ALTER TABLE public.products
    ADD COLUMN requires_prescription BOOLEAN NOT NULL DEFAULT false;

COMMIT;

-- Marcar un producto concreto (ejemplo):
-- UPDATE public.products SET requires_prescription = true WHERE id = <id>;

-- Sincronización (ejecutar después).
-- Esperado: 1 fila → requires_prescription | boolean | NO | false
-- SELECT column_name, data_type, is_nullable, column_default
-- FROM information_schema.columns
-- WHERE table_schema = 'public' AND table_name = 'products' AND column_name = 'requires_prescription';
