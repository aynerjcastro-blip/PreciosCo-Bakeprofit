# Evidencia de verificación: filtros por sección

- Backend: suite completa con PostgreSQL aislado, Java 25, migraciones y procesador
  programado apagado. 239 pruebas, cero fallos, errores y omitidas.
- Cuatro integraciones nuevas verifican separación entre dominios, compatibilidad
  global sin sectionId, raíces activas y rechazo de hoja/raíz inactiva/ID inexistente.
- Frontend: npm ci, npm run lint y npm run build.
- Chromium: ocho pruebas en `frontend/tests/test_search_sections.py`. Incluyen las
  tres secciones, «Todas», cambio de sector y Atrás, URL con tienda ajena, respuesta
  tardía, error/reintento, filtros vacíos, sector desconocido y viewport móvil con
  limpieza de búsqueda por teclado y recuperación de foco.
- Auditoría SQL preflight: ejecutada en PostgreSQL local, transacción de solo lectura.
- DESIGN.md: lint oficial sin errores ni advertencias.
- Auditoría estática premium de todo frontend/src: nueve hallazgos existentes fuera
  del buscador: botón sin acción en Home, seis formularios sin declaración noValidate
  y dos textareas de reseñas sin declaración de resize. No se alteró validación de
  autenticación ni otros formularios para solucionar un filtro de búsqueda.
  El informe estático no sustituye las pruebas reales del flujo modificado.

## Reproducción de navegador

En una terminal, desde frontend: `npm ci` y `npm run dev`.
En otra, desde la raíz:

```powershell
python -m pip install playwright
python -m playwright install chromium
python -m unittest discover -s frontend/tests -p "test_*.py" -v
```

Estas pruebas simulan respuestas HTTP de la API; no envían datos a ninguna base.
Las integraciones Java sí necesitan una base aislada con esquema completo.
