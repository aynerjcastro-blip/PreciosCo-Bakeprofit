"""
Scraper de PreciosCo sobre la API pública de VTEX (reemplaza al de Selenium).

Solo escribe datos crudos en `scraped_offers` (ADR-03). No elige ganador, no normaliza el EAN,
no filtra por rango de tamaño, no crea productos y no toca `prices`: todo eso es del backend Java.

Precios de Valledupar (DECISIONS.md, "Precios regionales"): la búsqueda de VTEX devuelve el precio
del vendedor por defecto, que no siempre es el de Valledupar. En las tiendas regionales, el precio
y la disponibilidad salen de una simulación de compra con el vendedor que atiende Valledupar; un
producto que ese vendedor no vende no se guarda. D1 no tiene vendedores por ciudad: se usa el precio
de la búsqueda.

Variables de entorno: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
                      (opcional) DB_SSLMODE, por ejemplo "require" para Supabase.

Uso:
    pip install -r requirements.txt
    python -m unittest test_scraper_supermercados   # pruebas, sin red ni base de datos
    python scraper_supermercados.py

Código de salida: 0 si todo salió bien; 1 si hubo errores o si alguna tienda no insertó ninguna
oferta (alerta de cero resultados: GitHub Actions marca la corrida como fallida); 2 si no hay
conexión o configuración.

Patrón de diseño (DECISIONS.md): Strategy. `TiendaStrategy` define cómo se consulta una tienda y
`VtexStrategy` es la implementación para las cuatro tiendas actuales; otra plataforma (Shopify,
HTML) sería otra estrategia sin tocar el flujo principal.
"""
import html
import json
import logging
import os
import re
import sys
import time
import unicodedata
from urllib.parse import quote, urlencode
from abc import ABC, abstractmethod
from collections import OrderedDict
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
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

# Ubicación de los precios: centro de Valledupar, como VTEX espera las coordenadas (longitud, latitud)
CIUDAD = "Valledupar"
COORDENADAS_CIUDAD = (-73.2532, 10.4631)
POLITICA_COMERCIAL = 1      # canal de venta (sc) de la tienda en línea
LOTE_SIMULACION = 50        # productos por simulación (una búsqueda trae hasta 50)

# Colombia no tiene horario de verano: UTC-5 fijo. Así scraped_at queda en la misma hora local que
# usa el backend, aunque el scraper corra en un runner de GitHub Actions (UTC) o en Windows (sin tzdata).
HORA_COLOMBIA = timezone(timedelta(hours=-5))

log = logging.getLogger("scraper_vtex")


def _pedir(session, metodo, url, **kwargs):
    """Petición con reintentos ante error de red, 429 o 5xx. Devuelve el JSON de la respuesta."""
    ultimo_error = None
    for intento in range(REINTENTOS + 1):
        if intento:
            time.sleep(PAUSA * (2 ** intento))
        try:
            r = session.request(metodo, url, timeout=TIMEOUT, **kwargs)
        except requests.RequestException as e:
            ultimo_error = f"error de red: {e}"
            continue
        if r.status_code in STATUS_OK:
            try:
                return r.json()
            except ValueError:
                raise RuntimeError(f"la respuesta de {url} no es JSON")
        if r.status_code == 429 or r.status_code >= 500:
            ultimo_error = f"HTTP {r.status_code}"
            continue
        raise RuntimeError(f"HTTP {r.status_code} en {r.url} -> {r.text[:150]!r}")
    raise RuntimeError(ultimo_error or "sin respuesta")


# ─────────────────────────── Tiendas (Strategy) ───────────────────────────

class TiendaStrategy(ABC):
    def __init__(self, base_url, regional):
        self.base_url = base_url.rstrip("/")
        self.regional = regional

    @abstractmethod
    def buscar(self, session, termino, fq):
        """Devuelve la lista de productos crudos (dicts) de la tienda."""

    @abstractmethod
    def precios_locales(self, session, items):
        """
        items: lista de (sku, vendedores de la búsqueda). Devuelve {sku: (precio, precio_lista)}
        con los productos que se venden en la ciudad; los que no están quedan fuera.
        """


class VtexStrategy(TiendaStrategy):
    RUTA_BUSQUEDA = "/api/catalog_system/pub/products/search"
    RUTA_REGIONES = "/api/checkout/pub/regions"
    RUTA_SIMULACION = "/api/checkout/pub/orderForms/simulation"

    def __init__(self, base_url, regional=True):
        super().__init__(base_url, regional)
        self._vendedores = None     # vendedores de la ciudad, se consultan una vez por corrida

    def buscar(self, session, termino, fq):
        params = {"ft": termino, "_from": 0, "_to": RESULTADOS_POR_CATEGORIA - 1}
        if fq:
            params["fq"] = fq
        # Los espacios van como %20 (requests usa '+', y varias tiendas VTEX responden 400 con "agua+sin+gas")
        url = self.base_url + self.RUTA_BUSQUEDA + "?" + urlencode(params, quote_via=quote, safe=":/")
        data = _pedir(session, "GET", url)
        if not isinstance(data, list):
            raise RuntimeError("la respuesta no es una lista de productos")
        return data

    def vendedores_locales(self, session):
        """Vendedores que atienden la ciudad, según las coordenadas (VTEX asigna uno por ciudad)."""
        if self._vendedores is None:
            lng, lat = COORDENADAS_CIUDAD
            params = {"country": "COL", "geoCoordinates": f"{lng};{lat}"}
            url = self.base_url + self.RUTA_REGIONES + "?" + urlencode(params, quote_via=quote, safe=";")
            regiones = _pedir(session, "GET", url)
            vendedores = []
            for region in regiones if isinstance(regiones, list) else []:
                for vendedor in region.get("sellers") or []:
                    if vendedor.get("id") and vendedor["id"] not in vendedores:
                        vendedores.append(vendedor["id"])
            if not vendedores:
                raise RuntimeError(f"la tienda no tiene vendedor para {CIUDAD}")
            self._vendedores = vendedores
        return self._vendedores

    def simular(self, session, skus, vendedor):
        """Simula la compra de cada sku con ese vendedor, entregado en la ciudad."""
        lng, lat = COORDENADAS_CIUDAD
        cuerpo = {
            "items": [{"id": sku, "quantity": 1, "seller": vendedor} for sku in skus],
            "country": "COL",
            "geoCoordinates": [lng, lat],
        }
        url = f"{self.base_url}{self.RUTA_SIMULACION}?sc={POLITICA_COMERCIAL}"
        return _pedir(session, "POST", url, data=json.dumps(cuerpo),
                      headers={"Content-Type": "application/json"})

    def precios_locales(self, session, items):
        pendientes = OrderedDict((sku, vendedores) for sku, vendedores in items)
        precios = {}

        def intentar(skus, vendedor):
            for i in range(0, len(skus), LOTE_SIMULACION):
                lote = skus[i:i + LOTE_SIMULACION]
                encontrados = precios_de_simulacion(self.simular(session, lote, vendedor), lote)
                for sku, precio in encontrados.items():
                    precios[sku] = precio
                    pendientes.pop(sku, None)
                time.sleep(PAUSA)

        # 1. El vendedor de la ciudad (las tiendas propias de Jumbo y Olímpica)
        for vendedor in self.vendedores_locales(session):
            if pendientes:
                intentar(list(pendientes), vendedor)
        # 2. El vendedor de la búsqueda (marketplace), si entrega en la ciudad
        por_vendedor = OrderedDict()
        for sku, vendedores in pendientes.items():
            for vendedor in vendedores:
                if vendedor not in self._vendedores:
                    por_vendedor.setdefault(vendedor, []).append(sku)
                    break
        for vendedor, skus in por_vendedor.items():
            intentar(skus, vendedor)
        return precios


def precios_de_simulacion(respuesta, skus):
    """
    {sku: (precio, precio_lista)} con los ítems disponibles de una simulación. VTEX devuelve los
    precios en centavos; requestIndex dice a qué ítem pedido corresponde cada uno.
    """
    precios = {}
    for posicion, item in enumerate((respuesta or {}).get("items") or []):
        indice = item.get("requestIndex", posicion)
        if not isinstance(indice, int) or not 0 <= indice < len(skus):
            continue
        if item.get("availability") != "available":
            continue
        precio = _centavos(item.get("sellingPrice"))
        if precio is None or precio <= 0:
            continue
        lista = _centavos(item.get("listPrice"))
        precios[skus[indice]] = (precio, lista if lista and lista > 0 else None)
    return precios


def _centavos(v):
    if v is None:
        return None
    try:
        return (Decimal(str(v)) / 100).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    except InvalidOperation:
        return None


# Clave (dentro del nombre de la tienda, sin tildes y en minúsculas) -> estrategia
TIENDAS_VTEX = OrderedDict([
    ("d1",       VtexStrategy("https://www.d1.com.co", regional=False)),   # precio nacional, sin vendedores por ciudad
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
    # Onzas: su unidad base depende del producto (líquido -> ML, sólido -> G); se resuelve con la unidad
    # esperada del scrape_target (ver parsear_tamano). Sin esa pista, el tamaño queda NULL.
    "oz": ("OZ", 1), "onza": ("OZ", 1), "onzas": ("OZ", 1),
}
_OZ_A_BASE = {"ML": Decimal("29.5735"), "G": Decimal("28.3495")}   # fl oz -> ml, oz -> g


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
# Total entre paréntesis, como lo publica Éxito: "x6und (5400 ml)". Ya es el total del empaque.
_RE_TOTAL_PARENTESIS = re.compile(
    r"\(\s*(?P<num>\d+(?:[.,]\d+)*)\s*(?P<unidad>" + _ALT_PV + r")\s*\)"
)
# "6 x 1000 ml" o "6x1000ml": la x con espacios a los dos lados o sin ninguno. Con espacio solo
# antes ("Roa 10 x2.5kg") el número es parte del nombre del producto, no la cantidad del empaque.
_RE_PACK_ANTES = re.compile(r"(?<![\d.,])(\d{1,3})(?:\s+[x*]\s+|[x*])$")
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


def _a_base(num, unidad, unidad_esperada):
    """(cantidad en la unidad base, unidad base), o (None, None) si no se puede convertir."""
    base, factor = _UNIDADES[unidad]
    cantidad = _numero(num)
    if cantidad is None:
        return None, None
    if base == "OZ":
        if unidad_esperada not in _OZ_A_BASE:
            return None, None
        base, factor = unidad_esperada, _OZ_A_BASE[unidad_esperada]
    return cantidad * factor, base


def _redondear(total, base):
    total = total.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    if total <= 0 or total > MAX_NET_CONTENT:
        return None, None
    return total, base


def parsear_tamano(nombre, unidad_esperada=None):
    """
    Devuelve (net_content, unit_base) a partir del nombre, o (None, None) si no se puede leer.
    G, ML o UN. Un pack "6 x 1000 ml" o "1000 ml x6" da 6000 ML; un total entre paréntesis
    ("x6und (5400 ml)") se toma tal cual. unidad_esperada (la del scrape_target) solo sirve
    para interpretar las onzas.
    """
    texto = sin_tildes(nombre)

    m = _RE_TOTAL_PARENTESIS.search(texto)
    if m:
        total, base = _a_base(m.group("num"), m.group("unidad"), unidad_esperada)
        if total is not None:
            return _redondear(total, base)

    candidatos = list(_RE_TAMANO.finditer(texto))
    if not candidatos:
        return None, None
    # Prefiere peso/volumen sobre unidades sueltas ("Aceite 1 L 1 un" -> 1 L)
    elegido = next((m for m in candidatos if _UNIDADES[m.group("unidad")][0] != "UN"), candidatos[0])
    total, base = _a_base(elegido.group("num"), elegido.group("unidad"), unidad_esperada)
    if total is None:
        return None, None

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

    return _redondear(total, base)


_RE_ETIQUETA_TAMANO = re.compile(r"(?:presentacion|contenido(?:\s+neto)?)\s*:\s*([^\n.]{0,40})")


def tamano_desde_descripcion(descripcion, unidad_esperada=None):
    """
    Respaldo cuando el nombre no trae tamaño (p. ej. marketplace de Éxito: la ficha dice
    "<strong>Presentación:</strong> 500ml"). Solo se acepta si el tamaño va tras la etiqueta
    "Presentación:" o "Contenido (neto):"; no se busca tamaño suelto en el texto libre.
    """
    if not descripcion:
        return None, None
    texto = sin_tildes(html.unescape(re.sub(r"<[^>]+>", " ", descripcion)))
    texto = re.sub(r"\s+", " ", texto)
    m = _RE_ETIQUETA_TAMANO.search(texto)
    return parsear_tamano(m.group(1), unidad_esperada) if m else (None, None)


# ─────────────────────────── Producto VTEX -> fila ───────────────────────────

def oferta_del_producto(p):
    item = (p.get("items") or [{}])[0]
    seller = (item.get("sellers") or [{}])[0]
    return item, seller.get("commertialOffer") or {}


def vendedores_del_item(item):
    return [s["sellerId"] for s in item.get("sellers") or [] if s.get("sellerId")]


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


def construir_fila(p, target, ahora, precio_local=None):
    """
    Devuelve la tupla para scraped_offers, o None si el producto no tiene precio ni nombre.
    precio_local: (precio, precio_lista) de la ciudad; sin él se usa el precio de la búsqueda.
    """
    item, oferta = oferta_del_producto(p)
    nombre = (p.get("productName") or "").strip()
    if precio_local is not None:
        precio, lista = precio_local
    else:
        precio, lista = _dinero(oferta.get("Price")), _dinero(oferta.get("ListPrice"))
    if not nombre or precio is None:
        return None
    if lista is not None and lista <= 0:
        lista = None

    ean = (item.get("ean") or "").strip() or None      # crudo: el backend lo normaliza y valida
    if ean and len(ean) > 20:
        log.warning("EAN de más de 20 caracteres, se guarda NULL: %r", ean[:40])
        ean = None

    imagenes = item.get("images") or []
    imagen = imagenes[0].get("imageUrl") if imagenes else None
    marca = (p.get("brand") or "").strip()[:100] or None
    contenido, unidad = parsear_tamano(nombre, target.get("unit_base"))
    if contenido is None:
        contenido, unidad = tamano_desde_descripcion(p.get("description"), target.get("unit_base"))

    return (target["store_id"], target["id"], ean, nombre, marca, precio, lista,
            p.get("link"), imagen, contenido, unidad, ahora, "PENDING")


def ahora_colombia():
    """Hora local de Colombia sin zona, como la guardan las columnas TIMESTAMP del backend."""
    return datetime.now(HORA_COLOMBIA).replace(tzinfo=None)


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
    no_disponibles: int = 0     # sin stock o, en tiendas regionales, no se venden en la ciudad
    sin_precio: int = 0
    insertados: int = 0
    sin_tamano: int = 0
    error: str = ""


def _filas_regionales(session, estrategia, productos, target, ahora, r):
    items, por_sku = [], {}
    for p in productos:
        item, _ = oferta_del_producto(p)
        sku = item.get("itemId")
        if sku and sku not in por_sku:
            por_sku[sku] = p
            items.append((sku, vendedores_del_item(item)))
    precios = estrategia.precios_locales(session, items) if items else {}
    r.no_disponibles = len(productos) - len(precios)
    filas = []
    for sku, p in por_sku.items():
        if sku not in precios:
            continue
        try:
            fila = construir_fila(p, target, ahora, precios[sku])
        except Exception as e:      # un producto raro no tumba el resto
            log.warning("[%s][%s] producto omitido por error de lectura: %s", r.tienda, r.termino, e)
            continue
        if fila is None:
            r.sin_precio += 1
        else:
            filas.append(fila)
    return filas


def _filas_nacionales(productos, target, ahora, r):
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
        filas.append(fila)
    return filas


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
                    "(completar la categoría de la tienda en scrape_targets)", r.tienda, r.termino)

    ahora = ahora_colombia()
    try:
        productos = estrategia.buscar(session, target["search_term"], fq)
        r.encontrados = len(productos)
        if estrategia.regional:
            filas = _filas_regionales(session, estrategia, productos, target, ahora, r)
        else:
            filas = _filas_nacionales(productos, target, ahora, r)
    except Exception as e:
        r.error = f"petición fallida: {e}"
        log.error("[%s][%s] %s", r.tienda, r.termino, r.error)
        return r, True

    r.sin_tamano = sum(1 for f in filas if f[9] is None)

    if productos and r.no_disponibles == len(productos):
        log.warning("[%s][%s] ninguno de los %d productos está disponible%s", r.tienda, r.termino,
                    len(productos), f" en {CIUDAD}" if estrategia.regional else "")

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


def tiendas_sin_ofertas(resumenes):
    """Tiendas que no insertaron ninguna oferta en toda la corrida (alerta de cero resultados)."""
    insertados = OrderedDict()
    for r in resumenes:
        insertados[r.tienda] = insertados.get(r.tienda, 0) + r.insertados
    return [tienda for tienda, total in insertados.items() if total == 0]


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
        log.info("%d scrape_targets activos; precios de %s", len(targets), CIUDAD)
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
    vacias = tiendas_sin_ofertas(resumenes)
    for tienda in vacias:
        log.error("ALERTA: %s no insertó ninguna oferta en esta corrida; revisar su API y sus scrape_targets",
                  tienda)
    return 1 if vacias or any(r.error for r in resumenes) else 0


if __name__ == "__main__":
    sys.exit(main())