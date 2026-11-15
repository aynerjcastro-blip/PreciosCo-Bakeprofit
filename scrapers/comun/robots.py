"""
Lectura del robots.txt de una tienda: qué rutas permite y qué Crawl-delay pide.

Sigue las reglas habituales (RFC 9309): se usa el grupo del agente del proyecto o, si no hay, el de "*";
gana la regla más larga y, a igual longitud, Allow; "*" y "$" valen en los patrones; un robots.txt que
no existe (4xx) no restringe nada. Si no se puede leer por un error de red o 5xx, no se pide la página.
Python trae urllib.robotparser, pero no entiende "*" dentro de una ruta ("Disallow: /producto/*/review").
"""
import re
from urllib.parse import urlsplit

import requests

from .http import TIMEOUT, USER_AGENT

AGENTE = USER_AGENT.split("/")[0].lower()      # "preciosco-proyecto-academico"


def _patron(ruta):
    ancla = ruta.endswith("$")
    cuerpo = re.escape(ruta[:-1] if ancla else ruta).replace(r"\*", ".*")
    return re.compile("^" + cuerpo + ("$" if ancla else ""))


class ReglasRobots:
    def __init__(self, texto, agente=AGENTE):
        grupos = []                 # [(agentes, reglas, crawl_delay)]
        agentes, reglas, delay, en_reglas = [], [], None, False
        for linea in (texto or "").splitlines():
            linea = linea.split("#", 1)[0].strip()
            if ":" not in linea:
                continue
            campo, valor = (x.strip() for x in linea.split(":", 1))
            campo = campo.lower()
            if campo == "user-agent":
                if en_reglas:       # un User-agent después de reglas abre un grupo nuevo
                    grupos.append((agentes, reglas, delay))
                    agentes, reglas, delay, en_reglas = [], [], None, False
                agentes.append(valor.lower())
            elif campo in ("allow", "disallow"):
                en_reglas = True
                if valor:           # "Disallow:" vacío = todo permitido
                    reglas.append((campo == "allow", valor))
            elif campo == "crawl-delay":
                en_reglas = True
                try:
                    delay = float(valor)
                except ValueError:
                    pass
        if agentes:
            grupos.append((agentes, reglas, delay))

        propio = [g for g in grupos if any(a != "*" and a in agente for a in g[0])]
        general = [g for g in grupos if "*" in g[0]]
        elegido = (propio or general or [([], [], None)])[0]
        self.reglas = [(permite, _patron(ruta), len(ruta)) for permite, ruta in elegido[1]]
        self.crawl_delay = elegido[2]

    def permite(self, ruta):
        """ruta: camino y query de la URL, por ejemplo '/producto/123-algo'."""
        mejor = None
        for permite, patron, largo in self.reglas:
            if patron.match(ruta) and (mejor is None or largo > mejor[0] or (largo == mejor[0] and permite)):
                mejor = (largo, permite)
        return True if mejor is None else mejor[1]


class Robots:
    """Reglas por sitio, leídas una sola vez por corrida con la sesión del proyecto."""

    def __init__(self, session):
        self.session = session
        self._por_sitio = {}

    def _reglas(self, url):
        partes = urlsplit(url)
        sitio = f"{partes.scheme}://{partes.netloc}"
        if sitio not in self._por_sitio:
            try:
                r = self.session.get(f"{sitio}/robots.txt", timeout=TIMEOUT)
            except requests.RequestException as e:
                raise RuntimeError(f"no se pudo leer {sitio}/robots.txt: {e}")
            if r.status_code == 200:
                self._por_sitio[sitio] = ReglasRobots(r.text)
            elif 400 <= r.status_code < 500:
                self._por_sitio[sitio] = ReglasRobots("")
            else:
                raise RuntimeError(f"HTTP {r.status_code} al leer {sitio}/robots.txt")
        return self._por_sitio[sitio]

    def permite(self, url):
        partes = urlsplit(url)
        ruta = partes.path or "/"
        if partes.query:
            ruta += "?" + partes.query
        return self._reglas(url).permite(ruta)

    def retraso(self, url):
        """Crawl-delay del sitio en segundos, o None si no lo declara."""
        return self._reglas(url).crawl_delay
