-- HT-13: ahorro por producto (vista). Probar primero en la base de pruebas.
-- SUPUESTOS A AJUSTAR: prices(product_id, store_id, value, registration_date, id), stores(id, active), products(id)
BEGIN;

CREATE OR REPLACE VIEW public.product_savings
WITH (security_invoker = true) AS
WITH current_prices AS (
  -- precio vigente: la fila más reciente de cada par producto-tienda, solo tiendas activas (DP-34, DP-37)
  SELECT DISTINCT ON (p.product_id, p.store_id)
         p.product_id, p.store_id, p.value
  FROM public.prices p
  JOIN public.stores s ON s.id = p.store_id AND s.active
  ORDER BY p.product_id, p.store_id, p.registration_date DESC, p.id DESC
), agg AS (
  SELECT product_id, count(*) AS store_count, max(value) AS max_price, min(value) AS min_price
  FROM current_prices GROUP BY product_id
)
SELECT pr.id AS product_id,
       COALESCE(a.store_count, 0) AS store_count,
       a.min_price,
       a.max_price,
       CASE WHEN COALESCE(a.store_count, 0) > 1 AND a.max_price > 0
            THEN round((a.max_price - a.min_price)::numeric / a.max_price::numeric, 4)
            ELSE 0 END AS savings_pct
FROM public.products pr
LEFT JOIN agg a ON a.product_id = pr.id;

-- Supabase publica por REST lo del esquema public: nadie más que el backend la lee
REVOKE ALL ON public.product_savings FROM PUBLIC, anon, authenticated;

-- Consulta de sincronización: las dos deben dar false
SELECT has_table_privilege('anon', 'public.product_savings', 'SELECT') AS anon_puede,
       has_table_privilege('authenticated', 'public.product_savings', 'SELECT') AS auth_puede;

COMMIT;