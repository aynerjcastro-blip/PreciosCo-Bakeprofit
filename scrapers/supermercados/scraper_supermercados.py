"""
Scraper unificado de precios — Supermercados (D1, Éxito, Jumbo, Olímpica)

Reemplaza a los 5 scripts sueltos (scraper_d1.py, scraper_exito.py,
scraper_jumbo.py, scraper_olimpica.py, scraper.py). Usa UN solo driver de
Chrome (headless) y UNA sola conexión a la base de datos (Postgres/Supabase)
para las 4 tiendas.

Variables de entorno esperadas (ver README / GitHub Secrets):
    DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
    HEADLESS=true|false   (opcional, default true)
"""

import os
import re
import time
import logging
from dataclasses import dataclass, field

import psycopg2
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from webdriver_manager.chrome import ChromeDriverManager
from bs4 import BeautifulSoup

logging.basicConfig(level=logging.INFO, format="%(message)s")
log = logging.getLogger("scraper")

# ── Configuración de conexión (Supabase/Postgres) ───────────────────────
DB_HOST = os.environ["DB_HOST"]
DB_PORT = os.environ.get("DB_PORT", "5432")
DB_NAME = os.environ["DB_NAME"]
DB_USER = os.environ["DB_USER"]
DB_PASSWORD = os.environ["DB_PASSWORD"]
HEADLESS = os.environ.get("HEADLESS", "true").lower() != "false"

# Nombres de las tiendas EXACTAMENTE como están en la tabla "store".
NOMBRES_TIENDA = {
    "d1": "D1",
    "exito": "Éxito",
    "jumbo": "Jumbo",
    "olimpica": "Olímpica",
}

# ── Catálogo único de productos (canasta básica) ────────────────────────
# "filtros": si la tienda lo soporta, se usa para escoger la presentación
#            correcta (p. ej. 500g) entre varios resultados.
# "exclusiones": nombres que NO deben aparecer en el producto encontrado.
PRODUCTOS = {
    "Arroz":               {"busqueda": "arroz",           "filtros": ["500g", "500 g", "1000g", "1000 g", "2500g", "2500 g", "1kg", "1 kg", "2kg", "2 kg"], "exclusiones": []},
    "Maíz":                {"busqueda": "harina maiz",     "filtros": ["340g", "340 g"], "exclusiones": []},
    "Frijoles":            {"busqueda": "frijoles",        "filtros": ["300g", "300 g", "500g", "500 g"], "exclusiones": []},
    "Lentejas":            {"busqueda": "lentejas",        "filtros": ["500g", "500 g", "500gr", "500 gr"], "exclusiones": []},
    "Pasta":               {"busqueda": "spaguetti",       "filtros": ["250g", "250 g", "500g", "500 g"], "exclusiones": []},
    "Garbanzo":            {"busqueda": "garbanzo",        "filtros": [], "exclusiones": []},
    "Avena":               {"busqueda": "avena",           "filtros": ["500g", "500 g", "1000g", "1000 g"], "exclusiones": []},
    "Harina de Trigo":     {"busqueda": "harina trigo",    "filtros": ["1000g", "1000 g", "1kg", "1 kg"], "exclusiones": []},
    "Harina de Maíz":      {"busqueda": "harina maiz",     "filtros": ["340g", "340 g", "500g", "500 g"], "exclusiones": []},
    "Azúcar":              {"busqueda": "azucar",          "filtros": ["1000g", "1000 g", "1kg", "1 kg", "2kg", "2 kg"], "exclusiones": []},
    "Sal":                 {"busqueda": "sal",             "filtros": ["500g", "500 g", "1000g", "1000 g"], "exclusiones": ["salsa", "salchicha", "salmon"]},
    "Aceite Vegetal":      {"busqueda": "aceite vegetal",  "filtros": ["900ml", "900 ml"], "exclusiones": []},
    "Mantequilla":         {"busqueda": "mantequilla",     "filtros": ["125g", "125 g", "180g", "180 g", "250g", "250 g"], "exclusiones": ["pan bimbo"]},
    "Pollo":               {"busqueda": "pollo",           "filtros": ["500g", "500 g"], "exclusiones": ["salchichon", "salchichón", "mortadela"]},
    "Res":                 {"busqueda": "carne res",       "filtros": ["500g", "500 g"], "exclusiones": ["cerdo", "tocino", "pulpa", "lomo de cerdo"]},
    "Cerdo":               {"busqueda": "carne cerdo",     "filtros": ["500g", "500 g", "1kg", "1 kg", "1000g", "1000 g"], "exclusiones": ["res", "molida de res"]},
    "Pescado":             {"busqueda": "pescado",         "filtros": ["500g", "500 g", "1kg", "1 kg"], "exclusiones": []},
    "Mortadela":           {"busqueda": "mortadela",       "filtros": ["250g", "250 g"], "exclusiones": []},
    "Huevo":               {"busqueda": "huevo",           "filtros": ["huevo tipo A"], "exclusiones": ["orveja", "sorpresa", "pan bimbo", "pan taj"]},
    "Salchicha":           {"busqueda": "salchicha",       "filtros": ["250g", "250 g", "500g", "500 g"], "exclusiones": []},
    "Jamón":               {"busqueda": "jamon",           "filtros": [], "exclusiones": ["jabón", "jabon"]},
    "Atún":                {"busqueda": "atun",            "filtros": ["170g", "170 g"], "exclusiones": []},
    "Leche Entera":        {"busqueda": "leche entera",    "filtros": ["200ml", "200 ml", "900ml", "900 ml"], "exclusiones": ["deslactosada", "polvo"]},
    "Leche Deslactosada":  {"busqueda": "leche deslactosada", "filtros": ["900ml", "900 ml"], "exclusiones": []},
    "Leche en Polvo":      {"busqueda": "leche polvo",     "filtros": ["300g", "300 g", "1000g", "1000 g"], "exclusiones": []},
    "Yogur":               {"busqueda": "yogur",           "filtros": ["1000ml", "1000 ml", "1 l"], "exclusiones": []},
    "Queso Costeño":       {"busqueda": "queso costeno",   "filtros": ["400g", "400 g"], "exclusiones": []},
    "Crema de Leche":      {"busqueda": "crema leche",     "filtros": ["200g", "200 g", "200ml", "200 ml"], "exclusiones": []},
    "Suero Costeño":       {"busqueda": "suero costeno",   "filtros": [], "exclusiones": []},
    "Agua Botellada":      {"busqueda": "agua",            "filtros": ["1000ml", "1000 ml", "1 l", "1l"], "exclusiones": ["gaseosa", "jugo", "saborizada"]},
    "Gaseosa":             {"busqueda": "gaseosa",         "filtros": ["1000ml", "1000 ml", "2000ml", "2000 ml", "1 l", "2 l"], "exclusiones": []},
    "Jugo Natural":        {"busqueda": "jugo",            "filtros": ["1000ml", "1000 ml", "2000ml", "2000 ml", "1 l", "2 l"], "exclusiones": ["polvo", "light"]},
    "Jugo en Caja":        {"busqueda": "jugo caja",       "filtros": ["200ml", "200 ml", "250ml", "250 ml"], "exclusiones": []},
    "Café Molido":         {"busqueda": "cafe molido",     "filtros": ["125g", "125 g", "220g", "220 g", "500g", "500 g"], "exclusiones": []},
    "Café Instantáneo":    {"busqueda": "cafe instantaneo", "filtros": ["85g", "85 g", "85gr", "85 gr"], "exclusiones": []},
    "Chocolate en Polvo":  {"busqueda": "chocolate polvo", "filtros": ["100g", "100 g", "500g", "500 g", "1000g", "1000 g"], "exclusiones": []},
    "Jabón de Baño":       {"busqueda": "jabon tocador",   "filtros": [], "exclusiones": ["liquido", "líquido"]},
    "Shampoo":             {"busqueda": "shampoo",         "filtros": ["300ml", "300 ml", "600ml", "600 ml"], "exclusiones": []},
    "Jabón Líquido":       {"busqueda": "jabon liquido",   "filtros": ["500ml", "500 ml", "1000ml", "1000 ml", "1l", "1 l"], "exclusiones": []},
    "Detergente en Polvo": {"busqueda": "detergente multiusos", "filtros": ["900g", "900 g", "2800g", "2800 g"], "exclusiones": ["liquido", "líquido"]},
    "Detergente Líquido":  {"busqueda": "detergente liquido", "filtros": ["1l", "1 l", "1000ml", "1000 ml", "2l", "2 l"], "exclusiones": []},
    "Suavizante de Ropa":  {"busqueda": "suavizante",      "filtros": ["1l", "1 l", "1000ml", "1000 ml", "2l", "2 l"], "exclusiones": []},
    "Blanqueador":         {"busqueda": "blanqueador",     "filtros": ["1l", "1 l", "1000ml", "1000 ml", "2000ml", "2000 ml"], "exclusiones": []},
    "Desinfectante":       {"busqueda": "desinfectante",   "filtros": [], "exclusiones": ["toalla", "toallas"]},
    "Papel Higiénico":     {"busqueda": "papel higienico", "filtros": ["un", "UN", "mts", "MTS"], "exclusiones": []},
    "Toallas de Cocina":   {"busqueda": "toalla cocina",   "filtros": ["h", "H", "hojas", "130", "660"], "exclusiones": []},
    "Pasta Dental":        {"busqueda": "crema dental",    "filtros": [], "exclusiones": []},
    "Desodorante":         {"busqueda": "desodorante",     "filtros": ["150ml", "150 ml", "150ML"], "exclusiones": []},
}


# ── Utilidades comunes ───────────────────────────────────────────────────
def iniciar_driver():
    options = webdriver.ChromeOptions()
    if HEADLESS:
        options.add_argument("--headless=new")
    options.add_argument("--start-maximized")
    options.add_argument("--disable-gpu")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--disable-blink-features=AutomationControlled")
    options.add_experimental_option(
        "prefs", {"profile.managed_default_content_settings.images": 2}
    )
    driver = webdriver.Chrome(
        service=Service(ChromeDriverManager().install()), options=options
    )
    driver.set_page_load_timeout(120)
    return driver


def limpiar_precio(texto):
    try:
        limpio = re.sub(r"[^\d]", "", texto)
        return float(limpio) if limpio else None
    except Exception:
        return None


def limpiar_nombre(nombre):
    return (
        nombre.replace("_", " ")
        .replace("*", " ")
        .replace("`", " ")
        .replace("[", "(")
        .replace("]", ")")
    )


def cumple_filtro(nombre, filtros, exclusiones):
    nombre_lower = nombre.lower()
    if any(ex.lower() in nombre_lower for ex in exclusiones):
        return False
    if not filtros:
        return True
    return any(f.lower() in nombre_lower for f in filtros)


def elegir_mejor(candidatos, filtros, exclusiones, modo):
    """candidatos: lista de {'nombre':.., 'precio':..} ya extraídos del HTML.
    modo 'min_filtrado' -> el más barato entre los que cumplen filtro (D1).
    modo 'primero_valido' -> el primero que no esté excluido (Éxito/Jumbo/Olímpica).
    """
    validos = [c for c in candidatos if cumple_filtro(c["nombre"], filtros if modo == "min_filtrado" else [], exclusiones)]
    if not validos:
        return None
    if modo == "min_filtrado":
        return min(validos, key=lambda c: c["precio"])
    return validos[0]


# ── Estrategias de scraping por tienda ──────────────────────────────────
def scrapear_d1(driver, termino):
    url = f"https://domicilios.tiendasd1.com/search?name={termino.replace(' ', '+')}"
    driver.get(url)
    try:
        WebDriverWait(driver, 15).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, "h3[data-testid='card-name']"))
        )
    except Exception:
        return []
    for _ in range(3):
        driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
        time.sleep(2)

    soup = BeautifulSoup(driver.page_source, "html.parser")
    tarjetas = soup.select("div.ProductWrapper_product-wrapper__N92On")
    candidatos = []
    for t in tarjetas:
        nombre_tag = t.select_one("h3[data-testid='card-name']")
        precio_tag = t.select_one("p[data-testid='card-base-price']")
        if not nombre_tag or not precio_tag:
            continue
        nombre = limpiar_nombre(nombre_tag.text.strip())
        precio = limpiar_precio(precio_tag.text)
        if precio is not None:
            candidatos.append({"nombre": nombre, "precio": precio})
    return candidatos


def scrapear_exito(driver, termino):
    url = f"https://www.exito.com/s?q={termino.replace(' ', '+')}&sort=score_desc&page=0"
    driver.get(url)
    try:
        WebDriverWait(driver, 15).until(EC.presence_of_element_located((By.CSS_SELECTOR, "article")))
    except Exception:
        return []
    time.sleep(3)
    soup = BeautifulSoup(driver.page_source, "html.parser")
    candidatos = []
    for art in soup.select("article")[:5]:
        nombre_tag = art.select_one("h3.styles_name__qQJiK")
        precio_tag = art.select_one("p[class*='ProductPrice_container__price']")
        if not nombre_tag or not precio_tag:
            continue
        nombre = limpiar_nombre(nombre_tag.text.strip())
        precio = limpiar_precio(precio_tag.text)
        if precio is not None:
            candidatos.append({"nombre": nombre, "precio": precio})
    return candidatos


def scrapear_jumbo(driver, termino):
    url = f"https://www.jumbocolombia.com/{termino.replace(' ', '%20')}?_q={termino.replace(' ', '%20')}&map=ft"
    driver.get(url)
    try:
        WebDriverWait(driver, 15).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, "span.vtex-product-summary-2-x-productBrand"))
        )
    except Exception:
        return []
    time.sleep(3)
    soup = BeautifulSoup(driver.page_source, "html.parser")
    candidatos = []
    for prod in soup.select("div[data-af-element='search-result']")[:5]:
        nombre_tag = prod.select_one("span.vtex-product-summary-2-x-productBrand")
        precio_tag = prod.select_one("div.tiendasjumboqaio-jumbo-minicart-2-x-price")
        if not nombre_tag or not precio_tag:
            continue
        nombre = limpiar_nombre(nombre_tag.text.strip())
        precio = limpiar_precio(precio_tag.text)
        if precio is not None:
            candidatos.append({"nombre": nombre, "precio": precio})
    return candidatos


def scrapear_olimpica(driver, termino):
    url = f"https://www.olimpica.com/{termino.replace(' ', '%20')}?_q={termino.replace(' ', '%20')}&map=ft"
    driver.get(url)
    try:
        WebDriverWait(driver, 15).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, "div[data-af-element='search-result']"))
        )
    except Exception:
        return []
    time.sleep(3)
    soup = BeautifulSoup(driver.page_source, "html.parser")
    candidatos = []
    for prod in soup.select("div[data-af-element='search-result']")[:5]:
        nombre_tag = prod.select_one("span.vtex-product-summary-2-x-productBrand")
        if not nombre_tag:
            continue
        nombre = limpiar_nombre(nombre_tag.text.strip())
        precio_container = prod.select_one("div[class*='sellingPrice']")
        if not precio_container:
            continue
        spans = precio_container.select("span.olimpica-dinamic-flags-0-x-currencyContainer span")
        precio = limpiar_precio("".join(s.text for s in spans))
        if precio is not None:
            candidatos.append({"nombre": nombre, "precio": precio})
    return candidatos


# Tienda -> (función de scraping, modo de selección)
TIENDAS = {
    "d1":       {"fn": scrapear_d1,       "modo": "min_filtrado",  "espera": 2},
    "exito":    {"fn": scrapear_exito,    "modo": "primero_valido", "espera": 3},
    "jumbo":    {"fn": scrapear_jumbo,    "modo": "primero_valido", "espera": 3},
    "olimpica": {"fn": scrapear_olimpica, "modo": "primero_valido", "espera": 3},
}


# ── Base de datos (Postgres / Supabase) ─────────────────────────────────
def conectar_bd():
    return psycopg2.connect(
        host=DB_HOST, port=DB_PORT, dbname=DB_NAME,
        user=DB_USER, password=DB_PASSWORD, sslmode="require",
    )


def obtener_id_tienda(cur, nombre_tienda):
    cur.execute('SELECT id FROM store WHERE lower(name) = lower(%s)', (nombre_tienda,))
    row = cur.fetchone()
    return row[0] if row else None


def obtener_id_producto(cur, nombre_producto):
    cur.execute('SELECT id FROM product WHERE lower(name) = lower(%s)', (nombre_producto,))
    row = cur.fetchone()
    return row[0] if row else None


def precio_repetido(cur, id_producto, id_tienda, valor):
    """Verificación previa: evita reinsertar el mismo precio si no cambió
    desde la última corrida (no hay procedimiento de BD que lo bloquee,
    como sí pasaba en Oracle con PKG_PRECIO)."""
    cur.execute(
        """SELECT value FROM price
           WHERE product_id = %s AND store_id = %s
           ORDER BY registration_date DESC LIMIT 1""",
        (id_producto, id_tienda),
    )
    row = cur.fetchone()
    return row is not None and float(row[0]) == float(valor)


def insertar_precio(conn, cur, valor, source, id_producto, id_tienda):
    if precio_repetido(cur, id_producto, id_tienda, valor):
        log.info("   ⚠️  Precio repetido, no se reinserta")
        return False
    cur.execute(
        "INSERT INTO price (value, source, registration_date, product_id, store_id) "
        "VALUES (%s, %s, now(), %s, %s)",
        (valor, source[:500], id_producto, id_tienda),
    )
    conn.commit()
    return True


# ── Orquestador ──────────────────────────────────────────────────────────
def main():
    log.info("Conectando a Supabase/Postgres...")
    conn = conectar_bd()
    cur = conn.cursor()
    log.info("✅ Conexión exitosa\n")

    ids_tienda = {}
    for clave, nombre in NOMBRES_TIENDA.items():
        id_tienda = obtener_id_tienda(cur, nombre)
        if id_tienda is None:
            log.warning(f"⚠️  Tienda no encontrada en BD: {nombre} (se omite)")
        ids_tienda[clave] = id_tienda

    driver = iniciar_driver()
    total_guardados = 0
    total_errores = 0

    for nombre_producto, datos in PRODUCTOS.items():
        id_producto = obtener_id_producto(cur, nombre_producto)
        if id_producto is None:
            log.warning(f"⚠️  Producto no encontrado en BD: {nombre_producto}")
            total_errores += 1
            continue

        for clave, tienda in TIENDAS.items():
            id_tienda = ids_tienda.get(clave)
            if id_tienda is None:
                continue

            log.info(f"🔍 {NOMBRES_TIENDA[clave]} — {nombre_producto}")
            try:
                candidatos = tienda["fn"](driver, datos["busqueda"])
            except Exception as e:
                log.error(f"   ❌ Error scrapeando: {e}. Reiniciando Chrome...")
                try:
                    driver.quit()
                except Exception:
                    pass
                driver = iniciar_driver()
                total_errores += 1
                continue

            mejor = elegir_mejor(candidatos, datos["filtros"], datos["exclusiones"], tienda["modo"])
            if not mejor:
                log.info("   ⚠️  Sin resultados válidos")
                total_errores += 1
                time.sleep(tienda["espera"])
                continue

            guardado = insertar_precio(
                conn, cur, mejor["precio"], mejor["nombre"], id_producto, id_tienda
            )
            if guardado:
                log.info(f"   ✅ ${mejor['precio']:,.0f} — {mejor['nombre'][:60]}")
                total_guardados += 1
            time.sleep(tienda["espera"])

    driver.quit()
    cur.close()
    conn.close()

    log.info("\n══════════════════════════════")
    log.info(f"✅ Precios guardados: {total_guardados}")
    log.info(f"❌ Sin resultado/errores: {total_errores}")
    log.info("══════════════════════════════")


if __name__ == "__main__":
    main()
