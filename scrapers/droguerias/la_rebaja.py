"""
Estrategia de Drogas La Rebaja (www.larebajavirtual.com), tienda VTEX con vendedor propio en
Valledupar: precio y disponibilidad salen de la simulación de compra, como en Jumbo y Olímpica.

Diferencias con un supermercado VTEX:
- Kits: La Rebaja publica la caja como "kit" del blíster o del sobre, con el mismo EAN de la unidad.
  Se descartan (DECISIONS.md, hallazgo 2): un EAN tiene un solo tamaño en el catálogo.
- Medicamentos con fórmula: con excluir_formula=True (la decisión del equipo; se cambia en
  scraper_droguerias.py) solo se guardan productos de venta libre. Se descarta un producto con
  "Producto RX" = SI o en una categoría "con fórmula médica". Los que no son medicamentos (tapabocas,
  toallas higiénicas, termómetros) llegan con OTC = NO y RX = NO, y sí se guardan.
- Tamaño: el nombre no trae la cantidad ("ACETAMINOFEN 500 MG (GENFAR)"); se lee de las
  especificaciones Unidadmedida y Cantidadunidadesmedida (TABLETA 10, MILILITRO 90.00, SOBRE 1).
"""
import logging
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP

from comun.tamanos import MAX_NET_CONTENT
from comun.texto import sin_tildes
from comun.vtex import VtexStrategy, oferta_del_producto

log = logging.getLogger(__name__)

# Unidadmedida (sin tildes, minúsculas) -> (unidad base, factor)
UNIDADES_LA_REBAJA = {
    "tableta": ("UN", 1), "comprimido": ("UN", 1), "capsula": ("UN", 1), "sobre": ("UN", 1),
    "ampolla": ("UN", 1), "vial": ("UN", 1), "unidad": ("UN", 1),
    "gramo": ("G", 1), "kilogramo": ("G", 1000),
    "mililitro": ("ML", 1), "litro": ("ML", 1000),
}


def _especificacion(p, nombre):
    """Primer valor de una especificación de VTEX (llegan como listas: ["SI"]), o None."""
    valor = p.get(nombre)
    if isinstance(valor, list):
        valor = valor[0] if valor else None
    return valor.strip() if isinstance(valor, str) and valor.strip() else None


def es_kit(p):
    item, _ = oferta_del_producto(p)
    return item.get("isKit") is True


def requiere_formula(p):
    if (_especificacion(p, "Producto RX") or "").upper() == "SI":
        return True
    return any("formula medica" in sin_tildes(c) for c in p.get("categories") or [])


class LaRebajaStrategy(VtexStrategy):

    def __init__(self, base_url, excluir_formula=True):
        super().__init__(base_url, regional=True)
        self.excluir_formula = excluir_formula
        self._unidades_desconocidas = set()

    def filtrar(self, productos, target, r):
        kits = [p for p in productos if es_kit(p)]
        con_formula = [p for p in productos
                       if self.excluir_formula and not es_kit(p) and requiere_formula(p)]
        quedan = [p for p in productos if not es_kit(p) and not (self.excluir_formula and requiere_formula(p))]
        r.excluidos = len(kits) + len(con_formula)
        if r.excluidos:
            log.info("[%s][%s] excluidos: %d kits, %d con fórmula médica",
                     r.tienda, r.termino, len(kits), len(con_formula))
        return quedan

    def tamano(self, p, target):
        unidad = _especificacion(p, "Unidadmedida")
        cantidad = _especificacion(p, "Cantidadunidadesmedida")
        if unidad is None or cantidad is None:
            return None, None
        clave = sin_tildes(unidad)
        if clave not in UNIDADES_LA_REBAJA:
            if clave not in self._unidades_desconocidas:
                self._unidades_desconocidas.add(clave)
                log.warning("Unidadmedida desconocida %r (p. ej. %r): el producto queda sin tamaño; "
                            "agregarla a UNIDADES_LA_REBAJA si es una unidad válida",
                            unidad, (p.get("productName") or "")[:60])
            return None, None
        base, factor = UNIDADES_LA_REBAJA[clave]
        try:
            total = (Decimal(cantidad.replace(",", ".")) * factor).quantize(Decimal("0.01"),
                                                                            rounding=ROUND_HALF_UP)
        except InvalidOperation:
            return None, None
        if total <= 0 or total > MAX_NET_CONTENT:
            return None, None
        return total, base
