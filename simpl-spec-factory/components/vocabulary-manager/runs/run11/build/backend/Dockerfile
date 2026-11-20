# syntax=docker/dockerfile:1
# Vocabulary Manager backend: Java 21, Spring Boot 4. Base images pinned by digest (OWT-A03).
FROM maven:3.9-eclipse-temurin-21-alpine@sha256:308cba8b638ed7e4658cea3f8399066219466211c805f6d5728c3c9c7614661b AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline -DexcludeReactor=false || true
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests -Dspotless.check.skip=true -Djacoco.skip=true

FROM eclipse-temurin:21-jre-alpine@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474
RUN addgroup -S app && adduser -S -G app -H app
COPY --from=build /src/target/vocabulary-manager-be.jar /app/app.jar
USER app
EXPOSE 8080 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-XX:+ExitOnOutOfMemoryError", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
