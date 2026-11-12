-- Reversión de 2026-11-jerarquia-categorias-trigger.sql
-- Quita los triggers y sus funciones; no toca datos.
BEGIN;

DROP TRIGGER IF EXISTS trg_products_leaf_category ON public.products;
DROP TRIGGER IF EXISTS trg_scrape_targets_leaf_category ON public.scrape_targets;
DROP TRIGGER IF EXISTS trg_categories_hierarchy ON public.categories;

DROP FUNCTION IF EXISTS public.enforce_leaf_category();
DROP FUNCTION IF EXISTS public.enforce_category_hierarchy();

COMMIT;
