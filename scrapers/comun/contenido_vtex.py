"""Contenido publicado por cada tienda; no multiplica piezas inferidas del nombre."""
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP
from .texto import sin_tildes
from .tamanos import MAX_NET_CONTENT

UNIDADES = {"g": ("G", 1), "gr": ("G", 1), "gramo": ("G", 1),
            "kg": ("G", 1000), "kilogramo": ("G", 1000),
            "ml": ("ML", 1), "mililitro": ("ML", 1),
            "l": ("ML", 1000), "lt": ("ML", 1000), "litro": ("ML", 1000),
            "un": ("UN", 1), "unidad": ("UN", 1)}


def valor(producto, campo):
    dato = producto.get(campo)
    if isinstance(dato, list):
        dato = dato[0] if len(dato) == 1 else None
    return dato


def positivo(dato):
    try:
        n = Decimal(str(dato).replace(",", "."))
        return n if n.is_finite() and n > 0 else None
    except (InvalidOperation, ValueError):
        return None


def contenido_publicado(p, tienda):
    # Jumbo expresa el multiplicador en kg/l incluso cuando etiqueta gr/ml.
    campos = ("measurement_unit_un", "unit_multiplier_un") if tienda == "jumbo" else ("measurement_unit", "unit_multiplier")
    unidad, cantidad = valor(p, campos[0]), valor(p, campos[1])
    if campos[0] not in p and campos[1] not in p:
        return None  # Solo la ausencia completa permite respaldo desde el nombre.
    base_factor = UNIDADES.get(sin_tildes(str(unidad)).strip())
    n = positivo(cantidad)
    if not base_factor or n is None:
        return (None, None)
    base, factor = base_factor
    if tienda == "jumbo" and base in ("G", "ML"):
        factor = 1000
    total = (n * factor).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    return (total, base) if 0 < total <= MAX_NET_CONTENT else (None, None)


def peso_venta_kg(item):
    # measurementUnit es la unidad de venta del SKU, no una inferencia por EAN.
    if str(item.get("measurementUnit", "")).lower() != "kg":
        return None
    return positivo(item.get("unitMultiplier"))
