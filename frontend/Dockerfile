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