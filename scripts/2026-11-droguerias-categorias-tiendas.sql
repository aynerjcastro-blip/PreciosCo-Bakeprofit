-- EP-04 · Dominio Droguería: categoría raíz, 7 categorías hoja, 3 tiendas y scrape_targets (HU-28, HU-29)
-- Requiere HT-09 (scrape_targets) y el script de jerarquía de categorías ya aplicados.
--
-- Qué hace:
--   1. Crea la raíz 'Droguería' y sus 7 hojas (acordadas el 12-nov-2026).
--   2. Crea las tiendas 'Drogas La Rebaja', 'Farmatodo - Droguería' y 'Farmanorte', INACTIVAS:
--      el procesador carga sus precios igual, pero no aparecen en la búsqueda ni en el comparador
--      hasta que se activen (ver "Activación" al final).
--   3. Agrega scrape_targets.target_url: Farmatodo y Farmanorte prohíben la búsqueda en su
--      robots.txt, así que sus objetivos son páginas de producto, no términos. La entidad
--      ScrapeTarget no usa la columna (ddl-auto=validate admite columnas sin mapear); solo la
--      lee el scraper.
--   4. Carga los objetivos de La Rebaja (búsqueda por término en la API VTEX), INACTIVOS, con
--      la lista de productos de Darwin. El rango de tamaño es la presentación exacta de la lista
--      (mín = máx). store_category_fq queda en NULL.
--
-- Es idempotente: no repite categorías, tiendas ni objetivos que ya existan.
-- Los nombres de categoría son únicos en todo el catálogo (CategoryService): si alguna de las
-- 7 hojas ya existe bajo otra raíz, el script se detiene sin cambiar nada.
-- El archivo está en UTF-8; sin esta línea, psql en Windows no empareja los nombres con tilde.
SET client_encoding TO 'UTF8';
BEGIN;

-- 0. Ningún nombre puede estar ya en uso fuera de Droguería
DO $$
DECLARE
    ocupados TEXT;
BEGIN
    SELECT string_agg(c.name, ', ') INTO ocupados
    FROM categories c
    LEFT JOIN categories p ON p.id = c.parent_id
    WHERE (lower(c.name) = lower('Droguería') AND c.parent_id IS NOT NULL)
       OR (lower(c.name) IN (lower('Dolor y fiebre'), lower('Gripa, tos y alergias'),
                             lower('Digestivos e hidratación'), lower('Vitaminas y suplementos'),
                             lower('Primeros auxilios e insumos'),
                             lower('Cuidado íntimo y salud sexual'), lower('Dermatológicos'))
           AND (p.id IS NULL OR lower(p.name) <> lower('Droguería')));
    IF ocupados IS NOT NULL THEN
        RAISE EXCEPTION 'Nombres de categoría ya usados fuera de Droguería: %', ocupados;
    END IF;
END $$;

-- 1. Raíz
INSERT INTO categories (name, active, registration_date, parent_id)
SELECT 'Droguería', true, now(), NULL
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE lower(name) = lower('Droguería'));

-- 2. Hojas
INSERT INTO categories (name, active, registration_date, parent_id)
SELECT h.name, true, now(), r.id
FROM (VALUES
    ('Dolor y fiebre'),
    ('Gripa, tos y alergias'),
    ('Digestivos e hidratación'),
    ('Vitaminas y suplementos'),
    ('Primeros auxilios e insumos'),
    ('Cuidado íntimo y salud sexual'),
    ('Dermatológicos')
) AS h (name)
JOIN categories r ON lower(r.name) = lower('Droguería') AND r.parent_id IS NULL
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE lower(c.name) = lower(h.name));

-- 3. Tiendas (inactivas)
INSERT INTO stores (name, active, registration_date, category_id)
SELECT t.name, false, now(), r.id
FROM (VALUES
    ('Drogas La Rebaja'),
    ('Farmatodo - Droguería'),
    ('Farmanorte')
) AS t (name)
JOIN categories r ON lower(r.name) = lower('Droguería') AND r.parent_id IS NULL
WHERE NOT EXISTS (SELECT 1 FROM stores s WHERE lower(s.name) = lower(t.name));

-- 4. Página de producto del objetivo (tiendas sin búsqueda permitida)
ALTER TABLE scrape_targets ADD COLUMN IF NOT EXISTS target_url TEXT;

-- 5. Objetivos de La Rebaja (inactivos): lista de productos de Darwin, presentación exacta.
--    search_term es también la clave del producto genérico: los objetivos de Farmatodo y
--    Farmanorte para el mismo producto deben llevar el mismo término (ver plantilla al final).
INSERT INTO scrape_targets (store_id, category_id, search_term, unit_base, min_content, max_content, active)
SELECT s.id, c.id, t.search_term, t.unit_base, t.content, t.content, false
FROM (VALUES
    -- término,                            categoría hoja,                   unidad, contenido
    ('acetaminofen 500 mg',                'Dolor y fiebre',                 'UN',  10),
    ('acetaminofen jarabe',                'Dolor y fiebre',                 'ML',  90),
    ('acido acetilsalicilico 500 mg',      'Dolor y fiebre',                 'UN',  10),
    ('diclofenaco gel',                    'Dolor y fiebre',                 'G',   40),
    ('loratadina 10 mg',                   'Gripa, tos y alergias',          'UN',  10),
    ('cetirizina 10 mg',                   'Gripa, tos y alergias',          'UN',  10),
    ('sales de rehidratacion oral',        'Digestivos e hidratación',       'UN',   1),
    ('hidroxido de aluminio y magnesio',   'Digestivos e hidratación',       'ML', 360),
    ('vitamina c 500 mg',                  'Vitaminas y suplementos',        'UN',  10),
    ('calcio vitamina d',                  'Vitaminas y suplementos',        'UN',  30),
    ('alcohol antiseptico',                'Primeros auxilios e insumos',    'ML', 350),
    ('algodon',                            'Primeros auxilios e insumos',    'G',  100),
    ('bandas adhesivas',                   'Primeros auxilios e insumos',    'UN',  20),
    ('gasa esteril',                       'Primeros auxilios e insumos',    'UN',   5),
    ('povidona yodada',                    'Primeros auxilios e insumos',    'ML', 120),
    ('termometro digital',                 'Primeros auxilios e insumos',    'UN',   1),
    ('tapabocas',                          'Primeros auxilios e insumos',    'UN',  50),
    ('preservativos',                      'Cuidado íntimo y salud sexual',  'UN',   3),
    ('prueba de embarazo',                 'Cuidado íntimo y salud sexual',  'UN',   1),
    ('toallas higienicas',                 'Cuidado íntimo y salud sexual',  'UN',  10),
    -- propuesta: la lista de Darwin no tiene productos para esta hoja
    ('clotrimazol crema',                  'Dermatológicos',                 'G',   20)
) AS t (search_term, category_name, unit_base, content)
JOIN categories c ON c.name = t.category_name AND c.parent_id IS NOT NULL
JOIN stores s ON s.name = 'Drogas La Rebaja'
WHERE NOT EXISTS (
    SELECT 1 FROM scrape_targets x
    WHERE x.store_id = s.id AND x.search_term = t.search_term AND x.unit_base = t.unit_base
);

COMMIT;

-- Sincronización (correr después; resultados esperados entre paréntesis)
-- SELECT count(*) FROM categories c JOIN categories r ON r.id = c.parent_id WHERE r.name = 'Droguería';            -- (7)
-- SELECT s.name, s.active FROM stores s JOIN categories r ON r.id = s.category_id WHERE r.name = 'Droguería';      -- (3 filas, false)
-- SELECT count(*) FROM scrape_targets t JOIN stores s ON s.id = t.store_id WHERE s.name = 'Drogas La Rebaja';      -- (21)
-- SELECT count(*) FROM information_schema.columns WHERE table_name = 'scrape_targets' AND column_name = 'target_url'; -- (1)

-- Activación (correr aparte, cuando el scraper de droguerías ya filtre los objetivos por dominio):
-- UPDATE scrape_targets SET active = true WHERE store_id = (SELECT id FROM stores WHERE name = 'Drogas La Rebaja');
-- Y cuando la tienda ya tenga precios cargados y revisados:
-- UPDATE stores SET active = true WHERE name = 'Drogas La Rebaja';

-- Plantilla para Farmatodo y Farmanorte (una fila por página de producto, mismo término que en La
-- Rebaja). Rellenar con las URL curadas y correr aparte:
-- INSERT INTO scrape_targets (store_id, category_id, search_term, target_url, unit_base, min_content, max_content, active)
-- SELECT s.id, c.id, t.search_term, t.target_url, t.unit_base, t.content, t.content, false
-- FROM (VALUES
--     ('acetaminofen 500 mg', 'Dolor y fiebre', 'UN', 10, 'Farmanorte', 'https://www.farmanorte.com.co/.../p')
-- ) AS t (search_term, category_name, unit_base, content, store_name, target_url)
-- JOIN categories c ON c.name = t.category_name AND c.parent_id IS NOT NULL
-- JOIN stores s ON s.name = t.store_name
-- WHERE NOT EXISTS (SELECT 1 FROM scrape_targets x WHERE x.store_id = s.id AND x.target_url = t.target_url);