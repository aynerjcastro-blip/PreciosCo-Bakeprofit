-- HU-39 · RN-33 · Plantilla para marcar productos de fórmula médica por lote.
-- Requiere 2026-11-hu39-productos-formula-medica.sql (columna products.requires_prescription).
--
-- Para qué: la lista de principios activos del backend (ai/rx-active-ingredients.txt) detecta un
-- medicamento de fórmula por su NOMBRE. Un producto con nombre comercial que no menciona el principio
-- activo se le escapa; ese caso se cubre marcando la columna, aquí por lote o uno a uno desde la API:
--   PATCH /api/products/{id}/prescription/require   (rol ADMIN)
--   PATCH /api/products/{id}/prescription/release   (rol ADMIN)
--
-- Nota: este script NO se ejecuta solo. Paso 1 es de consulta; el paso 2 se edita con los IDs que
-- confirme el químico farmacéutico. El asistente toma el cambio cuando su caché expira (30 min) o
-- de inmediato si se usa la API.

-- ── Paso 1: revisar los productos activos de Droguería y su marca actual ──
SELECT p.id,
       p.name,
       b.name  AS marca,
       c.name  AS subcategoria,
       p.requires_prescription
FROM public.products p
JOIN public.categories c ON c.id = p.category_id
JOIN public.categories r ON r.id = c.parent_id AND r.parent_id IS NULL
LEFT JOIN public.brands b ON b.id = p.brand_id
WHERE lower(r.name) = lower('Droguería')
  AND p.active = true
ORDER BY c.name, p.name;

-- ── Paso 2: marcar los confirmados (reemplazar los IDs) ──
-- BEGIN;
-- UPDATE public.products
-- SET requires_prescription = true
-- WHERE id IN (/* ids confirmados, p. ej. 101, 102 */);
-- COMMIT;

-- ── Deshacer una marca ──
-- UPDATE public.products SET requires_prescription = false WHERE id IN (/* ids */);

-- ── Verificación: productos marcados ──
-- SELECT id, name FROM public.products WHERE requires_prescription ORDER BY name;
