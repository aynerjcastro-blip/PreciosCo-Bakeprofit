BEGIN;

ALTER TABLE public.payment_transactions
    RENAME COLUMN external_payment_id TO external_order_id;
ALTER TABLE public.payment_transactions
    RENAME CONSTRAINT payment_transactions_external_payment_id_key TO payment_transactions_external_order_id_key;
ALTER TABLE public.payment_transactions ALTER COLUMN external_order_id DROP NOT NULL;

-- Conserva las referencias si se aplicó la versión de HT-23 que ya las incluía.
ALTER TABLE public.payment_transactions ADD COLUMN IF NOT EXISTS external_reference VARCHAR(64);
UPDATE public.payment_transactions
SET external_reference = gen_random_uuid()::text
WHERE external_reference IS NULL;
ALTER TABLE public.payment_transactions
    ALTER COLUMN external_reference SET NOT NULL,
    ALTER COLUMN external_reference DROP DEFAULT;

DO $$
DECLARE
    status_check RECORD;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.payment_transactions'::regclass
          AND conname = 'payment_transactions_external_reference_key'
    ) THEN
        ALTER TABLE public.payment_transactions
            ADD CONSTRAINT payment_transactions_external_reference_key UNIQUE (external_reference);
    END IF;

    -- HT-23 no declara un CHECK de status. Si una base lo agregó, se conserva su
    -- definición y comentario para poder restaurarlos sin conocer su nombre.
    FOR status_check IN
        SELECT c.conname, pg_get_constraintdef(c.oid) AS definition,
               pg_get_expr(c.conbin, c.conrelid) AS expression,
               obj_description(c.oid, 'pg_constraint') AS original_comment,
               c.connoinherit, c.convalidated
        FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attname = 'status'
        WHERE c.conrelid = 'public.payment_transactions'::regclass
          AND c.contype = 'c' AND c.conkey = ARRAY[a.attnum]
    LOOP
        EXECUTE format('ALTER TABLE public.payment_transactions DROP CONSTRAINT %I', status_check.conname);
        EXECUTE format(
            'ALTER TABLE public.payment_transactions ADD CONSTRAINT %I CHECK (status = %L OR (%s))%s%s',
            status_check.conname, 'CREATED', status_check.expression,
            CASE WHEN status_check.connoinherit THEN ' NO INHERIT' ELSE '' END,
            CASE WHEN NOT status_check.convalidated THEN ' NOT VALID' ELSE '' END
        );
        EXECUTE format('COMMENT ON CONSTRAINT %I ON public.payment_transactions IS %L',
            status_check.conname, 'HT24:' || json_build_object(
                'definition', status_check.definition, 'comment', status_check.original_comment
            )::text);
    END LOOP;
END $$;

COMMIT;

-- Sincronización de HT-24. Cada verificación debe devolver true.
WITH payment_columns AS (
    SELECT attname, attnum, attnotnull
    FROM pg_attribute
    WHERE attrelid = 'public.payment_transactions'::regclass
      AND attnum > 0 AND NOT attisdropped
)
SELECT 'external_order_id admite NULL' AS verificacion, EXISTS (
    SELECT 1 FROM payment_columns WHERE attname = 'external_order_id' AND NOT attnotnull
) AS ok
UNION ALL
SELECT 'external_reference es NOT NULL', EXISTS (
    SELECT 1 FROM payment_columns WHERE attname = 'external_reference' AND attnotnull
)
UNION ALL
SELECT 'UNIQUE ' || expected.constraint_name, EXISTS (
    SELECT 1 FROM pg_constraint c
    JOIN payment_columns col ON c.conkey = ARRAY[col.attnum]
    WHERE c.conrelid = 'public.payment_transactions'::regclass AND c.contype = 'u'
      AND c.conname = expected.constraint_name AND col.attname = expected.column_name
)
FROM (VALUES
    ('payment_transactions_external_order_id_key', 'external_order_id'),
    ('payment_transactions_external_reference_key', 'external_reference')
) AS expected(constraint_name, column_name)
UNION ALL
SELECT 'RLS ' || expected.table_name, EXISTS (
    SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' AND c.relname = expected.table_name AND c.relrowsecurity
)
FROM (VALUES ('payment_transactions'), ('subscriptions')) AS expected(table_name);
