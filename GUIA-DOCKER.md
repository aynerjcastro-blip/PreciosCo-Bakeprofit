# Guía de Docker para BakeProfit

Desde ahora la app se levanta con Docker. No necesitas instalar Java ni Node para correrla.

## Qué necesitas
- Docker Desktop instalado y **abierto** (que diga "Engine running").
- Git.

## Primera vez (una sola vez)

**1. Clona y entra a la rama**
```bash
git clone <url-del-repo>
cd <carpeta-del-repo>
```

**2. Crea tres archivos de variables.** Ninguno se sube a Git. Pídele los valores reales al Scrum Master por un canal privado, nunca por el chat del grupo.

| Archivo | Para qué | Contenido |
|---|---|---|
| `.env` (raíz, junto a `docker-compose.yml`) | Lo lee Compose al **construir** el frontend | `GOOGLE_CLIENT_ID=...` |
| `backend/.env` | Lo lee el backend al **arrancar** | Copia `backend/.env.example` y llena los valores |
| `frontend/.env` | Solo si corres el front **sin Docker** (`npm run dev`) | `VITE_GOOGLE_CLIENT_ID=...` |

Reglas importantes:
- El `GOOGLE_CLIENT_ID` debe ser **idéntico** en la raíz y en `backend/.env`. Si no, el login con Google da 401.
- Sin comillas, sin espacios al final, y guarda como **UTF-8 sin BOM** (en VS Code, abajo a la derecha debe decir `UTF-8`).
- Los nombres son exactos. Un typo (`GOOGLE_CLIENTE_ID`) rompe todo en silencio.

**3. Levanta todo**
```bash
docker compose up --build -d
```
La primera vez tarda varios minutos (descarga imágenes y compila).

**4. Abre la app**
- Frontend: http://localhost:5173
- Backend / Swagger: http://localhost:8080/swagger-ui.html

## Día a día

| Quiero... | Comando |
|---|---|
| Levantar todo | `docker compose up -d` |
| Ver logs del backend | `docker compose logs -f backend` |
| Ver logs del frontend | `docker compose logs -f frontend` |
| Ver qué está corriendo | `docker compose ps` |
| Parar sin borrar | `docker compose stop` |
| Parar y borrar contenedores | `docker compose down` |

## ¿Cuándo hay que reconstruir?

Esta es la parte donde más gente se confunde. Docker corre una **copia** del código, no tus archivos en vivo.

| Cambié... | Debo ejecutar |
|---|---|
| Código del frontend (JSX, CSS) | `docker compose up --build -d frontend` |
| Código del backend (Java) | `docker compose up --build -d backend` |
| `backend/.env` | `docker compose up -d --force-recreate backend` (`restart` **no** basta) |
| `.env` de la raíz | `docker compose up --build -d frontend` (el valor se incrusta al compilar) |
| Algo y no se ve reflejado | Casi seguro falta `--build`. Recarga con Ctrl+Shift+R |

Si estás desarrollando mucho el frontend, es más cómodo correrlo sin Docker (`cd frontend && npm run dev`, con su `frontend/.env`) y dejar solo el backend en Docker.

## Problemas comunes

| Error | Causa | Solución |
|---|---|---|
| `required variable GOOGLE_CLIENT_ID is missing a value` | Falta el `.env` de la raíz | Crearlo (paso 2). Compose lo exige para **cualquier** comando |
| El frontend no inicia: `unknown directive "server"` | `nginx.conf` guardado con BOM | Re-guardar como UTF-8 sin BOM y `--build` |
| Backend: `Could not resolve placeholder 'X'` | Falta la variable `X` en `backend/.env` | Agregarla y `--force-recreate backend` |
| Google: "client ID is not found" | Frontend construido sin el ID | Revisar `.env` de la raíz y `--build frontend` |
| Google: `origin_mismatch` | Origen no autorizado en Google Cloud | Agregar `http://localhost:5173` en "Orígenes de JavaScript autorizados" |
| Google: 401 / "audience distinto" | IDs distintos en front y back | Igualarlos y recrear el backend |
| Puerto 8080 o 5173 ocupado | Otro proceso lo usa | Cerrarlo, o `docker compose down` |
| Docker no responde | Docker Desktop apagado | Abrirlo y esperar "Engine running" |

Para comprobar qué ve el backend (sin exponer secretos):
```bash
docker exec bakeprofit-backend printenv GOOGLE_CLIENT_ID
```

## Reglas del equipo
1. **Nunca** subas un `.env`. Solo se versionan los `.env.example` (sin valores).
2. **Nunca** ejecutes `docker compose config` a secas: imprime contraseñas y secretos. Si lo necesitas, filtra: `docker compose config | grep VITE`.
3. Si un secreto se filtra (chat, commit, captura), se **rota** de inmediato; borrar el mensaje no basta.
4. Si agregas una variable nueva, súmala al `.env.example` correspondiente en el mismo PR.
5. Antes de pedir revisión, prueba tu rama con `docker compose up --build -d`, no solo con `npm run dev`.

## Limpieza (si algo quedó raro)
```bash
docker compose down
docker compose up --build -d
```
Para liberar espacio en disco: `docker system prune` (pide confirmación; no borra volúmenes).