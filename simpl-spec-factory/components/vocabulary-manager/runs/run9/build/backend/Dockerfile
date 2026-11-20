# Vocabulary Manager backend: Java 21, Spring Boot 4, built from source in the first stage.
FROM maven:3.9-eclipse-temurin-21-alpine@sha256:308cba8b638ed7e4658cea3f8399066219466211c805f6d5728c3c9c7614661b AS build
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -ntp dependency:go-offline -DexcludeReactor=true || true
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -ntp package -DskipTests -Djacoco.skip=true -Dspotless.check.skip=true -Dcyclonedx.skip=true

FROM eclipse-temurin:21-jre-alpine-3.24@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474
RUN apk upgrade --no-cache \
    && addgroup -S -g 10001 app && adduser -S -u 10001 -G app -h /app app
COPY --from=build /build/target/vocabulary-manager-be.jar /app/app.jar
USER 10001
WORKDIR /app
EXPOSE 8080 8081
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
