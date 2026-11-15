"""
Estrategia para tiendas que se leen por página de producto, no por búsqueda.

Se usa donde el robots.txt prohíbe la búsqueda (Farmatodo, Farmanorte): cada scrape_target lleva la
URL (target_url) de UN producto curado por el equipo, y la estrategia lee los datos de esa página.
Antes de pedirla se consulta el robots.txt de la tienda (ruta permitida y Crawl-delay) y se respeta una
pausa propia entre páginas.

Una tienda concreta hereda de PaginaProductoStrategy y define extraer() (y, si lo necesita, descartar()).
"""
import logging
from dataclasses import dataclass
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP
from typing import Optional

from .corrida import TiendaStrategy
from .http import pedir_texto
from .robots import Robots

log = logging.getLogger(__name__)

PAUSA_PAGINAS = 5.0         # segundos entre páginas de una tienda; sube si el robots.txt pide más


@dataclass
class DatosPagina:
    nombre: str
    precio: Optional[Decimal]
    precio_lista: Optional[Decimal] = None
    ean: Optional[str] = None
    marca: Optional[str] = None
    imagen: Optional[str] = None
    url: Optional[str] = None
    disponible: Optional[bool] = None       # None = la página no lo dice
    requiere_formula: bool = False
    contenido: Optional[Decimal] = None
    unidad: Optional[str] = None


def dinero(v):
    """Precio como Decimal con 2 decimales, o None si no es un número positivo."""
    try:
        d = Decimal(str(v)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    except (InvalidOperation, ValueError, TypeError):
        return None
    return d if d > 0 else None


class PaginaProductoStrategy(TiendaStrategy):
    pausa = PAUSA_PAGINAS

    def __init__(self):
        self._robots = None
        self._sesion_robots = None

    def extraer(self, html, target):
        """DatosPagina del producto que muestra la página, o None si no se encuentra."""
        raise NotImplementedError

    def descartar(self, datos):
        """Motivo para no guardar el producto (por ejemplo "con fórmula médica"), o None."""
        return None

    def filas(self, session, target, ahora, r):
        url = target.get("target_url")
        if not url:
            raise RuntimeError("el scrape_target no tiene target_url")
        if self._robots is None or self._sesion_robots is not session:
            self._robots, self._sesion_robots = Robots(session), session
        if not self._robots.permite(url):
            raise RuntimeError(f"el robots.txt de la tienda no permite {url}")
        espera = self._robots.retraso(url)
        if espera and espera > (self.pausa or 0):
            log.warning("[%s] el robots.txt pide Crawl-delay de %s s: se usa esa pausa", r.tienda, espera)
            self.pausa = espera

        try:
            html = pedir_texto(session, url)
        except RuntimeError as e:
            if "HTTP 404" in str(e) or "HTTP 410" in str(e):
                log.warning("[%s][%s] la página ya no existe: %s", r.tienda, r.termino, url)
                return []
            raise
        r.encontrados = 1
        datos = self.extraer(html, target)
        if datos is None:
            raise RuntimeError(f"no se encontró el producto en {url}: ¿cambió la estructura de la página?")

        motivo = self.descartar(datos)
        if motivo:
            r.excluidos += 1
            log.info("[%s][%s] excluido: %s", r.tienda, r.termino, motivo)
            return []
        if datos.disponible is False:
            r.no_disponibles += 1
            return []
        if datos.precio is None or not datos.nombre:
            r.sin_precio += 1
            return []

        ean = (datos.ean or "").strip() or None      # crudo: el backend lo normaliza y valida
        if ean and len(ean) > 20:
            log.warning("EAN de más de 20 caracteres, se guarda NULL: %r", ean[:40])
            ean = None
        marca = (datos.marca or "").strip()[:100] or None
        return [(target["store_id"], target["id"], ean, datos.nombre.strip(), marca, datos.precio,
                 datos.precio_lista, datos.url or url, datos.imagen, datos.contenido, datos.unidad,
                 ahora, "PENDING")]
