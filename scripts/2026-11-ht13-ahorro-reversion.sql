-- Reversión de 2026-11-ht13-ahorro.sql (HT-13, SCRUM-309).
-- OJO: después de esto un backend con HT-13 no arranca (ddl-auto=validate busca product_savings).
-- Revertir solo junto con el código de HT-13.
DROP VIEW IF EXISTS public.product_savings;
