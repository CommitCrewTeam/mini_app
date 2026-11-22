# ---- Stage 1: build ----
FROM gradle:8.10-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle :auth_service:bootJar --no-daemon

# ---- Stage 2: runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/auth_service/build/libs/auth_service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8091
ENTRYPOINT ["java", "-jar", "app.jar"]