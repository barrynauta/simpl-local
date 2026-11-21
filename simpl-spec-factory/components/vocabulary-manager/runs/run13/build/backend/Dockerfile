# syntax=docker/dockerfile:1
# Vocabulary Manager backend. Build stage: Maven on Temurin 21; run stage: Temurin 21 JRE on Alpine.
# The own tests run in the finish line (mvn verify); the image build skips them to stay fast.
FROM maven:3.9-eclipse-temurin-21-alpine@sha256:308cba8b638ed7e4658cea3f8399066219466211c805f6d5728c3c9c7614661b AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline -DexcludeReactor=true || true
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests -Djacoco.skip=true -Dspotless.check.skip=true \
 && java -Djarmode=tools -jar target/vocabulary-manager-be.jar extract --layers --launcher --destination /extracted

FROM eclipse-temurin:21-jre-alpine@sha256:5d7689f47d670aca766b9503a70bccc2d73b4eb9221ae8f714ae958313c4ce70
RUN apk upgrade --no-cache && addgroup -S app && adduser -S -G app -h /app app
WORKDIR /app
COPY --from=build --chown=app:app /extracted/dependencies/ ./
COPY --from=build --chown=app:app /extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /extracted/application/ ./
USER app
EXPOSE 8080 8081
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=6 \
  CMD wget -q -O /dev/null http://127.0.0.1:8081/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
