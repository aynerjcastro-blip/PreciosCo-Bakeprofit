"""
Scraper de supermercados de PreciosCo (D1, Éxito, Jumbo, Olímpica) sobre la API pública de VTEX.

Solo escribe datos crudos en `scraped_offers` (ADR-03). No elige ganador, no normaliza el EAN,
no filtra por rango de tamaño, no crea productos y no toca `prices`: todo eso es del backend Java.

Lee solo los scrape_targets de las tiendas del dominio "Supermercado" (la categoría raíz de la
tienda). El flujo, la estrategia VTEX, el lector de tamaños y el acceso a la base están en
scrapers/comun; aquí solo se declaran el dominio y las tiendas.

Variables de entorno: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
                      (opcional) DB_SSLMODE, por ejemplo "require" para Supabase.
                      (opcional) SOLO_TIENDAS, por ejemplo "exito" o "exito,jumbo": procesa solo esas
                      tiendas (se compara con el nombre, sin tildes ni mayúsculas). Vacía = todas.

Uso:
    pip install -r requirements.txt
    python scraper_supermercados.py

Código de salida: 0 si todo salió bien; 1 si hubo errores o si alguna tienda no insertó ninguna
oferta (alerta de cero resultados: GitHub Actions marca la corrida como fallida); 2 si no hay
conexión o configuración, o si no existe la categoría raíz del dominio.
"""
import sys
from collections import OrderedDict
from pathlib import Path

# scrapers/ en el path, para importar el paquete común con el mismo comando de siempre
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from comun.corrida import ejecutar          # noqa: E402
from comun.texto import sin_tildes          # noqa: E402
from comun.vtex import VtexStrategy         # noqa: E402

DOMINIO = "Supermercado"

# Clave (dentro del nombre de la tienda, sin tildes y en minúsculas) -> estrategia
TIENDAS_VTEX = OrderedDict([
    ("d1",       VtexStrategy("https://www.d1.com.co", regional=False)),   # precio nacional, sin vendedores por ciudad
    ("exito",    VtexStrategy("https://www.exito.com/io")),   # Éxito cuelga de /io/
    ("jumbo",    VtexStrategy("https://www.jumbocolombia.com", tienda="jumbo")),
    ("olimpica", VtexStrategy("https://www.olimpica.com", tienda="olimpica")),
])


def estrategia_para(nombre_tienda):
    n = sin_tildes(nombre_tienda)
    for clave, estrategia in TIENDAS_VTEX.items():
        if clave in n:
            return estrategia
    return None


def main():
    return ejecutar(DOMINIO, estrategia_para)


if __name__ == "__main__":
    sys.exit(main())
