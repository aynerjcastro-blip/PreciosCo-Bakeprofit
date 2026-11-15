"""
Código común de los scrapers de PreciosCo.

Cada scraper (scrapers/supermercados, scrapers/droguerias, ...) es un punto de entrada que dice su
dominio y sus tiendas; lo que se repite entre ellos vive aquí:

    http.py      sesión con User-Agent del proyecto y peticiones con reintentos
    ciudad.py    ciudad de los precios (Valledupar) y hora de Colombia
    texto.py     comparación de textos sin tildes
    tamanos.py   contenido neto y unidad base desde el nombre o la descripción
    staging.py   conexión, lectura de scrape_targets por dominio y escritura en scraped_offers
    corrida.py   estrategia de tienda, recorrido de los targets, resumen y alerta de cero resultados
    vtex.py      estrategia para tiendas VTEX (API pública de catálogo y simulación de compra)
"""
