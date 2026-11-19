# Vocabulary Manager backend. Build and runtime images pinned by digest (OWT-A03).
FROM maven:3.9-eclipse-temurin-21-alpine@sha256:308cba8b638ed7e4658cea3f8399066219466211c805f6d5728c3c9c7614661b AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline -DexcludeArtifactIds=jena-fuseki-main || true
COPY src ./src
RUN mvn -B -q -DskipTests -Djacoco.skip=true package \
 && mkdir -p /build/extracted \
 && java -Djarmode=tools -jar target/vocabulary-manager-be.jar extract --layers --launcher --destination /build/extracted

FROM eclipse-temurin:21-jre-alpine@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474
RUN apk upgrade --no-cache \
 && addgroup -S app && adduser -S -G app -h /app app
WORKDIR /app
COPY --from=build --chown=app:app /build/extracted/dependencies/ ./
COPY --from=build --chown=app:app /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /build/extracted/application/ ./
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=3s --start-period=30s --retries=30 \
  CMD wget -q -O /dev/null http://127.0.0.1:8081/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
