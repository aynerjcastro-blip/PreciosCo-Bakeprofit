"""Ciudad de los precios y hora local de Colombia."""
from datetime import datetime, timedelta, timezone

# Ubicación de los precios: centro de Valledupar, como VTEX espera las coordenadas (longitud, latitud)
CIUDAD = "Valledupar"
COORDENADAS_CIUDAD = (-73.2532, 10.4631)

# Colombia no tiene horario de verano: UTC-5 fijo. Así scraped_at queda en la misma hora local que
# usa el backend, aunque el scraper corra en un runner de GitHub Actions (UTC) o en Windows (sin tzdata).
HORA_COLOMBIA = timezone(timedelta(hours=-5))


def ahora_colombia():
    """Hora local de Colombia sin zona, como la guardan las columnas TIMESTAMP del backend."""
    return datetime.now(HORA_COLOMBIA).replace(tzinfo=None)
