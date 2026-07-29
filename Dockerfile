# ---- Build stage: compila y empaqueta el fat jar con el Maven Wrapper ----
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Copiar primero el wrapper + pom para cachear la descarga de dependencias como capa aparte:
# mientras pom.xml no cambie, Docker reutiliza la capa de dependencias en rebuilds.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

# Ahora el codigo fuente; se reconstruye solo esta capa cuando cambia src/.
COPY src ./src
RUN ./mvnw -B -q -DskipTests clean package

# ---- Runtime stage: imagen JRE minima, solo el jar ----
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# Usuario no-root para no correr el proceso como root dentro del contenedor.
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
