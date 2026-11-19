# syntax=docker/dockerfile:1.7
# Build stage: Maven with a pinned JDK image. The Maven cache is a build cache mount, not part of the image.
FROM maven:3.9-eclipse-temurin-21-alpine@sha256:308cba8b638ed7e4658cea3f8399066219466211c805f6d5728c3c9c7614661b AS build
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline -DexcludeArtifactIds=jena-fuseki-main || true
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests -Dspotless.check.skip=true -Djacoco.skip=true \
    && cp target/vocabulary-manager-be.jar /build/app.jar

# Run stage: slim pinned JRE, non-root, no build tools.
FROM eclipse-temurin:21-jre-alpine@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app -h /app app
WORKDIR /app
COPY --from=build --chown=10001:10001 /build/app.jar /app/app.jar
USER 10001:10001
EXPOSE 8080 8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError -Djava.io.tmpdir=/tmp"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
HEALTHCHECK --interval=5s --timeout=4s --start-period=40s --retries=20 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/v1/health || exit 1
