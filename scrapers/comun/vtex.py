"""
Estrategia para tiendas VTEX: API pública de catálogo (búsqueda) y simulación de compra.

Precios de Valledupar (DECISIONS.md, "Precios regionales"): la búsqueda de VTEX devuelve el precio
del vendedor por defecto, que no siempre es el de Valledupar. En las tiendas regionales, el precio
y la disponibilidad salen de una simulación de compra con el vendedor que atiende Valledupar; un
producto que ese vendedor no vende no se guarda. Una tienda sin vendedores por ciudad (D1) usa el
precio de la búsqueda.

Una tienda VTEX con filtros propios o con otra forma de leer el tamaño hereda de VtexStrategy y
reemplaza filtrar() o tamano().
"""
import json
import logging
import time
from collections import OrderedDict
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP
from urllib.parse import quote, urlencode

from .contenido_vtex import contenido_publicado, peso_venta_kg
from .ciudad import CIUDAD, COORDENADAS_CIUDAD
from .corrida import TiendaStrategy
from .http import PAUSA, pedir_json
from .tamanos import parsear_tamano, tamano_desde_descripcion

RESULTADOS_POR_CATEGORIA = 50   # _from=0, _to=49
POLITICA_COMERCIAL = 1      # canal de venta (sc) de la tienda en línea
LOTE_SIMULACION = 50        # productos por simulación (una búsqueda trae hasta 50)

log = logging.getLogger(__name__)


class VtexStrategy(TiendaStrategy):
    RUTA_BUSQUEDA = "/api/catalog_system/pub/products/search"
    RUTA_REGIONES = "/api/checkout/pub/regions"
    RUTA_SIMULACION = "/api/checkout/pub/orderForms/simulation"

    def __init__(self, base_url, regional=True, tienda=None):
        self.tienda = tienda
        self.base_url = base_url.rstrip("/")
        self.regional = regional
        self._vendedores = None     # vendedores de la ciudad, se consultan una vez por corrida

    def buscar(self, session, termino, fq):
        params = {"ft": termino, "_from": 0, "_to": RESULTADOS_POR_CATEGORIA - 1}
        if fq:
            params["fq"] = fq
        # Espacios como %20 (requests usa '+' y varias tiendas VTEX responden 400 con "agua+sin+gas") y
        # ':' '/' codificados (%3A %2F), como los mandaba requests cuando Éxito aún respondía.
        url = self.base_url + self.RUTA_BUSQUEDA + "?" + urlencode(params, quote_via=quote, safe="")
        data = pedir_json(session, "GET", url)
        if not isinstance(data, list):
            raise RuntimeError("la respuesta no es una lista de productos")
        return data

    def vendedores_locales(self, session):
        """Vendedores que atienden la ciudad, según las coordenadas (VTEX asigna uno por ciudad)."""
        if self._vendedores is None:
            lng, lat = COORDENADAS_CIUDAD
            params = {"country": "COL", "geoCoordinates": f"{lng};{lat}"}
            url = self.base_url + self.RUTA_REGIONES + "?" + urlencode(params, quote_via=quote, safe=";")
            regiones = pedir_json(session, "GET", url)
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
        return pedir_json(session, "POST", url, data=json.dumps(cuerpo),
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

    # ── Ofertas de un target ──

    def filas(self, session, target, ahora, r):
        fq = target["store_category_fq"]
        if not fq:
            log.warning("[%s][%s] sin store_category_fq: la búsqueda solo por texto devuelve basura "
                        "(completar la categoría de la tienda en scrape_targets)", r.tienda, r.termino)
        productos = self.buscar(session, target["search_term"], fq)
        r.encontrados = len(productos)
        productos = self.filtrar(productos, target, r)
        if self.regional:
            filas = self._filas_regionales(session, productos, target, ahora, r)
        else:
            filas = self._filas_nacionales(productos, target, ahora, r)
        if productos and r.no_disponibles == len(productos):
            log.warning("[%s][%s] ninguno de los %d productos está disponible%s", r.tienda, r.termino,
                        len(productos), f" en {CIUDAD}" if self.regional else "")
        return filas

    def filtrar(self, productos, target, r):
        """Productos que siguen a precios y filas. Por defecto todos; una tienda con reglas propias
        (kits, medicamentos con fórmula) lo reemplaza y suma los descartados en r.excluidos."""
        return productos

    def tamano(self, p, target):
        """(net_content, unit_base) del producto: primero el nombre, después la descripción."""
        if self.tienda in ("jumbo", "olimpica"):
            estructurado = contenido_publicado(p, self.tienda)
            if estructurado is not None:
                return estructurado
        contenido, unidad = parsear_tamano((p.get("productName") or "").strip(), target.get("unit_base"))
        if contenido is None:
            contenido, unidad = tamano_desde_descripcion(p.get("description"), target.get("unit_base"))
        return contenido, unidad

    def construir_fila(self, p, target, ahora, precio_local=None):
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
        contenido, unidad = self.tamano(p, target)

        peso_kg = peso_venta_kg(item)
        if peso_kg is None and self.tienda == "olimpica":
            publicado = contenido_publicado(p, self.tienda)
            if publicado and publicado[1] == "G":
                peso_kg = publicado[0] / 1000

        return (target["store_id"], target["id"], ean, nombre, marca, precio, lista,
                p.get("link"), imagen, contenido, unidad, ahora, "PENDING",
                str(item.get("itemId")) if item.get("itemId") is not None else None,
                peso_kg)

    def _filas_regionales(self, session, productos, target, ahora, r):
        items, por_sku = [], {}
        for p in productos:
            item, _ = oferta_del_producto(p)
            sku = item.get("itemId")
            if sku and sku not in por_sku:
                por_sku[sku] = p
                items.append((sku, vendedores_del_item(item)))
        precios = self.precios_locales(session, items) if items else {}
        r.no_disponibles = len(productos) - len(precios)
        filas = []
        for sku, p in por_sku.items():
            if sku not in precios:
                continue
            try:
                fila = self.construir_fila(p, target, ahora, precios[sku])
            except Exception as e:      # un producto raro no tumba el resto
                log.warning("[%s][%s] producto omitido por error de lectura: %s", r.tienda, r.termino, e)
                continue
            if fila is None:
                r.sin_precio += 1
            else:
                filas.append(fila)
        return filas

    def _filas_nacionales(self, productos, target, ahora, r):
        filas = []
        for p in productos:
            try:
                _, oferta = oferta_del_producto(p)
                if not esta_disponible(oferta):
                    r.no_disponibles += 1
                    continue
                fila = self.construir_fila(p, target, ahora)
            except Exception as e:      # un producto raro no tumba el resto
                log.warning("[%s][%s] producto omitido por error de lectura: %s", r.tienda, r.termino, e)
                continue
            if fila is None:
                r.sin_precio += 1
                continue
            filas.append(fila)
        return filas


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
