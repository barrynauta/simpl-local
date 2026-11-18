# syntax=docker/dockerfile:1.7
# Build stage: Maven with JDK 21, pinned by digest (OWT-A03).
FROM maven:3.9-eclipse-temurin-21-alpine@sha256:308cba8b638ed7e4658cea3f8399066219466211c805f6d5728c3c9c7614661b AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B dependency:go-offline -DexcludeArtifactIds=jena-fuseki-main || true
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests package \
    && java -Djarmode=tools -jar target/vocabulary-manager-be.jar extract --layers --launcher --destination /extracted

# Runtime stage: slim JRE 21 on Alpine, non-root.
FROM eclipse-temurin:21-jre-alpine@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474
RUN apk upgrade --no-cache && addgroup -S app && adduser -S -G app -H app
WORKDIR /app
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./
USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=5s --timeout=4s --start-period=40s --retries=24 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/v1/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
