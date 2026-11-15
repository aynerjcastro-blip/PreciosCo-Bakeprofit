"""
Estrategia de Farmanorte (www.farmanorte.com.co), tienda VTEX cuyo robots.txt prohíbe la API (/api/)
y las búsquedas: solo se leen páginas de producto (/.../p), que sí permite.

De cada página se toman los datos que ya trae para los buscadores (JSON-LD de schema.org):
- EAN: está en "mpn". El campo "gtin" es el código interno de la tienda ("00005981"), no un EAN.
- Precio y disponibilidad: de la oferta del JSON-LD.
- Marca: la tienda publica "Generica" en los genéricos; se guarda sin marca para no crear una marca falsa.
- Tamaño: la especificación "Cantidad de unidades" (productos por unidades) o el nombre.

Límites conocidos:
- No trae ninguna marca de medicamento con fórmula: la lista curada de URLs es el único filtro.
- No se sabe si el precio es el de Valledupar: la ciudad se elige en el navegador y el HTML plano
  solo trae el precio por defecto.
"""
import json
import logging
import re

from comun.pagina import DatosPagina, PaginaProductoStrategy, dinero
from comun.presentacion import leer_presentacion

log = logging.getLogger(__name__)

_RE_JSONLD = re.compile(r'<script[^>]*application/ld\+json[^>]*>(.*?)</script>', re.S)
_RE_CANTIDAD = re.compile(r'"name":"Cantidad de unidades","values":\{"type":"json","json":\["(\d{1,4})"\]')
_RE_EAN = re.compile(r"\d{8,14}")
_MARCAS_GENERICAS = {"generica", "generico", "genericos", "genericas"}


def producto_jsonld(html):
    for m in _RE_JSONLD.finditer(html or ""):
        try:
            datos = json.loads(m.group(1))
        except ValueError:
            continue
        for x in (datos if isinstance(datos, list) else [datos]):
            if isinstance(x, dict) and x.get("@type") == "Product":
                return x
    return None


def _oferta(producto):
    ofertas = (producto.get("offers") or {})
    lista = ofertas.get("offers")
    return lista[0] if isinstance(lista, list) and lista else ofertas


def _marca(producto):
    marca = producto.get("brand")
    nombre = marca.get("name") if isinstance(marca, dict) else marca
    if not isinstance(nombre, str) or not nombre.strip():
        return None
    limpia = nombre.strip()
    return None if limpia.lower().replace("é", "e") in _MARCAS_GENERICAS else limpia


def _imagen(producto):
    img = producto.get("image")
    if isinstance(img, list):
        img = img[0] if img else None
    if isinstance(img, dict):
        img = img.get("url")
    return img if isinstance(img, str) else None


class FarmanorteStrategy(PaginaProductoStrategy):

    def extraer(self, html, target):
        p = producto_jsonld(html)
        if not p:
            return None
        oferta = _oferta(p)
        nombre = (p.get("name") or "").strip()
        mpn = str(p.get("mpn") or "").strip()
        ean = mpn if _RE_EAN.fullmatch(mpn) else None
        disponibilidad = str(oferta.get("availability") or "")
        disponible = None if not disponibilidad else disponibilidad.endswith("InStock")
        precio = dinero(oferta.get("price"))

        unidad_pedida = target.get("unit_base")
        contenido, unidad = leer_presentacion(nombre, unidad_pedida)
        if unidad_pedida == "UN":
            m = _RE_CANTIDAD.search(html)
            if m:
                de_ficha = dinero(m.group(1))
                if contenido is not None and de_ficha is not None and contenido != de_ficha:
                    log.warning("[%s] el nombre dice %s unidades y la ficha %s: queda sin tamaño para revisión",
                                target.get("store_name"), contenido, de_ficha)
                    contenido = unidad = None
                elif contenido is None and de_ficha is not None:
                    contenido, unidad = de_ficha, "UN"
        return DatosPagina(nombre=nombre, precio=precio, precio_lista=precio, ean=ean, marca=_marca(p),
                           imagen=_imagen(p), url=p.get("url") or (p.get("@id") or None),
                           disponible=disponible, contenido=contenido, unidad=unidad)
