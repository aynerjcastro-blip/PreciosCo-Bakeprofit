"""
Scraper de droguerías de PreciosCo: Drogas La Rebaja (API pública de VTEX; búsqueda por término),
Farmatodo - Droguería y Farmanorte (página de producto; una URL curada por scrape_target, porque sus
robots.txt prohíben la búsqueda). Una tienda sin scrape_targets activos no se consulta.

Solo escribe datos crudos en `scraped_offers` (ADR-03) y lee solo los scrape_targets de las tiendas
del dominio "Droguería" (la categoría raíz de la tienda).

Variables de entorno: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
                      (opcional) DB_SSLMODE, por ejemplo "require" para Supabase.
                      (opcional) SOLO_TIENDAS, por ejemplo "rebaja" o "farmatodo,farmanorte".

Uso:
    pip install -r ../supermercados/requirements.txt
    python scraper_droguerias.py

Código de salida: el mismo del scraper de supermercados (0 bien; 1 errores o alerta de cero
resultados; 2 sin conexión, configuración o categoría raíz del dominio).
"""
import sys
from collections import OrderedDict
from pathlib import Path

# scrapers/ en el path, para importar el paquete común
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from comun.corrida import ejecutar          # noqa: E402
from comun.farmatodo import FarmatodoStrategy   # noqa: E402
from comun.texto import sin_tildes          # noqa: E402
from farmanorte import FarmanorteStrategy   # noqa: E402
from la_rebaja import LaRebajaStrategy      # noqa: E402

DOMINIO = "Droguería"

# Decisión del equipo: True = solo medicamentos de venta libre (se descartan los de fórmula médica);
# False = también los de fórmula. La leen La Rebaja y Farmatodo; Farmanorte no marca la fórmula, así
# que allí solo vale la lista curada de URLs.
EXCLUIR_FORMULA = True

# Clave (dentro del nombre de la tienda, sin tildes y en minúsculas) -> estrategia
TIENDAS = OrderedDict([
    ("rebaja",     LaRebajaStrategy("https://www.larebajavirtual.com", excluir_formula=EXCLUIR_FORMULA)),
    ("farmatodo",  FarmatodoStrategy(excluir_formula=EXCLUIR_FORMULA)),
    ("farmanorte", FarmanorteStrategy()),
])


def estrategia_para(nombre_tienda):
    n = sin_tildes(nombre_tienda)
    for clave, estrategia in TIENDAS.items():
        if clave in n:
            return estrategia
    return None


def main():
    return ejecutar(DOMINIO, estrategia_para)


if __name__ == "__main__":
    sys.exit(main())
