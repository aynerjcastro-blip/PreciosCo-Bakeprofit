BEGIN;

ALTER TABLE public.users
    ADD COLUMN plan VARCHAR(10) NOT NULL DEFAULT 'FREE'
        CONSTRAINT chk_users_plan CHECK (plan IN ('FREE', 'PREMIUM')),
    ADD COLUMN ai_queries_used INT NOT NULL DEFAULT 0
        CONSTRAINT chk_users_ai_queries_used CHECK (ai_queries_used >= 0),
    ADD COLUMN ai_queries_reset_at TIMESTAMPTZ;

-- Inicializa las filas existentes con la fecha de reinicio de la migración.
UPDATE public.users
SET ai_queries_reset_at = now() + interval '1 month';

ALTER TABLE public.users
    ALTER COLUMN ai_queries_reset_at SET DEFAULT (now() + interval '1 month'),
    ALTER COLUMN ai_queries_reset_at SET NOT NULL;

CREATE TABLE public.subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_subscriptions_users FOREIGN KEY (user_id) REFERENCES public.users (id),
    CONSTRAINT chk_subscriptions_plan CHECK (plan IN ('FREE', 'PREMIUM')),
    CONSTRAINT chk_subscriptions_status CHECK (status IN ('ACTIVE', 'CANCELLED', 'EXPIRED'))
);

CREATE INDEX idx_subscriptions_user_id ON public.subscriptions (user_id);

CREATE TABLE public.payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subscription_id BIGINT NULL,
    external_order_id VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    status_detail VARCHAR(100) NULL,
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_payment_transactions_users FOREIGN KEY (user_id) REFERENCES public.users (id),
    CONSTRAINT fk_payment_transactions_subscriptions FOREIGN KEY (subscription_id) REFERENCES public.subscriptions (id),
    -- Impide vincular dos intentos con la misma Order de Mercado Pago.
    CONSTRAINT payment_transactions_external_order_id_key UNIQUE (external_order_id)
);

CREATE INDEX idx_payment_transactions_user_id ON public.payment_transactions (user_id);

ALTER TABLE public.subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payment_transactions ENABLE ROW LEVEL SECURITY;

-- Permite registrar la referencia de la aplicación antes de recibir el ID de Mercado Pago.
-- En una base que todavía use el nombre anterior, ejecutar estos dos cambios en una transacción.
-- ALTER TABLE public.payment_transactions RENAME COLUMN external_payment_id TO external_order_id;
-- ALTER TABLE public.payment_transactions RENAME CONSTRAINT payment_transactions_external_payment_id_key
--     TO payment_transactions_external_order_id_key;
-- Si la referencia aún no existe, ejecutar también estos tres ALTER TABLE dentro de esa transacción.
ALTER TABLE public.payment_transactions
    ALTER COLUMN external_order_id DROP NOT NULL;

ALTER TABLE public.payment_transactions
    ADD COLUMN external_reference VARCHAR(64) NOT NULL DEFAULT gen_random_uuid()::text;

ALTER TABLE public.payment_transactions
    ALTER COLUMN external_reference DROP DEFAULT,
    ADD CONSTRAINT payment_transactions_external_reference_key UNIQUE (external_reference);

COMMIT;

-- Consulta de sincronización. Puede ejecutarse de forma independiente para verificar
-- el esquema de HT-23 y devuelve una fila booleana por cada verificación.
WITH expected_tables (table_name) AS (
    VALUES ('subscriptions'), ('payment_transactions')
), expected_columns (table_name, column_name, column_type, not_null, default_expression) AS (
    VALUES
        ('users', 'plan', 'character varying(10)', true, format('%L::character varying', 'FREE')),
        ('users', 'ai_queries_used', 'integer', true, '0'),
        ('users', 'ai_queries_reset_at', 'timestamp with time zone', true,
            format('(now() + %L::interval)', interval '1 month')),
        ('subscriptions', 'id', 'bigint', true, NULL),
        ('subscriptions', 'user_id', 'bigint', true, NULL),
        ('subscriptions', 'plan', 'character varying(10)', true, NULL),
        ('subscriptions', 'status', 'character varying(20)', true, NULL),
        ('subscriptions', 'started_at', 'timestamp with time zone', true, NULL),
        ('subscriptions', 'ends_at', 'timestamp with time zone', false, NULL),
        ('subscriptions', 'created_at', 'timestamp with time zone', true, 'now()'),
        ('payment_transactions', 'id', 'bigint', true, NULL),
        ('payment_transactions', 'user_id', 'bigint', true, NULL),
        ('payment_transactions', 'subscription_id', 'bigint', false, NULL),
        ('payment_transactions', 'external_order_id', 'character varying(50)', false, NULL),
        ('payment_transactions', 'external_reference', 'character varying(64)', true, NULL),
        ('payment_transactions', 'status', 'character varying(30)', true, NULL),
        ('payment_transactions', 'status_detail', 'character varying(100)', false, NULL),
        ('payment_transactions', 'amount', 'numeric(12,2)', true, NULL),
        ('payment_transactions', 'currency', 'character varying(3)', true, NULL),
        ('payment_transactions', 'created_at', 'timestamp with time zone', true, 'now()'),
        ('payment_transactions', 'updated_at', 'timestamp with time zone', true, 'now()')
), expected_constraints (table_name, constraint_name, constraint_type, column_name, referenced_table, check_definition) AS (
    VALUES
        ('users', 'chk_users_plan', 'c', 'plan', NULL,
            $check$CHECK (plan::text = ANY (ARRAY['FREE'::character varying, 'PREMIUM'::character varying]::text[]))$check$),
        ('users', 'chk_users_ai_queries_used', 'c', 'ai_queries_used', NULL, 'CHECK (ai_queries_used >= 0)'),
        ('subscriptions', 'fk_subscriptions_users', 'f', 'user_id', 'users', NULL),
        ('payment_transactions', 'fk_payment_transactions_users', 'f', 'user_id', 'users', NULL),
        ('payment_transactions', 'fk_payment_transactions_subscriptions', 'f', 'subscription_id', 'subscriptions', NULL),
        ('payment_transactions', 'payment_transactions_external_order_id_key', 'u', 'external_order_id', NULL, NULL),
        ('payment_transactions', 'payment_transactions_external_reference_key', 'u', 'external_reference', NULL, NULL)
), actual_tables AS (
    SELECT relation.oid AS table_id, relation.relname::text AS table_name, relation.relrowsecurity
    FROM pg_catalog.pg_class AS relation
    JOIN pg_catalog.pg_namespace AS namespace ON namespace.oid = relation.relnamespace
    WHERE namespace.nspname = 'public'
      AND relation.relkind IN ('r', 'p')
      AND relation.relname IN ('users', 'subscriptions', 'payment_transactions')
), actual_columns AS (
    SELECT table_info.table_id, table_info.table_name, attribute.attnum AS column_number,
           attribute.attname::text AS column_name,
           pg_catalog.format_type(attribute.atttypid, attribute.atttypmod) AS column_type,
           attribute.attnotnull AS not_null,
           pg_catalog.pg_get_expr(column_default.adbin, column_default.adrelid) AS default_expression
    FROM actual_tables AS table_info
    JOIN pg_catalog.pg_attribute AS attribute ON attribute.attrelid = table_info.table_id
    LEFT JOIN pg_catalog.pg_attrdef AS column_default
        ON column_default.adrelid = attribute.attrelid AND column_default.adnum = attribute.attnum
    WHERE attribute.attnum > 0 AND NOT attribute.attisdropped
)
SELECT 'tabla public.' || expected.table_name AS verificacion,
       EXISTS (SELECT 1 FROM actual_tables AS actual WHERE actual.table_name = expected.table_name) AS resultado
FROM expected_tables AS expected
UNION ALL
SELECT 'columna public.' || expected.table_name || '.' || expected.column_name,
       EXISTS (
           SELECT 1
           FROM actual_columns AS actual
           WHERE actual.table_name = expected.table_name
             AND actual.column_name = expected.column_name
             AND actual.column_type = expected.column_type
             AND actual.not_null = expected.not_null
             AND (expected.default_expression IS NULL OR actual.default_expression = expected.default_expression)
       )
FROM expected_columns AS expected
UNION ALL
SELECT 'restricción public.' || expected.table_name || '.' || expected.constraint_name,
       EXISTS (
           SELECT 1
           FROM actual_columns AS actual
           JOIN pg_catalog.pg_constraint AS constraint_info
               ON constraint_info.conrelid = actual.table_id
              AND constraint_info.conkey = ARRAY[actual.column_number]
           WHERE actual.table_name = expected.table_name
             AND actual.column_name = expected.column_name
             AND constraint_info.conname = expected.constraint_name
             AND constraint_info.contype::text = expected.constraint_type
             AND constraint_info.convalidated
             AND (expected.check_definition IS NULL
                 OR pg_catalog.pg_get_constraintdef(constraint_info.oid, true) = expected.check_definition)
             AND (
                 expected.referenced_table IS NULL OR EXISTS (
                     SELECT 1
                     FROM actual_columns AS referenced
                     WHERE referenced.table_name = expected.referenced_table
                       AND referenced.column_name = 'id'
                       AND constraint_info.confrelid = referenced.table_id
                       AND constraint_info.confkey = ARRAY[referenced.column_number]
                 )
             )
       )
FROM expected_constraints AS expected
UNION ALL
SELECT 'RLS public.' || expected.table_name,
       EXISTS (
           SELECT 1
           FROM actual_tables AS actual
           WHERE actual.table_name = expected.table_name AND actual.relrowsecurity = true
       )
FROM expected_tables AS expected
ORDER BY verificacion;
