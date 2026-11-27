BEGIN;

LOCK TABLE public.payment_transactions IN ACCESS EXCLUSIVE MODE;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.payment_transactions WHERE external_order_id IS NULL) THEN
        RAISE EXCEPTION 'No se puede revertir HT-24: hay pagos con external_order_id NULL. Conserva los pagos y resuelve esas referencias antes de reintentar.';
    END IF;
END $$;

ALTER TABLE public.payment_transactions
    DROP CONSTRAINT payment_transactions_external_reference_key,
    DROP COLUMN external_reference;

DO $$
DECLARE
    status_check RECORD;
    original JSON;
BEGIN
    FOR status_check IN
        SELECT c.conname, obj_description(c.oid, 'pg_constraint') AS metadata
        FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attname = 'status'
        WHERE c.conrelid = 'public.payment_transactions'::regclass
          AND c.contype = 'c' AND c.conkey = ARRAY[a.attnum]
          AND obj_description(c.oid, 'pg_constraint') LIKE 'HT24:%'
    LOOP
        original := substring(status_check.metadata FROM 6)::json;
        EXECUTE format('ALTER TABLE public.payment_transactions DROP CONSTRAINT %I', status_check.conname);
        EXECUTE format('ALTER TABLE public.payment_transactions ADD CONSTRAINT %I %s',
            status_check.conname, original->>'definition');
        EXECUTE format('COMMENT ON CONSTRAINT %I ON public.payment_transactions IS %L',
            status_check.conname, original->>'comment');
    END LOOP;
END $$;

ALTER TABLE public.payment_transactions
    RENAME CONSTRAINT payment_transactions_external_order_id_key TO payment_transactions_external_payment_id_key;
ALTER TABLE public.payment_transactions
    RENAME COLUMN external_order_id TO external_payment_id;
ALTER TABLE public.payment_transactions ALTER COLUMN external_payment_id SET NOT NULL;

COMMIT;
