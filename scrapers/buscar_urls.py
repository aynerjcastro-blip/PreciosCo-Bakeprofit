"""
Busca URLs candidatas de Farmatodo y Farmanorte para los productos que ya trae Drogas La Rebaja.

No busca en las tiendas (sus robots.txt prohíben la búsqueda): lee el SITEMAP de producto de cada una,
que sus robots.txt permiten, y compara el nombre que va en cada URL con el nombre, la marca y el tamaño
del producto de La Rebaja. Solo PROPONE: la pareja es real únicamente si el EAN que lee la página
coincide, y eso lo comprueba el paso de verificación con el scraper de páginas (scraper_droguerias.py).

Cumplimiento: consulta el robots.txt de cada tienda antes de pedir un sitemap, usa el User-Agent del
proyecto (comun/http.py), respeta su Crawl-delay y hace una pausa entre archivos. No abre ninguna
página de producto: solo descarga los sitemaps (una vez; quedan en caché en disco).

Entrada (una de las dos):
    --csv rebaja.csv   columnas: ean, name, brand_name, net_content, unit_base y, opcional, falta_en
                       (tiendas donde ese EAN aún no tiene URL, separadas por coma; si falta, se busca en todas)
    --db               las lee de la base con DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD

Salida, en la carpeta --salida (por defecto salida_urls/):
    candidatas.csv          todas las candidatas con su puntaje
    candidatas_valores.sql  filas (store_id, 'ean', 'url') listas para el paso 1 de los objetivos

Uso:
    pip install -r ../supermercados/requirements.txt
    python buscar_urls.py --csv rebaja.csv
    python buscar_urls.py --csv rebaja.csv --excluir eans_con_url.txt --tiendas farmatodo
"""
import argparse
import csv
import gzip
import hashlib
import re
import sys
import time
import xml.etree.ElementTree as ET
from decimal import Decimal, InvalidOperation
from pathlib import Path
from urllib.parse import urlsplit

# scrapers/ en el path, para importar el paquete común
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from comun.http import TIMEOUT, nueva_sesion   # noqa: E402
from comun.presentacion import leer_presentacion   # noqa: E402
from comun.robots import Robots                 # noqa: E402
from comun.texto import sin_tildes              # noqa: E402

SQL_ENTRADA = """
SELECT DISTINCT ON (o.ean) o.ean, o.name, o.brand_name, o.net_content, o.unit_base
FROM scraped_offers o
JOIN stores s ON s.id = o.store_id
WHERE s.name ILIKE '%rebaja%' AND o.ean IS NOT NULL AND o.net_content IS NOT NULL
ORDER BY o.ean, o.scraped_at DESC
"""
# scraped_offers solo conserva las ofertas 7 días (retención del procesador): corre el scraper de
# La Rebaja antes si hace más tiempo.

# store_id de cada tienda en la base compartida; se pueden cambiar con --id-farmatodo / --id-farmanorte
STORE_IDS = {"farmatodo": 11, "farmanorte": 12}
BASES = {"farmatodo": "https://www.farmatodo.com.co", "farmanorte": "https://www.farmanorte.com.co"}

PAUSA_SITEMAP = 2.0     # segundos entre archivos de sitemap (sube si el robots.txt declara más)
MINIMO = 0.6            # puntaje mínimo para proponer una candidata
POR_PRODUCTO = 2        # candidatas que se guardan por producto y tienda

# Palabras del nombre que no sirven para distinguir un producto de otro
STOP = {
    "de", "del", "con", "para", "por", "sin", "y", "en", "el", "la", "los", "las", "una", "uno",
    "caja", "cajas", "tableta", "tabletas", "tab", "tabs", "capsula", "capsulas", "cap", "caps",
    "blister", "unidad", "unidades", "und", "unds", "tubo", "frasco", "sobre", "sobres", "bolsa",
    "pack", "kit", "ref", "referencia", "oral", "uso", "venta", "libre",
}
_RE_NUM = re.compile(r"\d+(?:[.,]\d+)?")
_RE_PAL = re.compile(r"[a-z]{3,}")


# ---------------------------------------------------------------- lectura de sitemaps

def parsear_sitemap(contenido):
    """(tipo, [urls]) de un sitemap: tipo es 'sitemapindex' (lista de sitemaps) o 'urlset' (páginas)."""
    raiz = ET.fromstring(contenido)
    etiqueta = lambda e: e.tag.rsplit("}", 1)[-1]       # noqa: E731  (ignora el espacio de nombres)
    urls = [e.text.strip() for e in raiz.iter() if etiqueta(e) == "loc" and e.text and e.text.strip()]
    return etiqueta(raiz), urls


def sitemaps_en_robots(texto_robots):
    return [m.group(1).strip() for m in re.finditer(r"(?im)^\s*sitemap\s*:\s*(\S+)", texto_robots or "")]


class Descargador:
    """Descarga los sitemaps con robots.txt, pausa y caché en disco."""

    def __init__(self, session, carpeta_cache, refrescar=False):
        self.session = session
        self.robots = Robots(session)
        self.cache = carpeta_cache
        self.refrescar = refrescar
        self.cache.mkdir(parents=True, exist_ok=True)

    def robots_txt(self, base):
        r = self.session.get(f"{base}/robots.txt", timeout=TIMEOUT)
        return r.text if r.status_code == 200 else ""

    def sitemap(self, url):
        archivo = self.cache / (hashlib.sha1(url.encode()).hexdigest()[:16] + ".xml")
        if archivo.exists() and not self.refrescar:
            return archivo.read_bytes()
        if not self.robots.permite(url):
            raise RuntimeError(f"el robots.txt de la tienda no permite {url}")
        pausa = max(PAUSA_SITEMAP, self.robots.retraso(url) or 0)
        time.sleep(pausa)
        r = self.session.get(url, timeout=TIMEOUT, headers={"Accept": "application/xml,text/xml,*/*"})
        if r.status_code != 200:
            raise RuntimeError(f"HTTP {r.status_code} en {url}")
        contenido = r.content
        if contenido[:2] == b"\x1f\x8b":                # .xml.gz
            contenido = gzip.decompress(contenido)
        archivo.write_bytes(contenido)
        return contenido


def urls_de_producto(descargador, tienda, todos=False):
    """URLs de producto de una tienda, leyendo su sitemap (y los sitemaps de producto del índice)."""
    base = BASES[tienda]
    raices = sitemaps_en_robots(descargador.robots_txt(base)) or [f"{base}/sitemap.xml"]
    paginas, pendientes, vistos = [], list(raices), set()
    while pendientes:
        url = pendientes.pop(0)
        if url in vistos:
            continue
        vistos.add(url)
        tipo, locs = parsear_sitemap(descargador.sitemap(url))
        if tipo == "sitemapindex":
            hijos = locs if todos else ([u for u in locs if "product" in u.lower()] or locs)
            print(f"[{tienda}] {url}: índice con {len(locs)} sitemaps, se leen {len(hijos)}")
            pendientes.extend(hijos)
        else:
            paginas.extend(locs)
    return [u for u in dict.fromkeys(paginas) if slug_de(u, tienda)]


def slug_de(url, tienda):
    """Texto del nombre que va en la URL de un producto, o None si no es una página de producto."""
    ruta = urlsplit(url).path.strip("/")
    if tienda == "farmatodo":
        if not ruta.startswith("producto/"):
            return None
        return re.sub(r"^\d+-", "", ruta[len("producto/"):].split("/")[0])
    if tienda == "farmanorte":
        if not ruta.endswith("/p"):
            return None
        return ruta[:-2].split("/")[-1]
    return None


# ---------------------------------------------------------------- comparación

def _numero(t):
    t = t.replace(",", ".")
    return t[:-2] if t.endswith(".0") else t


def numeros(texto):
    return {_numero(n) for n in _RE_NUM.findall(sin_tildes(texto))}


def palabras(texto):
    return {p for p in _RE_PAL.findall(sin_tildes(texto)) if p not in STOP}


def _coincide(a, b):
    return a == b or (min(len(a), len(b)) >= 5 and (a.startswith(b) or b.startswith(a)))


def _cobertura(buscadas, disponibles):
    if not buscadas:
        return 0.5                                  # nada que comparar: neutro
    return sum(any(_coincide(p, d) for d in disponibles) for p in buscadas) / len(buscadas)


def puntuar(producto, slug):
    """Entre 0 y 1: cuánto se parece el nombre de la URL al producto de La Rebaja."""
    texto = slug.replace("-", " ")
    pal_p = palabras(f"{producto['name']} {producto.get('brand_name') or ''}")
    pal_s = palabras(texto)
    num_p = numeros(producto["name"])
    if producto.get("net_content") not in (None, ""):
        num_p.add(_numero(str(producto["net_content"])))
    num_s = numeros(texto)
    if not pal_p or not any(_coincide(p, s) for p in pal_p for s in pal_s):
        return 0.0
    penal = min(0.3, 0.1 * len(num_s - num_p))      # la URL trae números que el producto no tiene
    puntaje = max(0.0, 0.6 * _cobertura(pal_p, pal_s) + 0.4 * _cobertura(num_p, num_s) - penal)
    return puntaje * _factor_tamano(producto, slug)


def _factor_tamano(producto, slug):
    """
    El tamaño decide si es el mismo producto (EAN distinto por cada presentación). Si la URL dice cuántas
    unidades, ml o g trae, se compara con el de La Rebaja: igual suma, distinto resta la mitad; si no
    lo dice o no se puede leer sin dudas, no cambia nada.
    """
    try:
        esperado = Decimal(str(producto.get("net_content")))
    except (InvalidOperation, ValueError, TypeError):
        return 1.0
    unidad = (producto.get("unit_base") or "UN").upper()
    contenido, unidad_url = leer_presentacion(slug.replace("-", " "), unidad)
    if contenido is None:
        return 1.0
    return 1.15 if contenido == esperado and unidad_url == unidad else 0.5


def buscar_candidatas(productos, urls, tienda, minimo=MINIMO, por_producto=POR_PRODUCTO):
    """{ean: [(puntaje, url)]} con las mejores candidatas de la tienda para cada producto."""
    indice = [(u, slug_de(u, tienda)) for u in urls]
    resultado = {}
    for p in productos:
        puntuadas = sorted(((puntuar(p, s), u) for u, s in indice), key=lambda x: (-x[0], x[1]))
        buenas = [(round(min(pt, 1.0), 3), u) for pt, u in puntuadas[:por_producto] if pt >= minimo]
        if buenas:
            resultado[p["ean"]] = buenas
    return resultado


# ---------------------------------------------------------------- entrada y salida

def leer_csv(ruta):
    with open(ruta, newline="", encoding="utf-8-sig") as f:
        muestra = f.read(4096)
        f.seek(0)
        delimitador = ";" if muestra.count(";") > muestra.count(",") else ","
        filas = [{(k or "").strip().lower(): (v or "").strip() for k, v in fila.items()}
                 for fila in csv.DictReader(f, delimiter=delimitador)]
    faltan = {"ean", "name"} - set(filas[0]) if filas else {"ean", "name"}
    if faltan:
        raise SystemExit(f"{ruta}: faltan las columnas {sorted(faltan)} (esperadas: ean, name, brand_name, net_content, unit_base)")
    return [f for f in filas if f["ean"]]


def leer_db():
    import psycopg2.extras                      # solo se necesita en este modo
    from comun.staging import conectar
    conn = conectar()
    try:
        with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            cur.execute(SQL_ENTRADA)
            return [{k: ("" if v is None else str(v)) for k, v in fila.items()} for fila in cur.fetchall()]
    finally:
        conn.close()


def leer_excluidos(ruta):
    if not ruta:
        return set()
    return set(re.findall(r"\d{8,14}", Path(ruta).read_text(encoding="utf-8")))


def pendientes_de(productos, tienda):
    """Productos que hay que buscar en la tienda: todos, o los que traen la tienda en su columna falta_en."""
    return [p for p in productos if not p.get("falta_en") or tienda in p["falta_en"].replace(" ", "").split(",")]


def escribir_salidas(carpeta, productos, por_tienda, ids):
    carpeta.mkdir(parents=True, exist_ok=True)
    por_ean = {p["ean"]: p for p in productos}
    filas_csv, valores, alternativas = [], {t: [] for t in por_tienda}, []
    sin_candidata = {t: [] for t in por_tienda}
    for tienda, candidatas in por_tienda.items():
        usadas = {}                                    # url -> ean que ya la tomó con mejor puntaje
        for ean, lista in sorted(candidatas.items(), key=lambda kv: -kv[1][0][0]):
            for rank, (pt, url) in enumerate(lista, 1):
                p = por_ean[ean]
                compartida = rank == 1 and url in usadas
                filas_csv.append([tienda, ean, p["name"], p.get("brand_name", ""),
                                  f"{p.get('net_content', '')} {p.get('unit_base', '')}".strip(),
                                  rank, pt, "SI" if compartida else "", url])
                if rank == 1 and not compartida:
                    usadas[url] = ean
                    valores[tienda].append(f"    ({ids[tienda]}, '{ean}', '{url}')")
                else:
                    alternativas.append(f"--   {tienda} {ean} rank {rank} ({pt}){' URL ya usada' if compartida else ''}: {url}")
        sin_candidata[tienda] = [p for p in pendientes_de(productos, tienda) if p["ean"] not in candidatas]

    with open(carpeta / "candidatas.csv", "w", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f)
        w.writerow(["tienda", "ean", "nombre_rebaja", "marca", "tamano", "rank", "puntaje", "url_repetida", "url"])
        w.writerows(filas_csv)

    sql = ["-- Candidatas de buscar_urls.py: NO verificadas. Una por EAN y tienda (la de mayor puntaje).",
           "-- Pégalas en el paso 1 de los objetivos y verifica con el paso 3 (el EAN que lee la página decide).",
           "-- Una segunda candidata para el mismo EAN chocaría con uq_scrape_targets_store_term_unit: no se incluye;",
           "-- si la primera sale DISTINTO, reemplázala a mano por la alternativa de abajo.", ""]
    for tienda, filas in valores.items():
        sql.append(f"-- {tienda} (store_id {ids[tienda]}): {len(filas)} candidatas")
        sql.append(",\n".join(filas) if filas else "-- (ninguna)")
        sql.append("")
    if alternativas:
        sql += ["-- Alternativas (rank 2 y URLs repetidas):"] + alternativas + [""]
    for tienda, faltan in sin_candidata.items():
        if faltan:
            sql.append(f"-- Sin candidata en {tienda} ({len(faltan)}): buscar a mano")
            sql += [f"--   {p['ean']}  {p['name']}" for p in faltan]
            sql.append("")
    (carpeta / "candidatas_valores.sql").write_text("\n".join(sql), encoding="utf-8")
    return valores, sin_candidata


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    origen = ap.add_mutually_exclusive_group(required=True)
    origen.add_argument("--csv", help="CSV con los productos de La Rebaja")
    origen.add_argument("--db", action="store_true", help="leerlos de la base (variables DB_*)")
    ap.add_argument("--tiendas", default="farmatodo,farmanorte", help="separadas por coma")
    ap.add_argument("--excluir", help="archivo con EAN que ya tienen URL (uno por línea, o cualquier texto)")
    ap.add_argument("--salida", default="salida_urls", help="carpeta de resultados y caché")
    ap.add_argument("--minimo", type=float, default=MINIMO, help=f"puntaje mínimo (por defecto {MINIMO})")
    ap.add_argument("--refrescar", action="store_true", help="volver a descargar los sitemaps")
    ap.add_argument("--todos-los-sitemaps", action="store_true", help="no limitar el índice a los de producto")
    ap.add_argument("--id-farmatodo", type=int, default=STORE_IDS["farmatodo"])
    ap.add_argument("--id-farmanorte", type=int, default=STORE_IDS["farmanorte"])
    args = ap.parse_args(argv)

    tiendas = [t.strip() for t in args.tiendas.split(",") if t.strip()]
    desconocidas = [t for t in tiendas if t not in BASES]
    if desconocidas:
        raise SystemExit(f"tiendas desconocidas: {desconocidas} (válidas: {sorted(BASES)})")
    ids = {"farmatodo": args.id_farmatodo, "farmanorte": args.id_farmanorte}

    productos = leer_db() if args.db else leer_csv(args.csv)
    excluidos = leer_excluidos(args.excluir)
    productos = [p for p in productos if p["ean"] not in excluidos]
    print(f"{len(productos)} productos de La Rebaja por buscar ({len(excluidos)} EAN excluidos)")
    if not productos:
        return 0

    carpeta = Path(args.salida)
    descargador = Descargador(nueva_sesion(accept="application/xml,text/xml,*/*"), carpeta / "cache", args.refrescar)
    por_tienda, errores = {}, 0
    for tienda in tiendas:
        try:
            urls = urls_de_producto(descargador, tienda, args.todos_los_sitemaps)
        except (RuntimeError, ET.ParseError, OSError) as e:
            print(f"[{tienda}] ERROR: {e}")
            errores += 1
            continue
        print(f"[{tienda}] {len(urls)} URLs de producto en el sitemap")
        buscados = pendientes_de(productos, tienda)
        por_tienda[tienda] = buscar_candidatas(buscados, urls, tienda, args.minimo)
        print(f"[{tienda}] {len(por_tienda[tienda])} de {len(buscados)} productos con candidata")
    if por_tienda:
        escribir_salidas(carpeta, productos, por_tienda, ids)
        print(f"Resultados en {carpeta / 'candidatas.csv'} y {carpeta / 'candidatas_valores.sql'}")
    return 1 if errores else 0


if __name__ == "__main__":
    sys.exit(main())
