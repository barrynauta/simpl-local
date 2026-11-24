# Vocabulary Manager backend: built with Maven inside Docker, run on a slim JRE as a non-root user.
FROM maven:3.9-eclipse-temurin-21-alpine@sha256:308cba8b638ed7e4658cea3f8399066219466211c805f6d5728c3c9c7614661b AS build
WORKDIR /src
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B -DskipTests -Dspotless.check.skip=true -Djacoco.skip=true package

FROM eclipse-temurin:21-jre-alpine@sha256:5d7689f47d670aca766b9503a70bccc2d73b4eb9221ae8f714ae958313c4ce70
RUN apk upgrade --no-cache \
 && addgroup -S -g 10001 app && adduser -S -u 10001 -G app app
COPY --from=build /src/target/vocabulary-manager-be.jar /app/app.jar
USER 10001
EXPOSE 8080 8081
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError -Djava.io.tmpdir=/tmp"
HEALTHCHECK --interval=5s --timeout=3s --start-period=40s --retries=30 \
  CMD wget -q -O /dev/null http://localhost:8081/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
