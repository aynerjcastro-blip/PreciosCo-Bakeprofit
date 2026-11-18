"""
Flujo común de una corrida: lee los targets del dominio, los procesa uno por uno con la estrategia
de cada tienda, escribe en scraped_offers y cierra con el resumen y la alerta de cero resultados.

Patrón de diseño (DECISIONS.md): Strategy. `TiendaStrategy` define cómo se obtienen las ofertas de
una tienda; cada plataforma (VTEX, página de producto, ...) es una estrategia sin tocar este flujo.

Código de salida de ejecutar(): 0 si todo salió bien; 1 si hubo errores o si alguna tienda no insertó
ninguna oferta (alerta de cero resultados); 2 si no hay conexión, configuración o el dominio no existe.
"""
import logging
import os
import time
from uuid import uuid4
from abc import ABC, abstractmethod
from collections import OrderedDict
from dataclasses import dataclass
from datetime import datetime

import psycopg2

from .ciudad import CIUDAD, HORA_COLOMBIA, ahora_colombia
from .http import PAUSA, nueva_sesion
from .staging import (POS_NET_CONTENT, DominioInexistente, conectar, filtrar_targets,
                      insertar_ofertas, leer_targets, iniciar_corrida, terminar_corrida)

BLOQUEOS_SEGUIDOS_MAX = 3   # 403/429 seguidos en una tienda -> se omite el resto de sus targets

log = logging.getLogger(__name__)


@dataclass
class Resumen:
    tienda: str
    termino: str
    encontrados: int = 0
    no_disponibles: int = 0     # sin stock o, en tiendas regionales, no se venden en la ciudad
    excluidos: int = 0          # descartados por la estrategia antes de pedir precios (kits, con fórmula)
    sin_precio: int = 0
    insertados: int = 0
    sin_tamano: int = 0
    error: str = ""


class TiendaStrategy(ABC):
    # Segundos entre una petición y la siguiente a esta tienda; None = la pausa general (PAUSA).
    pausa = None

    @abstractmethod
    def filas(self, session, target, ahora, r):
        """
        Consulta la tienda para un scrape_target y devuelve las tuplas para scraped_offers.
        Actualiza los contadores del Resumen r; una falla de la petición se lanza como excepción.
        """


def procesar_target(conn, session, target, estrategia_para, run_id=None):
    """Devuelve (Resumen, hubo_peticion)."""
    r = Resumen(target["store_name"], target["search_term"])
    estrategia = estrategia_para(target["store_name"])
    if estrategia is None:
        r.error = "tienda sin estrategia/URL configurada"
        log.error("[%s][%s] %s", r.tienda, r.termino, r.error)
        return r, False   # False: no se hizo petición, no hace falta pausa

    ahora = ahora_colombia()
    try:
        filas = estrategia.filas(session, target, ahora, r)
    except Exception as e:
        r.error = f"petición fallida: {e}"
        log.error("[%s][%s] %s", r.tienda, r.termino, r.error)
        return r, True

    r.sin_tamano = sum(1 for f in filas if f[POS_NET_CONTENT] is None)

    if filas:
        try:
            insertar_ofertas(conn, filas, run_id)
            r.insertados = len(filas)
        except Exception as e:
            conn.rollback()
            r.error = f"INSERT fallido: {e}"
            r.sin_tamano = 0
            log.error("[%s][%s] %s", r.tienda, r.termino, r.error)
            return r, True

    log.info("[%s][%s] encontrados=%d excluidos=%d no_disponibles=%d insertados=%d sin_tamano=%d",
             r.tienda, r.termino, r.encontrados, r.excluidos, r.no_disponibles, r.insertados, r.sin_tamano)
    return r, True


def tiendas_sin_ofertas(resumenes):
    """Tiendas que no insertaron ninguna oferta en toda la corrida (alerta de cero resultados)."""
    insertados = OrderedDict()
    for r in resumenes:
        insertados[r.tienda] = insertados.get(r.tienda, 0) + r.insertados
    return [tienda for tienda, total in insertados.items() if total == 0]


def imprimir_resumen(resumenes):
    log.info("=" * 107)
    log.info("%-12s %-14s %10s %10s %14s %10s %10s %10s", "Tienda", "Término", "Encontr.", "Excluidos",
             "No disponibles", "Sin precio", "Insertados", "Sin tamaño")
    for r in resumenes:
        log.info("%-12s %-14s %10d %10d %14d %10d %10d %10d%s", r.tienda, r.termino[:14], r.encontrados,
                 r.excluidos, r.no_disponibles, r.sin_precio, r.insertados, r.sin_tamano,
                 f"   ERROR: {r.error[:110]}" if r.error else "")
    log.info("-" * 107)
    log.info("%-27s %10d %10d %14d %10d %10d %10d", "TOTAL",
             sum(r.encontrados for r in resumenes), sum(r.excluidos for r in resumenes),
             sum(r.no_disponibles for r in resumenes), sum(r.sin_precio for r in resumenes),
             sum(r.insertados for r in resumenes), sum(r.sin_tamano for r in resumenes))


def configurar_logs():
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)-7s %(message)s",
                        datefmt="%Y-%m-%d %H:%M:%S")
    logging.Formatter.converter = lambda *_: datetime.now(HORA_COLOMBIA).timetuple()   # logs en hora de Colombia


def ejecutar(dominio, estrategia_para):
    """
    Corre todos los targets activos del dominio (nombre de la categoría raíz de sus tiendas).
    estrategia_para(nombre_tienda) devuelve la TiendaStrategy de esa tienda, o None.
    """
    configurar_logs()
    try:
        conn = conectar()
    except KeyError as e:
        log.error("Falta la variable de entorno %s", e)
        return 2
    except psycopg2.Error as e:
        log.error("No se pudo conectar a la base de datos: %s", e)
        return 2

    session = nueva_sesion()
    resumenes = []
    try:
        try:
            targets = leer_targets(conn, dominio)
        except DominioInexistente as e:
            log.error("%s: revisar el nombre del dominio del scraper", e)
            return 2
        solo = os.environ.get("SOLO_TIENDAS", "")
        if solo.strip():
            todos = len(targets)
            targets = filtrar_targets(targets, solo)
            log.info("SOLO_TIENDAS=%r: %d de %d scrape_targets", solo, len(targets), todos)
        log.info("%d scrape_targets activos de %s; precios de %s", len(targets), dominio, CIUDAD)
        if not targets:
            log.error("No hay scrape_targets activos para el dominio y las tiendas seleccionadas")
            return 2
        run_id = uuid4()
        iniciar_corrida(conn, run_id)
        bloqueos = {}          # store_id -> 403/429 seguidos
        tiendas_omitidas = set()
        for i, target in enumerate(targets):
            if target["store_id"] in tiendas_omitidas:
                resumenes.append(Resumen(target["store_name"], target["search_term"],
                                         error="omitido: la tienda bloqueó las peticiones (403/429)"))
                continue
            try:
                resumen, hubo_peticion = procesar_target(conn, session, target, estrategia_para, run_id)
            except Exception as e:      # ninguna falla de un target detiene a los demás
                conn.rollback()
                resumen = Resumen(target["store_name"], target["search_term"], error=f"inesperado: {e}")
                hubo_peticion = True
                log.exception("[%s][%s] error inesperado", resumen.tienda, resumen.termino)
            resumenes.append(resumen)
            sid = target["store_id"]
            if "HTTP 403" in resumen.error or "HTTP 429" in resumen.error:
                bloqueos[sid] = bloqueos.get(sid, 0) + 1
                if bloqueos[sid] >= BLOQUEOS_SEGUIDOS_MAX:
                    tiendas_omitidas.add(sid)
                    log.error("[%s] %d respuestas 403/429 seguidas: se omiten sus demás targets. "
                              "La tienda está bloqueando las peticiones desde este servidor.",
                              resumen.tienda, bloqueos[sid])
            elif hubo_peticion:
                bloqueos[sid] = 0
            if hubo_peticion and i < len(targets) - 1:
                time.sleep(getattr(estrategia_para(target["store_name"]), "pausa", None) or PAUSA)
        terminar_corrida(conn, run_id)
    finally:
        session.close()
        conn.close()

    imprimir_resumen(resumenes)
    vacias = tiendas_sin_ofertas(resumenes)
    for tienda in vacias:
        log.error("ALERTA: %s no insertó ninguna oferta en esta corrida; revisar su API y sus scrape_targets",
                  tienda)
    return 1 if vacias or any(r.error for r in resumenes) else 0
