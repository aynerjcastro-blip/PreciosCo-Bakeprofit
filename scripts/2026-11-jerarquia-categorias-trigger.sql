-- Jerarquía de categorías en la base
-- La regla de dos niveles vivía solo en Java (CategoryService, ProductService, OfferScreening) y un
-- script de datos de prueba insertó directo en la tabla productos que colgaban de la raíz. Estos triggers la exigen también en la base:
--   * products.category_id y scrape_targets.category_id apuntan a una hoja (categoría con padre).
--   * categories: no hay tercer nivel, una categoría con hijas no pasa a ser hija, una categoría con
--     tiendas no pasa a ser hoja y una hoja con productos o targets no pasa a ser raíz.
-- stores.category_id -> raíz se cubre desde el lado de categories; al crear o editar una tienda
-- lo sigue validando Java.
-- Los errores usan SQLSTATE 23514 (check_violation): Spring los traduce a DataIntegrityViolationException.
-- Los triggers no revisan las filas existentes: antes de correrlo, las consultas de sincronización (a)
-- del final deben dar 0; si no, se corrigen esas filas primero.
-- CREATE OR REPLACE FUNCTION se puede repetir; CREATE TRIGGER no: si ya existen, correr antes la reversión.
-- Reversión: 2026-11-jerarquia-categorias-trigger-reversion.sql
SET client_encoding TO 'UTF8';
BEGIN;

-- 1. products y scrape_targets: la categoría debe ser una hoja
CREATE OR REPLACE FUNCTION public.enforce_leaf_category()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    v_parent_id BIGINT;
BEGIN
    IF NEW.category_id IS NULL THEN
        RETURN NEW;
    END IF;

    -- FOR SHARE: nadie cambia el padre de esta categoría hasta que termine la transacción
    SELECT parent_id INTO v_parent_id
    FROM public.categories
    WHERE id = NEW.category_id
    FOR SHARE;

    IF NOT FOUND THEN
        RETURN NEW;  -- categoría inexistente: la rechaza la llave foránea
    END IF;

    IF v_parent_id IS NULL THEN
        RAISE EXCEPTION '%: la categoría % es una raíz; debe ser una categoría hoja',
            TG_TABLE_NAME, NEW.category_id
            USING ERRCODE = 'check_violation';
    END IF;

    RETURN NEW;
END;
$$;

-- 2. categories: dos niveles y sin dejar huérfanos a productos, targets ni tiendas
CREATE OR REPLACE FUNCTION public.enforce_category_hierarchy()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    v_grandparent_id BIGINT;
BEGIN
    IF NEW.parent_id IS NOT NULL THEN
        IF NEW.parent_id = NEW.id THEN
            RAISE EXCEPTION 'categories: la categoría % no puede ser su propio padre', NEW.id
                USING ERRCODE = 'check_violation';
        END IF;

        SELECT parent_id INTO v_grandparent_id
        FROM public.categories
        WHERE id = NEW.parent_id
        FOR SHARE;

        IF FOUND AND v_grandparent_id IS NOT NULL THEN
            RAISE EXCEPTION 'categories: el padre % es una hoja; solo se admiten dos niveles', NEW.parent_id
                USING ERRCODE = 'check_violation';
        END IF;

        IF TG_OP = 'UPDATE' AND OLD.parent_id IS NULL THEN
            IF EXISTS (SELECT 1 FROM public.categories WHERE parent_id = NEW.id) THEN
                RAISE EXCEPTION 'categories: la categoría % tiene hijas y no puede pasar a ser hoja', NEW.id
                    USING ERRCODE = 'check_violation';
            END IF;
            IF EXISTS (SELECT 1 FROM public.stores WHERE category_id = NEW.id) THEN
                RAISE EXCEPTION 'categories: la categoría % tiene tiendas y no puede pasar a ser hoja', NEW.id
                    USING ERRCODE = 'check_violation';
            END IF;
        END IF;

    ELSIF TG_OP = 'UPDATE' AND OLD.parent_id IS NOT NULL THEN
        IF EXISTS (SELECT 1 FROM public.products WHERE category_id = NEW.id)
           OR EXISTS (SELECT 1 FROM public.scrape_targets WHERE category_id = NEW.id) THEN
            RAISE EXCEPTION 'categories: la categoría % tiene productos o targets y no puede pasar a ser raíz', NEW.id
                USING ERRCODE = 'check_violation';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

-- 3. Triggers
CREATE TRIGGER trg_products_leaf_category
    BEFORE INSERT OR UPDATE OF category_id ON public.products
    FOR EACH ROW EXECUTE FUNCTION public.enforce_leaf_category();

CREATE TRIGGER trg_scrape_targets_leaf_category
    BEFORE INSERT OR UPDATE OF category_id ON public.scrape_targets
    FOR EACH ROW EXECUTE FUNCTION public.enforce_leaf_category();

CREATE TRIGGER trg_categories_hierarchy
    BEFORE INSERT OR UPDATE OF parent_id ON public.categories
    FOR EACH ROW EXECUTE FUNCTION public.enforce_category_hierarchy();

-- 4. Seguridad (SCRUM-304): Supabase da EXECUTE a PUBLIC, anon y authenticated sobre cada función
--    nueva. Se quitan los tres; al dueño (postgres) no lo afecta. En un PostgreSQL local los roles
--    anon y authenticated no existen: ahí solo se revoca PUBLIC (igual que en SCRUM-140).
ALTER FUNCTION public.enforce_leaf_category() OWNER TO postgres;
ALTER FUNCTION public.enforce_category_hierarchy() OWNER TO postgres;
REVOKE EXECUTE ON FUNCTION public.enforce_leaf_category() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.enforce_category_hierarchy() FROM PUBLIC;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon')
       AND EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        REVOKE EXECUTE ON FUNCTION public.enforce_leaf_category() FROM anon, authenticated;
        REVOKE EXECUTE ON FUNCTION public.enforce_category_hierarchy() FROM anon, authenticated;
    ELSE
        RAISE NOTICE 'Sin roles anon y authenticated (PostgreSQL local): solo se revocó PUBLIC.';
    END IF;
END
$$;

COMMIT;

-- Consultas de sincronización (correr después del COMMIT)
-- a) Datos existentes que incumplen la jerarquía: las cuatro deben dar 0
-- SELECT count(*) AS productos_fuera_de_hoja FROM products p JOIN categories c ON c.id = p.category_id WHERE c.parent_id IS NULL;
-- SELECT count(*) AS targets_fuera_de_hoja FROM scrape_targets t JOIN categories c ON c.id = t.category_id WHERE c.parent_id IS NULL;
-- SELECT count(*) AS tiendas_fuera_de_raiz FROM stores s JOIN categories c ON c.id = s.category_id WHERE c.parent_id IS NOT NULL;
-- SELECT count(*) AS tercer_nivel FROM categories c JOIN categories p ON p.id = c.parent_id WHERE p.parent_id IS NOT NULL;
-- b) Triggers creados: debe dar 3
-- SELECT count(*) FROM pg_trigger WHERE tgname IN ('trg_products_leaf_category', 'trg_scrape_targets_leaf_category', 'trg_categories_hierarchy');
-- c) Privilegios (solo en Supabase; en un PostgreSQL local no existen anon ni authenticated): todo false
-- SELECT f,
--        has_function_privilege('public', f, 'EXECUTE')        AS public_,
--        has_function_privilege('anon', f, 'EXECUTE')          AS anon,
--        has_function_privilege('authenticated', f, 'EXECUTE') AS authenticated
-- FROM unnest(ARRAY['public.enforce_leaf_category()', 'public.enforce_category_hierarchy()']) AS f;
