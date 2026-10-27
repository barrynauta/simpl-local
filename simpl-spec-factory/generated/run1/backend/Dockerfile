FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /src/target/vocabulary-manager.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=3s --retries=30 CMD curl -fs http://localhost:8080/v1/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
