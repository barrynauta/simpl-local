# syntax=docker/dockerfile:1.7
# Vocabulary Manager backend: Java 21, Spring Boot 4.1.3. Tests run outside the image (mvn verify).
FROM maven:3.9-eclipse-temurin-21-alpine@sha256:308cba8b638ed7e4658cea3f8399066219466211c805f6d5728c3c9c7614661b AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B dependency:go-offline -DexcludeArtifactIds=google-java-format || true
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests -Dspotless.check.skip=true -Djacoco.skip=true package \
    && java -Djarmode=tools -jar target/vocabulary-manager-be.jar extract --layers --launcher --destination /extracted

FROM eclipse-temurin:21-jre-alpine@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474
RUN apk upgrade --no-cache && addgroup -S -g 10002 app && adduser -S -u 10002 -G app app
WORKDIR /app
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./
USER 10002:10002
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080 9090
HEALTHCHECK --interval=5s --timeout=3s --retries=40 --start-period=10s \
    CMD wget -q -O /dev/null http://127.0.0.1:9090/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
