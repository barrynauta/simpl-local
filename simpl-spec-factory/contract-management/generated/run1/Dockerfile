# syntax=docker/dockerfile:1.7
# One Dockerfile for the three Java services. The build stage is shared (and cached) by all of them.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY common/pom.xml common/
COPY contract-manager/pom.xml contract-manager/
COPY contract-ui-backend/pom.xml contract-ui-backend/
COPY stubs/pom.xml stubs/
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -ntp dependency:go-offline -DskipTests || true
COPY common common
COPY contract-manager contract-manager
COPY contract-ui-backend contract-ui-backend
COPY stubs stubs
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -ntp package -DskipTests

FROM eclipse-temurin:21-jre AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --no-create-home app
ARG MODULE
COPY --from=build /src/${MODULE}/target/app.jar /app/app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-XX:+UseSerialGC", "-jar", "/app/app.jar"]
