"""
Estrategia de Farmatodo (www.farmatodo.com.co), tienda de aplicación propia (Angular).

Se lee solo la página pública del producto (/producto/...), que el robots.txt permite, y de ella el
estado que Angular deja dentro del HTML (<script id="app-root-state">). No se usan las APIs ni las
claves que aparecen en ese estado (Algolia, api-transactional): no están documentadas para terceros
(DECISIONS.md, hallazgo 8). Del estado se toma nada más: nombre, marca, código de barras, precio de
Valledupar, disponibilidad e imagen (enlazada, no copiada).

Precio de Valledupar: fullPriceByCity trae un precio por ciudad y solo las ciudades donde se vende el
producto; "VUP" es Valledupar [Probable: coincide con el código IATA y con el teléfono de Valledupar
que lista la página]. Si Valledupar no está en la lista, el producto no se guarda: no se cae al precio
nacional sin avisar (DECISIONS.md, "Precios regionales"). Se usa fullPrice, nunca offerPrice ni
primePrice: son ofertas condicionadas ("Exclusivo 1ra compra", Prime).

Límite conocido: la disponibilidad no se puede saber por ciudad sin la API interna (el estado solo
trae los ids de las tiendas con stock). Se guarda si hay stock en alguna tienda del país.
"""
import json
import logging
import re

from .pagina import DatosPagina, PaginaProductoStrategy, dinero
from .presentacion import leer_presentacion

log = logging.getLogger(__name__)

CIUDAD_CODIGO = "VUP"       # Valledupar en fullPriceByCity
_ENTIDADES = {"&q;": '"', "&a;": "&", "&s;": "'", "&l;": "<", "&g;": ">"}
_RE_ESTADO = re.compile(r'<script id="app-root-state"[^>]*>(.*?)</script>', re.S)


def leer_estado(html):
    """Estado de transferencia de Angular como dict, o None si la página no lo trae."""
    m = _RE_ESTADO.search(html or "")
    if not m:
        return None
    texto = re.sub(r"&[qasgl];", lambda e: _ENTIDADES[e.group(0)], m.group(1))
    try:
        return json.loads(texto)
    except ValueError:
        return None


def _producto(estado):
    for clave, valor in estado.items():
        if clave.startswith("pdp-algolia-index-") and isinstance(valor, dict):
            return valor
    return None


def _verdadero(v):
    return str(v).strip().lower() == "true"


class FarmatodoStrategy(PaginaProductoStrategy):

    def __init__(self, excluir_formula=True):
        super().__init__()
        self.excluir_formula = excluir_formula

    def extraer(self, html, target):
        estado = leer_estado(html)
        p = _producto(estado) if estado else None
        if not p:
            return None
        precio = None
        for c in p.get("fullPriceByCity") or []:
            if c.get("cityCode") == CIUDAD_CODIGO:
                precio = dinero(c.get("fullPrice"))
        con_stock = bool(p.get("stores_with_stock")) or (p.get("totalStock") or 0) > 0 or p.get("hasStock") is True
        if precio is None:
            log.warning("[%s] Valledupar (%s) no está en fullPriceByCity: el producto no se vende allí "
                        "o Farmatodo cambió sus códigos de ciudad", target.get("store_name"), CIUDAD_CODIGO)
        disponible = precio is not None and con_stock and p.get("outofstore") is not True
        nombre = (p.get("mediaDescription") or "").strip()
        contenido, unidad = leer_presentacion(nombre, target.get("unit_base"))
        return DatosPagina(
            nombre=nombre, precio=precio, precio_lista=precio,
            ean=str(p.get("barcode")) if p.get("barcode") else None,   # barcodeList trae ids internos; barcode es el EAN
            marca=p.get("marca") or p.get("brand"), imagen=p.get("mediaImageUrl"),
            disponible=disponible, requiere_formula=_verdadero(p.get("requirePrescription")),
            contenido=contenido, unidad=unidad)

    def descartar(self, datos):
        if datos.requiere_formula and self.excluir_formula:
            return "medicamento con fórmula médica"
        return None
