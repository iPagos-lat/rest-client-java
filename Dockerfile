# ETAPA 1: Compilación (Builder Stage)
# Usa la imagen JDK 21 para compilar el proyecto
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Establecer el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiar el archivo de configuración de Maven y el código fuente
COPY pom.xml .
COPY src ./src

# Empaquetar la aplicación Spring Boot como un JAR ejecutable
# El nombre del JAR resultante será 'morganainvoices-0.0.1-SNAPSHOT.jar' o similar
RUN mvn clean package -DskipTests

# ETAPA 2: Imagen Final (Runtime Stage)
# Usamos una imagen base más ligera (JRE)
FROM eclipse-temurin:21-jre-jammy

# Establecer el directorio de trabajo
WORKDIR /app

# Copiar el JAR compilado de la etapa 'builder'
# Copiamos el archivo JAR resultante (asumiendo que Maven lo nombra con el comodín)
COPY --from=builder /app/target/*.jar morganainvoices.jar

# Exponer el puerto de la aplicación
EXPOSE 8080

# Comando de ejecución
ENTRYPOINT ["java", "-jar", "morganainvoices.jar"]