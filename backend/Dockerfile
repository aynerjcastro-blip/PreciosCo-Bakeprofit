# syntax=docker/dockerfile:1

# Etapa 1: compila la aplicación Spring Boot con Maven y JDK
FROM maven:3.9-eclipse-temurin-25 AS build

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copia primero pom.xml para aprovechar la caché de Docker
COPY pom.xml .

# Descarga dependencias antes de copiar el código fuente
RUN mvn dependency:go-offline -B

# Copia el código fuente del backend
COPY src ./src

# Empaqueta la aplicación en un JAR; omite tests para builds locales
RUN mvn clean package -DskipTests

# Etapa 2: imagen final liviana, solo con JRE
FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

# Crea un usuario sin privilegios de root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copia el JAR generado en la etapa de build
COPY --from=build /app/target/*.jar app.jar

# Puerto por defecto de Spring Boot
EXPOSE 8080

# Comando que inicia Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]