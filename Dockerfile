# ----------------------------------------------------------------------
# ETAPA 1: Compilación (BUILDER STAGE) - YA COMPARTIDA
# ----------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-21 AS builder
LABEL maintainer="eshan@ipagos.lat"
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.wagon.http.ssl.insecure=true

# ----------------------------------------------------------------------
# ETAPA 2: Imagen Final (RUNTIME STAGE) - FALTA ESTO
# ----------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy

# Establecer el directorio de trabajo
WORKDIR /app

# Exponer el puerto de la aplicación (Cloud Run espera el puerto 8080 por defecto)
EXPOSE 8080

# Copiar el JAR compilado de la etapa 'builder'
# Usamos el nombre del artefacto final
COPY --from=builder /app/target/morganainvoices-0.0.1-SNAPSHOT.jar morganainvoices.jar

# Comando de ejecución
ENTRYPOINT ["java", "-jar", "morganainvoices.jar"]
