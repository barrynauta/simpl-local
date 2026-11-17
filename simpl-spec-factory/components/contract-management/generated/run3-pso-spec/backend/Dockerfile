# One build stage for the whole Maven reactor, one runtime target per service.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY common/pom.xml common/
COPY contract-manager/pom.xml contract-manager/
COPY contract-ui-backend/pom.xml contract-ui-backend/
COPY stubs/pom.xml stubs/
RUN --mount=type=cache,target=/root/.m2 mvn -q -B dependency:go-offline -DexcludeReactor=true || true
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre AS runtime-base
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app --no-create-home app
WORKDIR /app
USER 10001:10001
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

FROM runtime-base AS cm
COPY --from=build /src/contract-manager/target/contract-manager.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM runtime-base AS uibe
COPY --from=build /src/contract-ui-backend/target/contract-ui-backend.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM runtime-base AS stubs
COPY --from=build /src/stubs/target/stubs.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
