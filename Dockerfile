# ----------------------------------------------------------------------
# ETAPA 1: Compilación (BUILDER STAGE)
# Usa la imagen JDK 21 de Maven para compilar el proyecto
# ----------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Etiqueta estándar para metadatos del contenedor
LABEL maintainer="eshan@ipagos.lat"

# Establecer el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiar el archivo de configuración de Maven (para manejar dependencias)
COPY pom.xml .

# Descargar dependencias para que el siguiente paso se beneficie de la caché de Docker
# Si las dependencias cambian, solo se reconstruye esta capa.
RUN mvn dependency:go-offline

# Copiar el código fuente restante
COPY src ./src

# Empaquetar la aplicación Spring Boot como un JAR ejecutable
# El comando DEBE ejecutarse con éxito para que la siguiente etapa funcione
RUN mvn clean package -DskipTests -Dmaven.wagon.http.ssl.insecure=true