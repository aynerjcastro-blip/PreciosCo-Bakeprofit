"""
Scraper de Cuidado Facial (Cutis, Bellapiel, Medipiel) -> tabla scraped_offers.

Solo escribe datos crudos (ADR-03). NO elige ganador, NO normaliza ni valida el EAN,
NO crea productos, NO toca `prices`, NO filtra por min_content/max_content: todo eso
es trabajo del procesador de staging en Java.

Variables de entorno:
    DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD   (obligatorias, excepto DB_PORT=5432)
    DB_SSLMODE            opcional, por defecto "prefer" (Supabase suele pedir "require")
    SOLO_TIENDAS          opcional, p. ej. "cutis,medipiel" (por defecto las tres)
    MAX_PAGINAS           opcional, páginas de 50 productos por target (por defecto 4)
    SCRAPER_USER_AGENT    opcional, User-Agent que identifica al proyecto
    MOSTRAR_DESCARTES     opcional, "1" imprime los descartes de cada target en el log (por defecto
                          "1" en GitHub Actions y "0" en local); "0" lo apaga
    DESCARTADOS_PATH      opcional, dónde se guarda el CSV de descartes (por defecto descartados.csv)
    SIN_TAMANO_PATH       opcional, CSV con las ofertas insertadas sin net_content/unit_base y por qué
                          (por defecto sin_tamano.csv)

Uso:
    pip install -r requirements.txt
    python scraper_cuidado_facial.py

Código de salida 1 si una tienda no inserta ninguna oferta en toda la corrida
(alerta de cero resultados, ver DECISIONS.md) o si no hay targets activos.
"""
import csv
import os
import re
import sys
import time
import unicodedata
from collections import Counter, OrderedDict
from datetime import datetime
from urllib.parse import quote, urlencode, urlparse
from urllib.robotparser import RobotFileParser

import psycopg2
import psycopg2.extras
import requests

# ─────────────────────────── Configuración ───────────────────────────
TIENDAS = {  # nombre en la tabla stores (minúsculas) -> URL base
    "cutis": "https://www.cutis.com.co",
    "bellapiel": "https://www.bellapiel.com.co",
    "medipiel": "https://www.medipiel.com.co",
}
API_PATH = "/api/catalog_system/pub/products/search"
POR_PAGINA = 50                      # VTEX permite como máximo 50 por petición (_from/_to)
MAX_PAGINAS = int(os.getenv("MAX_PAGINAS", "4"))
PAUSA = 1.5                          # segundos entre peticiones (mínimo; sube si hay Crawl-delay)
MAX_PAUSA_ROBOTS = 60                # si el Crawl-delay supera esto, se omite la tienda
UMBRAL_SIN_TAMANO = 30             # % de ofertas sin tamaño por tienda que dispara un aviso
BLOQUEOS_MAX = 3                     # 403/429 seguidos antes de cortar la tienda
TIMEOUT = 30
OK = (200, 206)                      # VTEX responde 206 cuando se usa _from/_to
USER_AGENT = os.getenv(
    "SCRAPER_USER_AGENT",
    "PreciosCo-proyecto-academico (comparador de precios; scraper de cuidado facial)",
)


# ─────────────────────────── Texto y tamaños ───────────────────────────
def normalizar(t):
    t = unicodedata.normalize("NFD", (t or "").lower())
    return "".join(c for c in t if unicodedata.category(c) != "Mn")


# unidad escrita -> (unidad base, factor)
_U = {
    "ml": ("ML", 1), "cc": ("ML", 1),
    "l": ("ML", 1000), "lt": ("ML", 1000), "lts": ("ML", 1000),
    "litro": ("ML", 1000), "litros": ("ML", 1000),
    "g": ("G", 1), "gr": ("G", 1), "grs": ("G", 1), "gramo": ("G", 1), "gramos": ("G", 1),
    "kg": ("G", 1000), "kgs": ("G", 1000), "kilo": ("G", 1000), "kilos": ("G", 1000),
}
_UNIDADES_RE = "|".join(sorted(_U, key=len, reverse=True))
_NUM = r"\d+(?:[.,]\d+)*"
# (?<=\bx): tolera "x40ml" (la x pegada al número), que antes se perdía
_TAM = re.compile(r"(?:(?<![\w.,])|(?<=\bx))(" + _NUM + r")\s*(" + _UNIDADES_RE + r")(?![a-z])")
# En la descripción solo se acepta un tamaño precedido por una etiqueta de contenido
_DESC = re.compile(r"(?:contenido(?: neto)?|presentacion|tamano|volumen|peso(?: neto)?)(?: del producto)?"
                   r"\s*[:\-]?\s*(" + _NUM + r")\s*(" + _UNIDADES_RE + r")(?![a-z])")
# Packs, kits y combos: el tamaño es ambiguo (¿por unidad o total?) -> se deja NULL
_PACK = re.compile(r"(?<![\w.,])(?:[2-9]|1[0-2])\s*x\s*\d|\b(?:kit|pack|combo|duo|set|estuche)\b")
_CLAVES_SPEC = ("presentacion", "contenido", "tamano", "volumen", "peso", "cantidad", "capacidad", "neto")


def _numero(t):
    if re.fullmatch(r"\d{1,3}(?:\.\d{3})+", t):
        return float(t.replace(".", ""))          # 1.000 -> 1000
    if re.fullmatch(r"\d{1,3}(?:,\d{3})+", t):
        return float(t.replace(",", ""))
    if t.count(",") + t.count(".") <= 1:
        return float(t.replace(",", "."))         # 1,5 -> 1.5
    return None


def _tamanos_en(texto):
    """Conjunto de (unidad_base, cantidad) hallados en el texto; None si es un pack/kit."""
    t = normalizar(texto)
    if _PACK.search(t):
        return None
    hallados = set()
    for m in _TAM.finditer(t):
        n = _numero(m.group(1))
        base, factor = _U[m.group(2)]
        if n is None:
            continue
        valor = round(n * factor, 2)
        if 0 < valor < 10_000_000:
            hallados.add((base, valor))
    return hallados


def _valores(v):
    if isinstance(v, list):
        return [str(x) for x in v if x not in (None, "")]
    return [str(v)] if v not in (None, "") else []


def _textos_especificaciones(p):
    """Valores de las especificaciones de producto y de las variaciones del SKU que hablan de tamaño.
    Si el valor es solo un número y la clave trae la unidad (p. ej. 'Contenido (ml)': '50'), se une."""
    item = (p.get("items") or [{}])[0]
    claves = list(p.get("allSpecifications") or []) + list(item.get("variations") or [])
    claves.append("Tipo de presentación")
    vistos, textos = set(), []
    for k in claves:
        if not isinstance(k, str) or k in vistos:
            continue
        vistos.add(k)
        nk = normalizar(k)
        if not any(c in nk for c in _CLAVES_SPEC):
            continue
        unidad_k = re.search(r"\b(ml|g|gr|kg|l|lt|cc)\b", nk)
        for v in _valores(p.get(k)) + _valores(item.get(k)):
            if unidad_k and re.fullmatch(_NUM, v.strip()):
                v = f"{v.strip()} {unidad_k.group(1)}"
            textos.append(v)
    return textos


def _slug(p):
    """'.../heliocare-360-gel-fps-50-tubo-x-50-ml-2505/p' -> 'heliocare 360 gel fps 50 tubo x 50 ml 2505'.
    Muchas tiendas dejan el tamaño en la URL aunque el nombre ya no lo traiga."""
    ruta = urlparse(p.get("link") or "").path.strip("/")
    return ruta.split("/")[0].replace("-", " ") if ruta else ""


def _descripcion(p):
    t = " ".join(str(p.get(k) or "") for k in ("description", "metaTagDescription"))
    return normalizar(re.sub(r"<[^>]+>", " ", t))


def extraer_tamano(p):
    """(net_content, unit_base, detalle).
    detalle dice de dónde salió el tamaño (nombre, titulo, sku, spec, url, descripcion) o por qué no hay:
    pack (kit o '2 x 30 ml'), ambiguo (dos tamaños en el nombre), conflicto (las fuentes no coinciden)
    o sin_dato. Nunca se adivina: ante duda, NULL."""
    item = (p.get("items") or [{}])[0]
    nombre = p.get("productName") or ""
    s = _tamanos_en(nombre)
    if s is None:
        return None, None, "pack"
    if len(s) == 1:
        base, valor = next(iter(s))
        return valor, base, "nombre"
    if len(s) > 1:
        return None, None, "ambiguo"

    # El nombre no trae tamaño: se prueban otras fuentes y deben coincidir entre sí
    fuentes = []
    titulo = p.get("productTitle") or ""
    if titulo and titulo != nombre:
        fuentes.append(("titulo", titulo))
    for k in ("name", "nameComplete"):
        v = item.get(k) or ""
        if v and v != nombre:
            fuentes.append(("sku", v))
    fuentes += [("spec", t) for t in _textos_especificaciones(p)]
    fuentes.append(("url", _slug(p)))
    candidatos = []
    for fuente, texto in fuentes:
        s = _tamanos_en(texto)
        if s is None:
            return None, None, "pack"
        if len(s) == 1:
            candidatos.append((fuente, next(iter(s))))
    distintos = {c for _, c in candidatos}
    if len(distintos) == 1:
        base, valor = next(iter(distintos))
        return valor, base, candidatos[0][0]
    if len(distintos) > 1:
        return None, None, "conflicto: " + " vs ".join(f"{v:g} {b}({f})" for f, (b, v) in candidatos)

    # Último recurso: la descripción, solo con una etiqueta de contenido delante
    d = set()
    for m in _DESC.finditer(_descripcion(p)):
        n = _numero(m.group(1))
        if n is not None:
            base, factor = _U[m.group(2)]
            d.add((base, round(n * factor, 2)))
    if len(d) == 1:
        base, valor = next(iter(d))
        return valor, base, "descripcion"
    return None, None, "sin_dato"


def cumple_terminos(nombre, termino):
    """Respaldo cuando no hay fq: todas las palabras del término deben estar en el nombre
    (sin tildes; tolera el género/plural: 'limpiadora' acepta 'limpiador')."""
    palabras = re.findall(r"[a-z0-9]+", normalizar(nombre))
    for tok in re.findall(r"[a-z0-9]+", normalizar(termino)):
        if len(tok) <= 2:
            ok = tok in palabras
        else:
            stem = tok[:-1] if len(tok) >= 6 and tok[-1] in "aos" else tok
            ok = any(w.startswith(stem) for w in palabras)
        if not ok:
            return False
    return True


# Palabras que delatan productos de otra categoría cuando se busca sin fq. Solo se aplican si el
# término buscado no las contiene.
EXCLUIR = ("capilar", "cabello", "pestana", "ceja", "shampoo", "champu", "acondicionador")
# Por categoría del target: un sérum con protección solar se clasifica como protector solar
# (entra por los targets de la categoría de solares), no como sérum.
EXCLUIR_POR_CATEGORIA = {
    "Sérums faciales": ("solar", "spf", "fps"),
}


def excluido(nombre, termino, categoria=""):
    n, t = normalizar(nombre), normalizar(termino)
    palabras = EXCLUIR + EXCLUIR_POR_CATEGORIA.get(categoria, ())
    return any(re.search(r"\b" + x, n) and x not in t for x in palabras)


# ─────────────────────────── Cliente HTTP por tienda ───────────────────────────
class ErrorTienda(Exception):
    pass


class TiendaBloqueada(ErrorTienda):
    pass


class Cliente:
    def __init__(self, nombre, base):
        self.nombre, self.base = nombre, base
        self.url = base + API_PATH
        self.sesion = requests.Session()
        self.sesion.headers["User-Agent"] = USER_AGENT
        self.bloqueos = 0
        self.pausa = PAUSA

    def leer_robots(self):
        """Respeta el Crawl-delay de robots.txt (DECISIONS.md). Devuelve False si no se puede cumplir."""
        try:
            r = self.sesion.get(self.base + "/robots.txt", timeout=TIMEOUT)
            if r.status_code != 200:
                return True
            rp = RobotFileParser()
            rp.parse(r.text.splitlines())
            delay = rp.crawl_delay(USER_AGENT) or rp.crawl_delay("*")
        except requests.RequestException:
            return True
        finally:
            time.sleep(PAUSA)
        if delay:
            if delay > MAX_PAUSA_ROBOTS:
                print(f"  ⛔ {self.nombre}: Crawl-delay de {delay}s es inviable; tienda omitida.")
                return False
            self.pausa = max(PAUSA, float(delay))
        return True

    def get_json(self, params):
        try:
            try:
                # %20 y no '+': VTEX responde 400 a los términos de varias palabras con '+'
                qs = urlencode(params, quote_via=quote, safe=":/")
                r = self.sesion.get(f"{self.url}?{qs}", timeout=TIMEOUT)
            except requests.RequestException as e:
                raise ErrorTienda(f"error de red: {e}")
            if r.status_code in (403, 429):
                self.bloqueos += 1
                if r.status_code == 429:
                    ra = r.headers.get("Retry-After", "")
                    if ra.isdigit():
                        time.sleep(min(int(ra), 60))
                if self.bloqueos >= BLOQUEOS_MAX:
                    raise TiendaBloqueada(f"{self.bloqueos} respuestas {r.status_code} seguidas")
                raise ErrorTienda(f"HTTP {r.status_code}")
            self.bloqueos = 0
            if r.status_code not in OK:
                raise ErrorTienda(f"HTTP {r.status_code}: {r.text[:200].strip()}")
            try:
                data = r.json()
            except ValueError:
                raise ErrorTienda("la respuesta no es JSON")
            if not isinstance(data, list):
                raise ErrorTienda("la respuesta no es una lista")
            return data
        finally:
            time.sleep(self.pausa)

    def descargar(self, termino, fq, max_paginas=None):
        productos = []
        for pag in range(max_paginas or MAX_PAGINAS):
            desde = pag * POR_PAGINA
            params = {"ft": termino, "_from": desde, "_to": desde + POR_PAGINA - 1}
            if fq:
                params["fq"] = fq
            data = self.get_json(params)
            productos.extend(data)
            if len(data) < POR_PAGINA:
                break
        return productos


# ─────────────────────────── Parseo de productos ───────────────────────────
def disponible(oferta):
    """IsAvailable y Stock > 0. Si la respuesta no trae Stock se usa AvailableQuantity;
    si no trae ninguno, decide solo IsAvailable."""
    if oferta.get("IsAvailable") is not True:
        return False
    stock = oferta.get("Stock")
    if stock is None:
        stock = oferta.get("AvailableQuantity")
    return True if stock is None else stock > 0


def _positivo(x):
    try:
        x = float(x)
    except (TypeError, ValueError):
        return None
    return round(x, 2) if x > 0 else None


def parsear(p):
    """Devuelve (fila_sin_ids, motivo_descarte). motivo_descarte es None si la fila es válida."""
    item = (p.get("items") or [{}])[0]
    oferta = ((item.get("sellers") or [{}])[0]).get("commertialOffer") or {}
    if not disponible(oferta):
        return None, "no_disponible"
    nombre = (p.get("productName") or "").strip()
    precio = _positivo(oferta.get("Price"))
    if not nombre or precio is None:
        return None, "sin_precio"
    ean = str(item.get("ean") or "").strip() or None
    if ean and len(ean) > 20:             # no cabe en VARCHAR(20); el backend lo verá sin EAN
        ean = None
    brand = ((p.get("brand") or "").strip()[:100]) or None
    imagenes = item.get("images") or []
    net, unidad, detalle = extraer_tamano(p)
    return {
        "ean": ean,
        "name": nombre,
        "brand_name": brand,
        "price": precio,
        "list_price": _positivo(oferta.get("ListPrice")),
        "product_url": p.get("link"),
        "image_url": imagenes[0].get("imageUrl") if imagenes else None,
        "net_content": net,
        "unit_base": unidad,
        "tamano_detalle": detalle,
    }, None


# ─────────────────────────── Base de datos ───────────────────────────
def conectar():
    return psycopg2.connect(
        host=os.environ["DB_HOST"],
        port=os.getenv("DB_PORT", "5432"),
        dbname=os.environ["DB_NAME"],
        user=os.environ["DB_USER"],
        password=os.environ["DB_PASSWORD"],
        sslmode=os.getenv("DB_SSLMODE", "prefer"),
    )


def leer_targets(conn, tiendas):
    with conn.cursor(cursor_factory=psycopg2.extras.DictCursor) as cur:
        cur.execute(
            """SELECT t.id, t.store_id, s.name AS store_name, t.search_term, t.store_category_fq,
                      c.name AS category_name
               FROM scrape_targets t
               JOIN stores s ON s.id = t.store_id
               JOIN categories c ON c.id = t.category_id
               WHERE t.active = true AND lower(s.name) = ANY(%s)
               ORDER BY t.store_id, t.id""",
            (list(tiendas),),
        )
        return cur.fetchall()


SQL_INSERT = """INSERT INTO scraped_offers
    (store_id, scrape_target_id, ean, name, brand_name, price, list_price, product_url,
     image_url, net_content, unit_base, scraped_at, status)
    VALUES %s"""


def insertar(conn, filas):
    with conn.cursor() as cur:
        psycopg2.extras.execute_values(cur, SQL_INSERT, filas, page_size=200)
    conn.commit()


# ─────────────────────────── Registro de descartes ───────────────────────────
DESCARTADOS = []   # (tienda, término, motivo, nombre, IsAvailable, Stock, AvailableQuantity)


def _oferta(p):
    item = (p.get("items") or [{}])[0]
    return ((item.get("sellers") or [{}])[0]).get("commertialOffer") or {}


def _mostrar_descartes():
    por_defecto = "1" if os.getenv("GITHUB_ACTIONS") else "0"
    return os.getenv("MOSTRAR_DESCARTES", por_defecto) == "1"


def imprimir_descartes(tienda, termino, filas):
    """Imprime los descartes de UN target apenas termina, en un grupo plegable de GitHub Actions
    (en local se imprime como bloque simple). Así se ven a medida que el scraper avanza."""
    if not filas or not _mostrar_descartes():
        return
    en_actions = bool(os.getenv("GITHUB_ACTIONS"))
    titulo = f"Descartes · {tienda} · {termino} ({len(filas)})"
    print(f"::group::{titulo}" if en_actions else f"   {titulo}")
    for _, _, motivo, nombre, disp, stock, cant in filas:
        extra = (f" [IsAvailable={disp} Stock={stock} AvailableQuantity={cant}]"
                 if motivo == "no_disponible" else "")
        print(f"{motivo:<13} | {(nombre or '')[:110]}{extra}")
    if en_actions:
        print("::endgroup::")


def guardar_descartados():
    ruta = os.path.abspath(os.getenv("DESCARTADOS_PATH", "descartados.csv"))
    with open(ruta, "w", newline="", encoding="utf-8-sig") as fh:
        w = csv.writer(fh)
        w.writerow(["tienda", "termino", "motivo", "nombre", "IsAvailable", "Stock", "AvailableQuantity"])
        w.writerows(DESCARTADOS)
    print(f"\nDescartes: {len(DESCARTADOS)} filas guardadas en {ruta}")


SIN_TAMANO = []   # (tienda, término, nombre, detalle, sku, claves_specs, specs, link)


def _fila_sin_tamano(t, p, d):
    item = (p.get("items") or [{}])[0]
    claves = ",".join(str(k) for k in (p.get("allSpecifications") or []))
    specs = " | ".join(_textos_especificaciones(p))
    return (t["store_name"], t["search_term"], d["name"], d["tamano_detalle"],
            item.get("name") or "", claves, specs, p.get("link") or "")


def imprimir_sin_tamano(tienda, termino, filas):
    """Ofertas que SÍ se insertaron pero con net_content/unit_base en NULL, y por qué.
    Se controla con MOSTRAR_DESCARTES, igual que los descartes."""
    if not filas or not _mostrar_descartes():
        return
    en_actions = bool(os.getenv("GITHUB_ACTIONS"))
    titulo = f"Sin tamaño · {tienda} · {termino} ({len(filas)})"
    print(f"::group::{titulo}" if en_actions else f"   {titulo}")
    for _, _, nombre, detalle, sku, claves, specs, _link in filas:
        print(f"{detalle:<10} | {(nombre or '')[:90]} | sku={sku[:40]} | claves={claves[:120]} | specs={specs[:80]}")
    if en_actions:
        print("::endgroup::")


def guardar_sin_tamano():
    ruta = os.path.abspath(os.getenv("SIN_TAMANO_PATH", "sin_tamano.csv"))
    with open(ruta, "w", newline="", encoding="utf-8-sig") as fh:
        w = csv.writer(fh)
        w.writerow(["tienda", "termino", "nombre", "detalle", "sku", "claves_especificaciones",
                    "especificaciones", "link"])
        w.writerows(SIN_TAMANO)
    print(f"Sin tamaño: {len(SIN_TAMANO)} filas guardadas en {ruta}")


# ─────────────────────────── Flujo principal ───────────────────────────
def procesar_target(conn, cli, t):
    st = dict(encontrados=0, filtrados=0, excluidos=0, no_disp=0, sin_precio=0, insertados=0, sin_tamano=0, error="", por_detalle=Counter())
    fq = (t["store_category_fq"] or "").strip() or None
    try:
        productos = cli.descargar(t["search_term"], fq)
    except TiendaBloqueada:
        raise
    except ErrorTienda as e:
        st["error"] = str(e)
        return st

    ahora = datetime.now()
    vistos, filas = set(), []
    for p in productos:
        clave = p.get("productId") or p.get("link")
        if clave in vistos:
            continue
        vistos.add(clave)
        st["encontrados"] += 1
        if fq is None:
            motivo_f = None
            if not cumple_terminos(p.get("productName"), t["search_term"]):
                motivo_f = "sin_match"
            elif excluido(p.get("productName"), t["search_term"], t["category_name"]):
                motivo_f = "excluido"
            if motivo_f:
                st["excluidos" if motivo_f == "excluido" else "filtrados"] += 1
                DESCARTADOS.append((t["store_name"], t["search_term"], motivo_f,
                                    p.get("productName"), "", "", ""))
                continue
        d, motivo = parsear(p)
        if motivo == "no_disponible":
            st["no_disp"] += 1
            o = _oferta(p)
            DESCARTADOS.append((t["store_name"], t["search_term"], "no_disponible", p.get("productName"),
                                o.get("IsAvailable"), o.get("Stock"), o.get("AvailableQuantity")))
            continue
        if motivo:
            st["sin_precio"] += 1
            DESCARTADOS.append((t["store_name"], t["search_term"], "sin_precio",
                                p.get("productName"), "", "", ""))
            continue
        st["por_detalle"][d["tamano_detalle"].split(":")[0]] += 1
        if d["net_content"] is None:
            st["sin_tamano"] += 1
            SIN_TAMANO.append(_fila_sin_tamano(t, p, d))
        filas.append((t["store_id"], t["id"], d["ean"], d["name"], d["brand_name"], d["price"],
                      d["list_price"], d["product_url"], d["image_url"], d["net_content"],
                      d["unit_base"], ahora, "PENDING"))
    if filas:
        try:
            insertar(conn, filas)
        except psycopg2.Error as e:
            conn.rollback()
            st["error"] = f"BD: {str(e).strip().splitlines()[0]}"
            return st
    st["insertados"] = len(filas)
    return st


def linea_resultado(st, segundos):
    if st.get("error") and "encontrados" not in st:
        return f"   ✖ {st['error']}"
    txt = (f"   ✔ {st['encontrados']} hallados · {st['filtrados']} sin match · {st['excluidos']} excluidos · "
           f"{st['no_disp']} no disponibles · {st['sin_precio']} sin precio · "
           f"{st['insertados']} insertados · {st['sin_tamano']} sin tamaño  ({segundos:.0f}s)")
    pd = st.get("por_detalle")
    if pd:
        txt += "\n   · tamaño por: " + " · ".join(f"{k} {v}" for k, v in pd.most_common())
    if st.get("error"):
        txt = f"   ✖ {st['error']}\n" + txt
    return txt


def _correr():
    pedidas = [x.strip().lower() for x in os.getenv("SOLO_TIENDAS", ",".join(TIENDAS)).split(",") if x.strip()]
    tiendas = [x for x in pedidas if x in TIENDAS]
    if not tiendas:
        print("SOLO_TIENDAS no contiene tiendas válidas:", ", ".join(TIENDAS))
        return 1

    conn = conectar()
    try:
        targets = leer_targets(conn, tiendas)
        if not targets:
            print("No hay scrape_targets activos para estas tiendas "
                  "(el seed los inserta con active = false: actívalos antes de correr).")
            return 1

        resumen = OrderedDict()          # (tienda, término) -> stats
        totales = {}                     # tienda -> ofertas insertadas
        sin_tamano_tot = {}              # tienda -> ofertas insertadas sin tamaño
        clientes, omitidas = {}, set()

        for n, t in enumerate(targets, 1):
            tienda = t["store_name"]
            clave = tienda.lower()
            totales.setdefault(tienda, 0)
            if clave in omitidas:
                resumen[(tienda, t["search_term"])] = dict(error="tienda omitida")
                continue
            if clave not in clientes:
                clientes[clave] = Cliente(tienda, TIENDAS[clave])
                if not clientes[clave].leer_robots():
                    omitidas.add(clave)
                    resumen[(tienda, t["search_term"])] = dict(error="Crawl-delay inviable")
                    continue
            print(f"→ [{n}/{len(targets)}] {tienda} · {t['search_term']}"
                  + ("" if t["store_category_fq"] else "  (sin fq: filtro por palabras)"))
            inicio = time.time()
            antes = len(DESCARTADOS)
            antes_st = len(SIN_TAMANO)
            try:
                st = procesar_target(conn, clientes[clave], t)
            except TiendaBloqueada as e:
                print(f"  ⛔ {tienda} cortada: {e}")
                omitidas.add(clave)
                st = dict(error=f"bloqueada: {e}")
            print(linea_resultado(st, time.time() - inicio))
            imprimir_descartes(tienda, t["search_term"], DESCARTADOS[antes:])
            imprimir_sin_tamano(tienda, t["search_term"], SIN_TAMANO[antes_st:])
            totales[tienda] += st.get("insertados", 0)
            sin_tamano_tot[tienda] = sin_tamano_tot.get(tienda, 0) + st.get("sin_tamano", 0)
            resumen[(tienda, t["search_term"])] = st
    finally:
        conn.close()

    # ── Resumen ──
    print(f"\n{'═' * 100}\nRESUMEN\n{'═' * 100}")
    print(f"{'Tienda':<10} {'Término':<22} {'Hallados':>8} {'Sin match':>9} {'Excluidos':>9} {'No disp.':>8} "
          f"{'Sin precio':>10} {'Insertados':>10} {'Sin tamaño':>10}")
    for (tienda, termino), s in resumen.items():
        if s.get("error") and "insertados" not in s:
            print(f"{tienda:<10} {termino:<22} {s['error']}")
            continue
        extra = f"  ⚠ {s['error']}" if s.get("error") else ""
        print(f"{tienda:<10} {termino:<22} {s['encontrados']:>8} {s['filtrados']:>9} {s['excluidos']:>9} {s['no_disp']:>8} "
              f"{s['sin_precio']:>10} {s['insertados']:>10} {s['sin_tamano']:>10}{extra}")
    print("\nOfertas insertadas por tienda:")
    for tienda, n in totales.items():
        sin = sin_tamano_tot.get(tienda, 0)
        pct = round(100 * sin / n) if n else 0
        print(f"  {tienda}: {n} ({sin} sin tamaño, {pct}%)")
        if n and pct >= UMBRAL_SIN_TAMANO:
            aviso = f"{tienda}: {sin} de {n} ofertas sin tamaño ({pct}%); revisa el CSV sin_tamano"
            print(f"  ⚠ {aviso}")
            if os.getenv("GITHUB_ACTIONS"):
                print(f"::warning title=Ofertas sin tamaño::{aviso}")

    vacias = [t for t, n in totales.items() if n == 0]
    if vacias:
        print(f"\n⛔ Alerta de cero resultados: {', '.join(vacias)} no insertó ninguna oferta.")
        return 1
    return 0


def main():
    # Sin esto, en GitHub Actions (salida no interactiva) Python acumula el texto y lo suelta al final
    try:
        sys.stdout.reconfigure(line_buffering=True)
    except AttributeError:
        pass
    try:
        return _correr()
    finally:
        # El CSV se guarda siempre, incluso si la corrida falla a medias
        guardar_descartados()
        guardar_sin_tamano()


if __name__ == "__main__":
    sys.exit(main())