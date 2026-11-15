-- Reversión de 2026-11-droguerias-categorias-tiendas.sql (EP-04).
-- Borra los objetivos, las tiendas, las 7 hojas y la raíz 'Droguería', y quita scrape_targets.target_url.
-- Si ya hay ofertas en scraped_offers, productos o precios de estas tiendas o categorías, las FK
-- hacen fallar el borrado y no se cambia nada: primero hay que borrar o reasignar esos datos.
-- Si otro dominio ya usa target_url (cuidado facial), no correr el ALTER del final.
SET client_encoding TO 'UTF8';
BEGIN;

DELETE FROM scrape_targets
WHERE store_id IN (SELECT id FROM stores
                   WHERE name IN ('Drogas La Rebaja', 'Farmatodo - Droguería', 'Farmanorte'));

DELETE FROM stores
WHERE name IN ('Drogas La Rebaja', 'Farmatodo - Droguería', 'Farmanorte');

DELETE FROM categories
WHERE parent_id = (SELECT id FROM categories WHERE name = 'Droguería' AND parent_id IS NULL);

DELETE FROM categories
WHERE name = 'Droguería' AND parent_id IS NULL;

ALTER TABLE scrape_targets DROP COLUMN IF EXISTS target_url;

COMMIT;
