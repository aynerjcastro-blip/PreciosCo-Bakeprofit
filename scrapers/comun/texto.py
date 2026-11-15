"""Comparación de textos sin tildes ni mayúsculas."""
import unicodedata


def sin_tildes(t):
    t = unicodedata.normalize("NFD", (t or "").lower())
    return "".join(c for c in t if unicodedata.category(c) != "Mn")
