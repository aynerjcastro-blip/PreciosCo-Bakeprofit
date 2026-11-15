-- EP-04 · Ajuste de los objetivos de Drogas La Rebaja tras revisar su catálogo (13-nov-2026).
-- Correr después de 2026-11-droguerias-categorias-tiendas.sql.
--
-- 1. store_category_fq: la búsqueda solo por texto mezcla categorías ("acetaminofen 500 mg" trae
--    vitamina C 500 mg, que quedaría en "Dolor y fiebre"). El fq limita la búsqueda a la categoría
--    de La Rebaja donde está el producto.
-- 2. Presentaciones que La Rebaja no vende: diclofenaco gel en 50 g (no hay de 40 g) y alcohol entre
--    300 y 400 ml (el de 350 ml se publica como 345 ml).
-- 3. Loratadina y cetirizina son de fórmula médica en La Rebaja: el filtro las descarta todas. Se
--    reemplaza loratadina por antigripales (venta libre) y se borra cetirizina.
-- 4. "bandas adhesivas" no da resultados; La Rebaja las llama "curas".
-- Es idempotente: correrlo dos veces deja lo mismo.
SET client_encoding TO 'UTF8';
BEGIN;

UPDATE scrape_targets t
SET search_term = 'antigripal', store_category_fq = 'C:/9/53/'
FROM stores s
WHERE s.id = t.store_id AND s.name = 'Drogas La Rebaja' AND t.search_term = 'loratadina 10 mg';

DELETE FROM scrape_targets t
USING stores s
WHERE s.id = t.store_id AND s.name = 'Drogas La Rebaja' AND t.search_term = 'cetirizina 10 mg';

UPDATE scrape_targets t
SET search_term = 'curas'
FROM stores s
WHERE s.id = t.store_id AND s.name = 'Drogas La Rebaja' AND t.search_term = 'bandas adhesivas';

UPDATE scrape_targets t
SET store_category_fq = v.fq,
    min_content = COALESCE(v.min_content, t.min_content),
    max_content = COALESCE(v.max_content, t.max_content)
FROM stores s,
     (VALUES
        -- término,                          fq de La Rebaja (categoría),  mín, máx (NULL = no cambia)
        ('acetaminofen 500 mg',              'C:/9/50/',        NULL::numeric, NULL::numeric),  -- Dolor e inflamación
        ('acetaminofen jarabe',              'C:/9/50/',        NULL, NULL),
        ('acido acetilsalicilico 500 mg',    'C:/9/50/',        NULL, NULL),
        ('diclofenaco gel',                  'C:/9/50/248/',    50,   50),    -- Dolor muscular - articular
        ('antigripal',                       'C:/9/53/',        NULL, NULL),  -- Gripa y tos
        ('sales de rehidratacion oral',      'C:/9/62/',        NULL, NULL),  -- Rehidratación
        ('hidroxido de aluminio y magnesio', 'C:/9/52/257/',    NULL, NULL),  -- Antiácidos y antiulcerosos
        ('vitamina c 500 mg',                'C:/9/64/1221/',   NULL, NULL),  -- Vitamina C
        ('calcio vitamina d',                'C:/9/64/1218/',   NULL, NULL),  -- Calcio
        ('alcohol antiseptico',              'C:/9/48/242/',    300,  400),   -- Botiquín
        ('algodon',                          'C:/9/48/243/',    NULL, NULL),  -- Cubrimiento
        ('curas',                            'C:/9/48/243/',    NULL, NULL),
        ('gasa esteril',                     'C:/9/48/243/',    NULL, NULL),
        ('povidona yodada',                  'C:/9/48/',        NULL, NULL),  -- Cuidado de la herida
        ('termometro digital',               'C:/6/36/459/',    NULL, NULL),  -- Termómetros
        ('tapabocas',                        'C:/6/39/216/',    NULL, NULL),  -- Insumos médicos
        ('preservativos',                    'C:/9/989/990/',   NULL, NULL),  -- Preservativos
        ('prueba de embarazo',               'C:/9/989/1004/',  NULL, NULL),  -- Pruebas de embarazo
        ('toallas higienicas',               'C:/5/34/199/',    NULL, NULL),  -- Protección femenina
        ('clotrimazol crema',                'C:/9/44/',        NULL, NULL)   -- Antimicóticos
     ) AS v (search_term, fq, min_content, max_content)
WHERE s.id = t.store_id AND s.name = 'Drogas La Rebaja' AND t.search_term = v.search_term;

COMMIT;

-- Sincronización: (20) objetivos de La Rebaja, todos con fq
-- SELECT count(*), count(store_category_fq) FROM scrape_targets t JOIN stores s ON s.id = t.store_id WHERE s.name = 'Drogas La Rebaja';