"""
Presentación de un producto de droguería o cuidado personal (tabletas, mililitros, gramos) desde su nombre.

El lector de tamaños de supermercados (tamanos.py) no sirve aquí: "x 10 tabletas" no se reconoce y
"150 mg/5 ml frasco x 90 ml" se leería como 5 ml. Este lector es conservador: si el nombre trae dos
tamaños distintos, devuelve (None, None) y la oferta queda "sin tamaño" para revisión.

La dosis (mg, mcg, %) nunca es el tamaño: la distingue el EAN.
"""
import re
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP

from .tamanos import MAX_NET_CONTENT
from .texto import sin_tildes

_UNIDADES_CONTEO = (r"tabletas?|tabs?|capsulas?|caps?|comprimidos?|sobres?|ampollas?|amp|pastillas?|parches?|"
                    r"unidad(?:es)?|unds?|uds?|preservativos?|condones?|pruebas?|toallas?|tiras?|apositos?|"
                    r"curas?|gasas?|tapabocas|bandas?|frascos?|viales?|vial|dosis")
_RE_CONTEO = re.compile(rf"(?<![\d/.,])(\d{{1,4}})\s*(?:{_UNIDADES_CONTEO})\b")
_RE_X_NUMERO = re.compile(r"(?:^|\s)[x*]\s*(\d{1,4})\b(?!\s*(?:mg|mcg|ml|cc|g|gr|kg|l|lt|ui|iu|%|cm|mm))")
# La "X" pegada al final ("* 50 GRX") es un resto del formato de Farmanorte, no parte de la unidad
_RE_MEDIDA = re.compile(r"(?<![\d/.,])(\d{1,5}(?:[.,]\d{1,2})?)\s*(ml|cc|lts?|litros?|l|kg|kilos?|gramos?|grs?|g)x?\b")
_FACTORES = {"ml": ("ML", 1), "cc": ("ML", 1), "l": ("ML", 1000), "lt": ("ML", 1000), "lts": ("ML", 1000),
             "litro": ("ML", 1000), "litros": ("ML", 1000),
             "g": ("G", 1), "gr": ("G", 1), "grs": ("G", 1), "gramo": ("G", 1), "gramos": ("G", 1),
             "kg": ("G", 1000), "kilo": ("G", 1000), "kilos": ("G", 1000)}


def _decimal(txt):
    try:
        return Decimal(txt.replace(",", "."))
    except InvalidOperation:
        return None


def _unico(valores):
    """Un solo valor distinto -> ese valor; ninguno o varios distintos -> None."""
    distintos = set(valores)
    return distintos.pop() if len(distintos) == 1 else None


def _resultado(total, base):
    if total is None or total <= 0 or total > MAX_NET_CONTENT:
        return None, None
    return total.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP), base


def leer_presentacion(nombre, unidad_esperada="UN"):
    """(net_content, unit_base) a partir del nombre, o (None, None) si no se puede leer sin dudas."""
    texto = sin_tildes(nombre or "")
    if (unidad_esperada or "UN") == "UN":
        explicitos = [Decimal(m.group(1)) for m in _RE_CONTEO.finditer(texto)]
        if explicitos:
            return _resultado(_unico(explicitos), "UN")
        sueltos = [Decimal(m.group(1)) for m in _RE_X_NUMERO.finditer(texto)]
        return _resultado(_unico(sueltos), "UN")
    base = unidad_esperada
    totales = []
    for m in _RE_MEDIDA.finditer(texto):
        b, factor = _FACTORES[m.group(2)]
        n = _decimal(m.group(1))
        if b == base and n is not None:
            totales.append(n * factor)
    return _resultado(_unico(totales), base)
