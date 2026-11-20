# Vocabulary Manager backend. Build stage runs Maven (tests run outside, in `mvn verify`); runtime is a slim JRE.
FROM maven:3.9-eclipse-temurin-21-alpine@sha256:308cba8b638ed7e4658cea3f8399066219466211c805f6d5728c3c9c7614661b AS build
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline -DexcludeReactor=true || true
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests -Djacoco.skip=true -Dspotless.check.skip=true \
    && java -Djarmode=tools -jar target/vocabulary-manager.jar extract --layers --launcher --destination /extracted

FROM eclipse-temurin:21-jre-alpine@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474
RUN apk upgrade --no-cache \
    && addgroup -S -g 10001 app && adduser -S -u 10001 -G app -h /app app
WORKDIR /app
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./
USER 10001
EXPOSE 8080 8081
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError -Djava.io.tmpdir=/tmp"
HEALTHCHECK --interval=5s --timeout=3s --start-period=40s --retries=24 \
  CMD wget -q -O /dev/null http://127.0.0.1:8081/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
