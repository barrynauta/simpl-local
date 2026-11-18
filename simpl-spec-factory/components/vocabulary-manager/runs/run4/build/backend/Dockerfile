# Build stage: Maven with a cache for the local repository; tests run in 'mvn verify' outside the image build.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B dependency:go-offline -Dspotless.check.skip=true || true
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -B package -DskipTests -Djacoco.skip=true -Dspotless.check.skip=true

# Run stage: JRE only, non-root user.
FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --home /app app
WORKDIR /app
COPY --from=build /build/target/vocabulary-manager-be.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=3s --start-period=20s --retries=20 \
  CMD curl -fsS http://localhost:8080/v1/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
