"""
Scraper de PreciosCo sobre la API pública de VTEX (reemplaza al de Selenium).

Solo escribe datos crudos en `scraped_offers` (ADR-03). No elige ganador, no normaliza el EAN,
no filtra por rango de tamaño, no crea productos y no toca `prices`: todo eso es del backend Java.

Variables de entorno: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
                      (opcional) DB_SSLMODE, por ejemplo "require" para Supabase.

Uso:
    pip install -r requirements.txt
    python scraper_vtex.py

Patrón de diseño (DECISIONS.md): Strategy. `TiendaStrategy` define cómo se consulta una tienda y
`VtexStrategy` es la implementación para las cuatro tiendas actuales; otra plataforma (Shopify,
HTML) sería otra estrategia sin tocar el flujo principal.
"""
import logging
import os
import re
import sys
import time
import unicodedata
from abc import ABC, abstractmethod
from collections import OrderedDict
from dataclasses import dataclass
from datetime import datetime
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP

import psycopg2
import psycopg2.extras
import requests

USER_AGENT = "PreciosCo-proyecto-academico/1.0"
PAUSA = 1.5                 # segundos entre peticiones
RESULTADOS_POR_CATEGORIA = 50   # _from=0, _to=49
TIMEOUT = 30
REINTENTOS = 2              # reintentos ante error de red, 429 o 5xx
STATUS_OK = (200, 206)      # VTEX responde 206 cuando se usa _from/_to
MAX_NET_CONTENT = Decimal("99999999.99")   # NUMERIC(10,2)

log = logging.getLogger("scraper_vtex")


# ─────────────────────────── Tiendas (Strategy) ───────────────────────────

class TiendaStrategy(ABC):
    def __init__(self, base_url):
        self.base_url = base_url.rstrip("/")

    @abstractmethod
    def buscar(self, session, termino, fq):
        """Devuelve la lista de productos crudos (dicts) de la tienda."""


class VtexStrategy(TiendaStrategy):
    RUTA = "/api/catalog_system/pub/products/search"

    def buscar(self, session, termino, fq):
        params = {"ft": termino, "_from": 0, "_to": RESULTADOS_POR_CATEGORIA - 1}
        if fq:
            params["fq"] = fq
        url = self.base_url + self.RUTA
        ultimo_error = None
        for intento in range(REINTENTOS + 1):
            if intento:
                time.sleep(PAUSA * (2 ** intento))
            try:
                r = session.get(url, params=params, timeout=TIMEOUT)
            except requests.RequestException as e:
                ultimo_error = f"error de red: {e}"
                continue
            if r.status_code in STATUS_OK:
                try:
                    data = r.json()
                except ValueError:
                    raise RuntimeError("la respuesta no es JSON")
                if not isinstance(data, list):
                    raise RuntimeError("la respuesta no es una lista de productos")
                return data
            if r.status_code == 429 or r.status_code >= 500:
                ultimo_error = f"HTTP {r.status_code}"
                continue
            raise RuntimeError(f"HTTP {r.status_code}")
        raise RuntimeError(ultimo_error or "sin respuesta")


# Clave (dentro del nombre de la tienda, sin tildes y en minúsculas) -> estrategia
TIENDAS_VTEX = OrderedDict([
    ("d1",       VtexStrategy("https://www.d1.com.co")),
    ("exito",    VtexStrategy("https://www.exito.com/io")),   # Éxito cuelga de /io/
    ("jumbo",    VtexStrategy("https://www.jumbocolombia.com")),
    ("olimpica", VtexStrategy("https://www.olimpica.com")),
])


def sin_tildes(t):
    t = unicodedata.normalize("NFD", (t or "").lower())
    return "".join(c for c in t if unicodedata.category(c) != "Mn")


def estrategia_para(nombre_tienda):
    n = sin_tildes(nombre_tienda)
    for clave, estrategia in TIENDAS_VTEX.items():
        if clave in n:
            return estrategia
    return None


# ─────────────────────────── Tamaño desde el nombre ───────────────────────────

# unidad -> (unidad base, factor)
_UNIDADES = {
    "kg": ("G", 1000), "kgs": ("G", 1000), "kilo": ("G", 1000), "kilos": ("G", 1000),
    "kilogramo": ("G", 1000), "kilogramos": ("G", 1000),
    "g": ("G", 1), "gr": ("G", 1), "grs": ("G", 1), "gramo": ("G", 1), "gramos": ("G", 1),
    "lb": ("G", 500), "lbs": ("G", 500), "libra": ("G", 500), "libras": ("G", 500),
    "l": ("ML", 1000), "lt": ("ML", 1000), "lts": ("ML", 1000),
    "litro": ("ML", 1000), "litros": ("ML", 1000),
    "ml": ("ML", 1), "cc": ("ML", 1), "mililitro": ("ML", 1), "mililitros": ("ML", 1),
    "un": ("UN", 1), "und": ("UN", 1), "unds": ("UN", 1), "unid": ("UN", 1),
    "unidad": ("UN", 1), "unidades": ("UN", 1), "uds": ("UN", 1),
    "rollo": ("UN", 1), "rollos": ("UN", 1),      # papel higiénico, toallas de cocina
}


def _alt(claves):
    return "|".join(sorted(claves, key=len, reverse=True))


_UNIDADES_UN = [u for u, (b, _) in _UNIDADES.items() if b == "UN"]
_UNIDADES_PESO_VOL = [u for u, (b, _) in _UNIDADES.items() if b != "UN"]
_ALT_UN = _alt(_UNIDADES_UN)
_ALT_PV = _alt(_UNIDADES_PESO_VOL)

# Número + unidad. Acepta "x5kg" (la x pegada al número) pero no letras antes ("b2l").
_RE_TAMANO = re.compile(
    r"(?<![\d.,])(?:(?<![a-z])|(?<=x))(?P<num>\d+(?:[.,]\d+)*)\s*(?P<unidad>"
    + _alt(_UNIDADES) + r")(?![a-z])"
)
_RE_PACK_ANTES = re.compile(r"(?<![\d.,])(\d{1,3})\s*[x*]\s*$")                      # "6 x 1000 ml"
_RE_PACK_DESPUES = re.compile(                                                         # "900 ml x6", "x 6 und"
    r"^\s*[x*]\s*(\d{1,3})(?!\d|[.,]\d)(?!\s*(?:" + _ALT_PV + r")(?![a-z]))"
)
_RE_PACK_TEXTO = re.compile(
    r"(?<![\d.,])(\d{1,3})\s*(?:" + _ALT_UN + r")(?![a-z])|\bpack\s*(?:de|x)?\s*(\d{1,3})(?!\d)"
)


def _numero(txt):
    """Convierte '2.500', '2,5', '1.5', '0.500' en Decimal. Punto/coma de miles solo si hay grupos de 3."""
    try:
        if re.fullmatch(r"[1-9]\d{0,2}([.,]\d{3})+", txt):
            return Decimal(re.sub(r"[.,]", "", txt))
        return Decimal(txt.replace(",", "."))
    except InvalidOperation:
        return None


def parsear_tamano(nombre):
    """
    Devuelve (net_content, unit_base) a partir del nombre, o (None, None) si no se puede leer.
    G, ML o UN. Un pack "6 x 1000 ml" o "1000 ml x6" da 6000 ML.
    """
    texto = sin_tildes(nombre)
    candidatos = [m for m in _RE_TAMANO.finditer(texto)]
    if not candidatos:
        return None, None
    # Prefiere peso/volumen sobre unidades sueltas ("Aceite 1 L 1 un" -> 1 L)
    elegido = next((m for m in candidatos if _UNIDADES[m.group("unidad")][0] != "UN"), candidatos[0])
    base, factor = _UNIDADES[elegido.group("unidad")]
    cantidad = _numero(elegido.group("num"))
    if cantidad is None:
        return None, None
    total = cantidad * factor

    if base != "UN":
        pack = None
        m = _RE_PACK_ANTES.search(texto[:elegido.start()])
        if m:
            pack = int(m.group(1))
        else:
            m = _RE_PACK_DESPUES.match(texto[elegido.end():])
            if m:
                pack = int(m.group(1))
            else:
                resto = texto[:elegido.start()] + " " + texto[elegido.end():]
                m = _RE_PACK_TEXTO.search(resto)
                if m:
                    pack = int(m.group(1) or m.group(2))
        if pack and pack > 1:
            total *= pack

    total = total.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    if total <= 0 or total > MAX_NET_CONTENT:
        return None, None
    return total, base


# ─────────────────────────── Producto VTEX -> fila ───────────────────────────

def oferta_del_producto(p):
    item = (p.get("items") or [{}])[0]
    seller = (item.get("sellers") or [{}])[0]
    return item, seller.get("commertialOffer") or {}


def _stock(oferta):
    """La API pública de VTEX expone el stock como AvailableQuantity; se acepta Stock si existe."""
    for campo in ("Stock", "AvailableQuantity"):
        v = oferta.get(campo)
        if isinstance(v, (int, float)) and not isinstance(v, bool):
            return v
    return None


def esta_disponible(oferta):
    stock = _stock(oferta)
    return oferta.get("IsAvailable") is True and stock is not None and stock > 0


def _dinero(v):
    if v is None:
        return None
    try:
        return Decimal(str(v)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    except InvalidOperation:
        return None


def construir_fila(p, target, ahora):
    """Devuelve la tupla para scraped_offers, o None si el producto no tiene precio ni nombre."""
    item, oferta = oferta_del_producto(p)
    nombre = (p.get("productName") or "").strip()
    precio = _dinero(oferta.get("Price"))
    if not nombre or precio is None:
        return None

    ean = (item.get("ean") or "").strip() or None      # crudo: el backend lo normaliza y valida
    if ean and len(ean) > 20:
        log.warning("EAN de más de 20 caracteres, se guarda NULL: %r", ean[:40])
        ean = None

    lista = _dinero(oferta.get("ListPrice"))
    if lista is not None and lista <= 0:
        lista = None

    imagenes = item.get("images") or []
    imagen = imagenes[0].get("imageUrl") if imagenes else None
    marca = (p.get("brand") or "").strip()[:100] or None
    contenido, unidad = parsear_tamano(nombre)

    return (target["store_id"], target["id"], ean, nombre, marca, precio, lista,
            p.get("link"), imagen, contenido, unidad, ahora, "PENDING")


# ─────────────────────────── Base de datos ───────────────────────────

SQL_TARGETS = """
    SELECT t.id, t.store_id, s.name AS store_name, t.search_term, t.store_category_fq,
           t.unit_base, t.min_content, t.max_content
    FROM scrape_targets t
    JOIN stores s ON s.id = t.store_id
    WHERE t.active = true
    ORDER BY t.store_id, t.id
"""

SQL_INSERT = """
    INSERT INTO scraped_offers
        (store_id, scrape_target_id, ean, name, brand_name, price, list_price,
         product_url, image_url, net_content, unit_base, scraped_at, status)
    VALUES %s
"""


def conectar():
    return psycopg2.connect(
        host=os.environ["DB_HOST"],
        port=os.environ.get("DB_PORT", "5432"),
        dbname=os.environ["DB_NAME"],
        user=os.environ["DB_USER"],
        password=os.environ["DB_PASSWORD"],
        sslmode=os.environ.get("DB_SSLMODE", "prefer"),
    )


def leer_targets(conn):
    with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
        cur.execute(SQL_TARGETS)
        return cur.fetchall()


# ─────────────────────────── Flujo principal ───────────────────────────

@dataclass
class Resumen:
    tienda: str
    termino: str
    encontrados: int = 0
    no_disponibles: int = 0
    sin_precio: int = 0
    insertados: int = 0
    sin_tamano: int = 0
    error: str = ""


def procesar_target(conn, session, target):
    r = Resumen(target["store_name"], target["search_term"])
    estrategia = estrategia_para(target["store_name"])
    if estrategia is None:
        r.error = "tienda sin estrategia/URL configurada"
        log.error("[%s][%s] %s", r.tienda, r.termino, r.error)
        return r, False   # False: no se hizo petición, no hace falta pausa

    fq = target["store_category_fq"]
    if not fq:
        log.warning("[%s][%s] sin store_category_fq: la búsqueda solo por texto devuelve basura "
                    "(completar con verificar_categorias_vtex.py)", r.tienda, r.termino)

    try:
        productos = estrategia.buscar(session, target["search_term"], fq)
    except Exception as e:
        r.error = f"petición fallida: {e}"
        log.error("[%s][%s] %s", r.tienda, r.termino, r.error)
        return r, True

    r.encontrados = len(productos)
    ahora = datetime.now()
    filas = []
    for p in productos:
        try:
            _, oferta = oferta_del_producto(p)
            if not esta_disponible(oferta):
                r.no_disponibles += 1
                continue
            fila = construir_fila(p, target, ahora)
        except Exception as e:      # un producto raro no tumba el resto
            log.warning("[%s][%s] producto omitido por error de lectura: %s", r.tienda, r.termino, e)
            continue
        if fila is None:
            r.sin_precio += 1
            continue
        if fila[9] is None:
            r.sin_tamano += 1
        filas.append(fila)

    if productos and r.no_disponibles == len(productos):
        _, o = oferta_del_producto(productos[0])
        log.warning("[%s][%s] todos no disponibles; ejemplo de commertialOffer: IsAvailable=%r Stock=%r "
                    "AvailableQuantity=%r Price=%r (campos: %s)", r.tienda, r.termino,
                    o.get("IsAvailable"), o.get("Stock"), o.get("AvailableQuantity"), o.get("Price"),
                    ", ".join(sorted(o.keys()))[:300])

    if filas:
        try:
            with conn.cursor() as cur:
                psycopg2.extras.execute_values(cur, SQL_INSERT, filas, page_size=100)
            conn.commit()
            r.insertados = len(filas)
        except Exception as e:
            conn.rollback()
            r.error = f"INSERT fallido: {e}"
            r.sin_tamano = 0
            log.error("[%s][%s] %s", r.tienda, r.termino, r.error)
            return r, True

    log.info("[%s][%s] encontrados=%d no_disponibles=%d insertados=%d sin_tamano=%d",
             r.tienda, r.termino, r.encontrados, r.no_disponibles, r.insertados, r.sin_tamano)
    return r, True


def imprimir_resumen(resumenes):
    log.info("=" * 96)
    log.info("%-12s %-14s %10s %14s %10s %10s %10s", "Tienda", "Término", "Encontr.",
             "No disponibles", "Sin precio", "Insertados", "Sin tamaño")
    for r in resumenes:
        log.info("%-12s %-14s %10d %14d %10d %10d %10d%s", r.tienda, r.termino[:14], r.encontrados,
                 r.no_disponibles, r.sin_precio, r.insertados, r.sin_tamano,
                 f"   ERROR: {r.error}" if r.error else "")
    log.info("-" * 96)
    log.info("%-27s %10d %14d %10d %10d %10d", "TOTAL",
             sum(r.encontrados for r in resumenes), sum(r.no_disponibles for r in resumenes),
             sum(r.sin_precio for r in resumenes), sum(r.insertados for r in resumenes),
             sum(r.sin_tamano for r in resumenes))


def main():
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)-7s %(message)s",
                        datefmt="%Y-%m-%d %H:%M:%S")
    try:
        conn = conectar()
    except KeyError as e:
        log.error("Falta la variable de entorno %s", e)
        return 2
    except psycopg2.Error as e:
        log.error("No se pudo conectar a la base de datos: %s", e)
        return 2

    session = requests.Session()
    session.headers.update({"User-Agent": USER_AGENT, "Accept": "application/json"})
    resumenes = []
    try:
        targets = leer_targets(conn)
        log.info("%d scrape_targets activos", len(targets))
        for i, target in enumerate(targets):
            try:
                resumen, hubo_peticion = procesar_target(conn, session, target)
            except Exception as e:      # ninguna falla de un target detiene a los demás
                conn.rollback()
                resumen = Resumen(target["store_name"], target["search_term"], error=f"inesperado: {e}")
                hubo_peticion = True
                log.exception("[%s][%s] error inesperado", resumen.tienda, resumen.termino)
            resumenes.append(resumen)
            if hubo_peticion and i < len(targets) - 1:
                time.sleep(PAUSA)
    finally:
        conn.close()

    imprimir_resumen(resumenes)
    return 1 if any(r.error for r in resumenes) else 0


if __name__ == "__main__":
    sys.exit(main())