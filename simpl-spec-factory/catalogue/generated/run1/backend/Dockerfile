# syntax=docker/dockerfile:1.7
# One builder stage shared by the three Spring Boot services; the final stage picks the module.
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /src
COPY pom.xml .
COPY backend/common/pom.xml backend/common/pom.xml
COPY backend/catalogue-be/pom.xml backend/catalogue-be/pom.xml
COPY backend/query-mapper-adapter/pom.xml backend/query-mapper-adapter/pom.xml
COPY backend/catalogue-client-be/pom.xml backend/catalogue-client-be/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -q -B dependency:go-offline -DexcludeReactor=true || true
COPY backend backend
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
ARG MODULE
RUN groupadd --system app && useradd --system --gid app --home /app app
WORKDIR /app
COPY --from=builder /src/backend/${MODULE}/target/app.jar /app/app.jar
USER app
EXPOSE 8000
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
HEALTHCHECK --interval=5s --timeout=3s --start-period=20s --retries=40 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8000 && printf "GET /health HTTP/1.0\r\n\r\n" >&3 && grep -q "UP" <&3'
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
