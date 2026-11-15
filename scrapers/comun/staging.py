"""
Acceso a la base de datos: scrape_targets de un dominio y escritura cruda en scraped_offers (ADR-03).

El dominio es la categoría raíz de la tienda (stores.category_id apunta a una raíz: "Supermercado",
"Droguería", ...). Cada scraper lee solo los targets de su dominio, así un target activo de otro
dominio no aparece como "tienda sin estrategia" ni dispara su alerta.
"""
import os

import psycopg2
import psycopg2.extras

from .texto import sin_tildes

SQL_DOMINIO = """
    SELECT 1 FROM categories WHERE parent_id IS NULL AND lower(name) = lower(%s)
"""

SQL_TARGETS = """
    SELECT t.id, t.store_id, s.name AS store_name, t.search_term, t.store_category_fq,
           t.unit_base, t.min_content, t.max_content,
           to_jsonb(t) ->> 'target_url' AS target_url   -- NULL también si la columna aún no existe
    FROM scrape_targets t
    JOIN stores s ON s.id = t.store_id
    JOIN categories raiz ON raiz.id = s.category_id
    WHERE t.active = true AND lower(raiz.name) = lower(%s)
    ORDER BY t.store_id, t.id
"""

SQL_INSERT = """
    INSERT INTO scraped_offers
        (store_id, scrape_target_id, ean, name, brand_name, price, list_price,
         product_url, image_url, net_content, unit_base, scraped_at, status)
    VALUES %s
"""

# Posición de net_content en la tupla de SQL_INSERT (para contar las filas sin tamaño)
POS_NET_CONTENT = 9


class DominioInexistente(ValueError):
    """El nombre de dominio del scraper no coincide con ninguna categoría raíz."""


def conectar():
    return psycopg2.connect(
        host=os.environ["DB_HOST"],
        port=os.environ.get("DB_PORT", "5432"),
        dbname=os.environ["DB_NAME"],
        user=os.environ["DB_USER"],
        password=os.environ["DB_PASSWORD"],
        sslmode=os.environ.get("DB_SSLMODE", "prefer"),
    )


def leer_targets(conn, dominio):
    """Targets activos de las tiendas del dominio. Falla si el dominio no es una categoría raíz."""
    with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
        cur.execute(SQL_DOMINIO, (dominio,))
        if cur.fetchone() is None:
            raise DominioInexistente(f"no hay una categoría raíz llamada {dominio!r}")
        cur.execute(SQL_TARGETS, (dominio,))
        return cur.fetchall()


def filtrar_targets(targets, solo_tiendas):
    """Deja solo los targets de las tiendas pedidas en SOLO_TIENDAS ("exito,jumbo"); vacío = todos."""
    pedidas = [sin_tildes(t).strip() for t in (solo_tiendas or "").split(",") if t.strip()]
    if not pedidas:
        return targets
    return [t for t in targets if any(p in sin_tildes(t["store_name"]) for p in pedidas)]


def insertar_ofertas(conn, filas):
    with conn.cursor() as cur:
        psycopg2.extras.execute_values(cur, SQL_INSERT, filas, page_size=100)
    conn.commit()
