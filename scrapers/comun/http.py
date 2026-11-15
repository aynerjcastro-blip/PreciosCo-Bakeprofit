"""Sesión HTTP del proyecto y peticiones con reintentos."""
import time

import requests

USER_AGENT = "PreciosCo-proyecto-academico/1.0"
PAUSA = 1.5                 # segundos entre peticiones
TIMEOUT = 30
REINTENTOS = 2              # reintentos ante error de red, 429 o 5xx
STATUS_OK = (200, 206)      # VTEX responde 206 cuando se usa _from/_to


def nueva_sesion(accept="application/json"):
    session = requests.Session()
    session.headers.update({"User-Agent": USER_AGENT, "Accept": accept})
    return session


def _pedir(session, metodo, url, **kwargs):
    """Petición con reintentos ante error de red, 429 o 5xx. Devuelve la respuesta (200 o 206)."""
    ultimo_error = None
    for intento in range(REINTENTOS + 1):
        if intento:
            time.sleep(PAUSA * (2 ** intento))
        try:
            r = session.request(metodo, url, timeout=TIMEOUT, **kwargs)
        except requests.RequestException as e:
            ultimo_error = f"error de red: {e}"
            continue
        if r.status_code in STATUS_OK:
            return r
        if r.status_code == 429 or r.status_code >= 500:
            ultimo_error = f"HTTP {r.status_code}"
            continue
        raise RuntimeError(f"HTTP {r.status_code} en {r.url} -> {r.text[:150]!r}")
    raise RuntimeError(ultimo_error or "sin respuesta")


def pedir_json(session, metodo, url, **kwargs):
    """Devuelve el JSON de la respuesta (ver _pedir para los reintentos)."""
    r = _pedir(session, metodo, url, **kwargs)
    try:
        return r.json()
    except ValueError:
        raise RuntimeError(f"la respuesta de {url} no es JSON")


def pedir_texto(session, url, **kwargs):
    """Devuelve el texto (HTML) de una página, pidiéndola como la pide un navegador."""
    headers = {"Accept": "text/html,application/xhtml+xml"}
    headers.update(kwargs.pop("headers", {}))
    return _pedir(session, "GET", url, headers=headers, **kwargs).text
