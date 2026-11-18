"""
Contenido neto y unidad base (G, ML o UN) a partir del nombre o la descripción del producto.

El scraper solo lee el tamaño; filtrarlo por el rango del scrape_target es del backend Java.
"""
import html
import re
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP

from .texto import sin_tildes

MAX_NET_CONTENT = Decimal("99999999.99")   # NUMERIC(10,2)

# unidad -> (unidad base, factor)
_UNIDADES = {
    "kg": ("G", 1000), "kgs": ("G", 1000), "kilo": ("G", 1000), "kilos": ("G", 1000),
    "kilogramo": ("G", 1000), "kilogramos": ("G", 1000),
    "g": ("G", 1), "gr": ("G", 1), "grs": ("G", 1), "gramo": ("G", 1), "gramos": ("G", 1),
    "lb": ("G", 500), "lbs": ("G", 500), "libra": ("G", 500), "libras": ("G", 500),
    "l": ("ML", 1000), "lt": ("ML", 1000), "lts": ("ML", 1000),
    "litro": ("ML", 1000), "litros": ("ML", 1000),
    "ml": ("ML", 1), "cc": ("ML", 1), "mililitro": ("ML", 1), "mililitros": ("ML", 1),
    "un": ("UN", 1), "und": ("UN", 1), "unds": ("UN", 1), "unid": ("UN", 1),
    "unidad": ("UN", 1), "unidades": ("UN", 1), "uds": ("UN", 1),
    "rollo": ("UN", 1), "rollos": ("UN", 1),      # papel higiénico, toallas de cocina
    # Onzas: su unidad base depende del producto (líquido -> ML, sólido -> G); se resuelve con la unidad
    # esperada del scrape_target (ver parsear_tamano). Sin esa pista, el tamaño queda NULL.
    "oz": ("OZ", 1), "onza": ("OZ", 1), "onzas": ("OZ", 1),
}
_OZ_A_BASE = {"ML": Decimal("29.5735"), "G": Decimal("28.3495")}   # fl oz -> ml, oz -> g


def _alt(claves):
    return "|".join(sorted(claves, key=len, reverse=True))


_UNIDADES_UN = [u for u, (b, _) in _UNIDADES.items() if b == "UN"]
_UNIDADES_PESO_VOL = [u for u, (b, _) in _UNIDADES.items() if b != "UN"]
_ALT_UN = _alt(_UNIDADES_UN)
_ALT_PV = _alt(_UNIDADES_PESO_VOL)

# Número + unidad. Acepta "x5kg" (la x pegada al número) pero no letras antes ("b2l").
_RE_TAMANO = re.compile(
    r"(?<![\d.,])(?:(?<![a-z])|(?<=x))(?P<num>\d+(?:[.,]\d+)*)\s*(?P<unidad>"
    + _alt(_UNIDADES) + r")(?![a-z])"
)
# Total entre paréntesis, como lo publica Éxito: "x6und (5400 ml)". Ya es el total del empaque.
_RE_TOTAL_PARENTESIS = re.compile(
    r"\(\s*(?P<num>\d+(?:[.,]\d+)*)\s*(?P<unidad>" + _ALT_PV + r")\s*\)"
)
# "6 x 1000 ml" o "6x1000ml": la x con espacios a los dos lados o sin ninguno. Con espacio solo
# antes ("Roa 10 x2.5kg") el número es parte del nombre del producto, no la cantidad del empaque.
_RE_PACK_ANTES = re.compile(r"(?<![\d.,])(\d{1,3})(?:\s+[x*]\s+|[x*])$")
_RE_PACK_DESPUES = re.compile(                                                         # "900 ml x6", "x 6 und"
    r"^\s*[x*]\s*(\d{1,3})(?!\d|[.,]\d)(?!\s*(?:" + _ALT_PV + r")(?![a-z]))"
)
_RE_PACK_TEXTO = re.compile(
    r"(?<![\d.,])(\d{1,3})\s*(?:" + _ALT_UN + r")(?![a-z])|\bpack\s*(?:de|x)?\s*(\d{1,3})(?!\d)"
)


def _numero(txt):
    """Convierte '2.500', '2,5', '1.5', '0.500' en Decimal. Punto/coma de miles solo si hay grupos de 3."""
    try:
        if re.fullmatch(r"[1-9]\d{0,2}([.,]\d{3})+", txt):
            return Decimal(re.sub(r"[.,]", "", txt))
        return Decimal(txt.replace(",", "."))
    except InvalidOperation:
        return None


def _a_base(num, unidad, unidad_esperada):
    """(cantidad en la unidad base, unidad base), o (None, None) si no se puede convertir."""
    base, factor = _UNIDADES[unidad]
    cantidad = _numero(num)
    if cantidad is None:
        return None, None
    if base == "OZ":
        if unidad_esperada not in _OZ_A_BASE:
            return None, None
        base, factor = unidad_esperada, _OZ_A_BASE[unidad_esperada]
    return cantidad * factor, base


def _redondear(total, base):
    total = total.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    if total <= 0 or total > MAX_NET_CONTENT:
        return None, None
    return total, base


def parsear_tamano(nombre, unidad_esperada=None):
    """
    Devuelve (net_content, unit_base) a partir del nombre, o (None, None) si no se puede leer.
    G, ML o UN. Un pack "6 x 1000 ml" o "1000 ml x6" da 6000 ML; un total entre paréntesis
    ("x6und (5400 ml)") se toma tal cual. unidad_esperada (la del scrape_target) solo sirve
    para interpretar las onzas.
    """
    texto = sin_tildes(nombre)

    m = _RE_TOTAL_PARENTESIS.search(texto)
    if m:
        total, base = _a_base(m.group("num"), m.group("unidad"), unidad_esperada)
        if total is not None:
            return _redondear(total, base)

    candidatos = list(_RE_TAMANO.finditer(texto))
    if not candidatos:
        return None, None
    # Prefiere peso/volumen sobre unidades sueltas ("Aceite 1 L 1 un" -> 1 L)
    elegido = next((m for m in candidatos if _UNIDADES[m.group("unidad")][0] != "UN"), candidatos[0])
    total, base = _a_base(elegido.group("num"), elegido.group("unidad"), unidad_esperada)
    if total is None:
        return None, None

    if base != "UN":
        # Sin ficha, "500 G X16 Unds" no distingue piezas de un multipack.
        # Solo un pack explícito permite interpretar esas unidades como empaques.
        explicit_pack = re.search(r"\b(?:six\s*pack|pack)\b", texto)
        if not explicit_pack and re.search(r"[x*]\s*\d{1,3}\s*(?:" + _ALT_UN + r")(?![a-z])", texto[elegido.end():]):
            return None, None
        pack = 6 if re.search(r"\bsix\s*pack\b", texto) else None
        m = _RE_PACK_ANTES.search(texto[:elegido.start()])
        if m:
            pack = int(m.group(1))
        elif pack is None:
            m = _RE_PACK_DESPUES.match(texto[elegido.end():])
            if m:
                pack = int(m.group(1))
            else:
                resto = texto[:elegido.start()] + " " + texto[elegido.end():]
                m = _RE_PACK_TEXTO.search(resto)
                if m:
                    pack = int(m.group(1) or m.group(2))
        if pack and pack > 1:
            total *= pack

    return _redondear(total, base)


_RE_ETIQUETA_TAMANO = re.compile(r"(?:presentacion|contenido(?:\s+neto)?)\s*:\s*([^\n.]{0,40})")


def tamano_desde_descripcion(descripcion, unidad_esperada=None):
    """
    Respaldo cuando el nombre no trae tamaño (p. ej. marketplace de Éxito: la ficha dice
    "<strong>Presentación:</strong> 500ml"). Solo se acepta si el tamaño va tras la etiqueta
    "Presentación:" o "Contenido (neto):"; no se busca tamaño suelto en el texto libre.
    """
    if not descripcion:
        return None, None
    texto = sin_tildes(html.unescape(re.sub(r"<[^>]+>", " ", descripcion)))
    texto = re.sub(r"\s+", " ", texto)
    m = _RE_ETIQUETA_TAMANO.search(texto)
    return parsear_tamano(m.group(1), unidad_esperada) if m else (None, None)
