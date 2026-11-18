# syntax=docker/dockerfile:1

# Etapa 1: instala dependencias y genera el build de producción
FROM node:22-alpine AS build

WORKDIR /app

# Copia primero los archivos de dependencias
COPY package*.json ./

# Instala dependencias de forma reproducible
RUN npm ci

# Copia el resto del código React
COPY . .

# Vite incrusta las variables VITE_* al COMPILAR (no al ejecutar el contenedor),
# por eso se reciben aquí como argumento de build.
ARG VITE_GOOGLE_CLIENT_ID
ENV VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID

# Genera los archivos estáticos en /app/dist
RUN npm run build

# Etapa 2: sirve los archivos estáticos con Nginx
FROM nginx:stable-alpine AS production

# Configuración personalizada de Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copia el build de React al directorio servido por Nginx
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]