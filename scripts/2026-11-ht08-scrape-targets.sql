-- HT-08 · Cargar scrape_targets (SCRUM-164)
-- Pasa el diccionario PRODUCTOS del scraper a la tabla scrape_targets: una fila por tienda y término.
-- Requiere el script de HT-09 (2026-11-ht09-modelo-identificacion.sql) ya aplicado.
-- Tiendas: D1, Éxito, Jumbo y Olímpica (Ara no está en el scraper). Se buscan por nombre, no por id.
-- Las filas entran con active = false: se activan al final, cuando HT-02 haya verificado los precios
-- regionales y el scraper escriba en scraped_offers. store_category_fq queda en NULL hasta tener
-- el fq real de cada tienda (VTEX).
-- Los rangos son los del diccionario: min = presentación más pequeña, max = más grande; NULL = sin límite
-- en ese lado. Los que no tenían filtro de tamaño quedan sin rango.
-- Es idempotente: no repite una fila (tienda, término, unidad) que ya exista.
-- El archivo está en UTF-8; sin esta línea, psql en Windows lo lee con la codificación de la
-- consola y 'Éxito', 'Olímpica' y 'Lácteos' no emparejan con los nombres de la base.
SET client_encoding TO 'UTF8';
BEGIN;

INSERT INTO scrape_targets (store_id, category_id, search_term, unit_base, min_content, max_content, active)
SELECT s.id, c.id, t.search_term, t.unit_base, t.min_content, t.max_content, false
FROM (VALUES
    -- término,               categoría hoja, unidad, mín,            máx
    ('arroz',                 'Granos',   'G',  500::numeric,  2500::numeric),
    ('harina maiz',           'Granos',   'G',  340,           500),
    ('frijoles',              'Granos',   'G',  300,           500),
    ('lentejas',              'Granos',   'G',  500,           500),
    ('spaguetti',             'Granos',   'G',  250,           500),
    ('garbanzo',              'Granos',   'G',  NULL,          NULL),
    ('avena',                 'Granos',   'G',  500,           1000),
    ('harina trigo',          'Granos',   'G',  1000,          1000),
    ('azucar',                'Granos',   'G',  1000,          2000),
    ('sal',                   'Granos',   'G',  500,           1000),
    ('aceite vegetal',        'Aceites',  'ML', 900,           900),
    ('mantequilla',           'Lácteos',  'G',  125,           250),
    ('pollo',                 'Carnes',   'G',  500,           500),
    ('carne res',             'Carnes',   'G',  500,           500),
    ('carne cerdo',           'Carnes',   'G',  500,           1000),
    ('pescado',               'Carnes',   'G',  500,           1000),
    ('mortadela',             'Carnes',   'G',  250,           250),
    ('huevo',                 'Lácteos',  'UN', NULL,          NULL),
    ('salchicha',             'Carnes',   'G',  250,           500),
    ('jamon',                 'Carnes',   'G',  NULL,          NULL),
    ('atun',                  'Carnes',   'G',  170,           170),
    ('leche entera',          'Lácteos',  'ML', 200,           900),
    ('leche deslactosada',    'Lácteos',  'ML', 900,           900),
    ('leche polvo',           'Lácteos',  'G',  300,           1000),
    ('yogur',                 'Lácteos',  'ML', 1000,          1000),
    ('queso costeno',         'Lácteos',  'G',  400,           400),
    ('crema leche',           'Lácteos',  'G',  200,           200),
    ('crema leche',           'Lácteos',  'ML', 200,           200),
    ('suero costeno',         'Lácteos',  'G',  NULL,          NULL),
    ('agua',                  'Bebidas',  'ML', 1000,          1000),
    ('gaseosa',               'Bebidas',  'ML', 1000,          2000),
    ('jugo',                  'Bebidas',  'ML', 1000,          2000),
    ('jugo caja',             'Bebidas',  'ML', 200,           250),
    ('cafe molido',           'Bebidas',  'G',  125,           500),
    ('cafe instantaneo',      'Bebidas',  'G',  85,            85),
    ('chocolate polvo',       'Bebidas',  'G',  100,           1000),
    ('jabon tocador',         'Aseo',     'G',  NULL,          NULL),
    ('shampoo',               'Aseo',     'ML', 300,           600),
    ('jabon liquido',         'Aseo',     'ML', 500,           1000),
    ('detergente multiusos',  'Aseo',     'G',  900,           2800),
    ('detergente liquido',    'Aseo',     'ML', 1000,          2000),
    ('suavizante',            'Aseo',     'ML', 1000,          2000),
    ('blanqueador',           'Aseo',     'ML', 1000,          2000),
    ('desinfectante',         'Aseo',     'ML', NULL,          NULL),
    ('papel higienico',       'Aseo',     'UN', NULL,          NULL),
    ('toalla cocina',         'Aseo',     'UN', NULL,          NULL),
    ('crema dental',          'Aseo',     'G',  NULL,          NULL),
    ('desodorante',           'Aseo',     'ML', 150,           150)
) AS t (search_term, category_name, unit_base, min_content, max_content)
JOIN categories c ON c.name = t.category_name AND c.parent_id IS NOT NULL
JOIN stores s ON s.name IN ('D1', 'Éxito', 'Jumbo', 'Olímpica')
WHERE NOT EXISTS (
    SELECT 1 FROM scrape_targets x
    WHERE x.store_id = s.id AND x.search_term = t.search_term AND x.unit_base = t.unit_base
);

-- Comprobación: deben ser 4 tiendas x 48 términos = 192 filas
-- SELECT count(*) FROM scrape_targets;
-- SELECT s.name, count(*) FROM scrape_targets t JOIN stores s ON s.id = t.store_id GROUP BY s.name;

-- Activación (correr aparte, cuando HT-02 esté listo):
-- UPDATE scrape_targets SET active = true;

COMMIT;
