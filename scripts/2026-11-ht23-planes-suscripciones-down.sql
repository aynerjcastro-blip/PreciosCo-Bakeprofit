BEGIN;

-- Al eliminar la tabla se revierten también la referencia externa y su restricción UNIQUE.
DROP TABLE public.payment_transactions;
DROP TABLE public.subscriptions;

ALTER TABLE public.users
    DROP COLUMN ai_queries_reset_at,
    DROP COLUMN ai_queries_used,
    DROP COLUMN plan;

COMMIT;
