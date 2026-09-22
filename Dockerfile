# Etapa 1: construcción
# NOTA: requiere que ./mvnw y .mvn/wrapper/ ya existan en el repositorio
# (ver instrucciones de wrapper en el siguiente paso del instructivo).
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Etapa 2: ejecución
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Documentativo: en local/docker-compose la app escucha en 8080 (no se define
# PORT). En Render, la plataforma inyecta su propia variable de entorno PORT
# en el contenedor y la app la respeta (server.port=${PORT:8080} en
# application.yml) — el puerto real en ese caso NO es este 8080.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
