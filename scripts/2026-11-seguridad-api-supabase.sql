-- SCRUM-297 · Cerrar el acceso de la API REST de Supabase (roles anon y authenticated) a todas las tablas.
-- El backend se conecta como postgres, dueño de las tablas: RLS no le aplica y los REVOKE no lo afectan.
-- Activar RLS dos veces no da error, así que el script se puede volver a correr sin problema.
BEGIN;

ALTER TABLE users            ENABLE ROW LEVEL SECURITY;
ALTER TABLE categories       ENABLE ROW LEVEL SECURITY;
ALTER TABLE stores           ENABLE ROW LEVEL SECURITY;
ALTER TABLE products         ENABLE ROW LEVEL SECURITY;
ALTER TABLE prices           ENABLE ROW LEVEL SECURITY;
ALTER TABLE ratings          ENABLE ROW LEVEL SECURITY;
ALTER TABLE comments         ENABLE ROW LEVEL SECURITY;
ALTER TABLE comment_reports  ENABLE ROW LEVEL SECURITY;
ALTER TABLE follows          ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications    ENABLE ROW LEVEL SECURITY;
-- Tablas de HT-09 (por si su script se corrió sin las líneas de RLS)
ALTER TABLE brands           ENABLE ROW LEVEL SECURITY;
ALTER TABLE scrape_targets   ENABLE ROW LEVEL SECURITY;
ALTER TABLE scraped_offers   ENABLE ROW LEVEL SECURITY;

-- Segunda barrera: sin permisos, aunque alguien desactive RLS por error
REVOKE ALL ON ALL TABLES    IN SCHEMA public FROM anon, authenticated;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM anon, authenticated;

-- Que las tablas nuevas tampoco nazcan con esos permisos
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES    FROM anon, authenticated;
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM anon, authenticated;

COMMIT;
