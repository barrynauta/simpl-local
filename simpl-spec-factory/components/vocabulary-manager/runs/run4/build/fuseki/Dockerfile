# Apache Jena Fuseki 5.6.0 (decision E3), the same Jena version as the backend library.
# The server jar comes from Maven Central and is checked against its SHA-512 before use (OWT-A08);
# the SHA-1 published next to it on Maven Central was checked when the hash was pinned.
FROM eclipse-temurin:21-jre
ARG FUSEKI_VERSION=5.6.0
ARG FUSEKI_SHA512=6892853e217afde119f9237ad424033c4d309ec7129bed344d0199ad6e233d9f166610e2040abd42b739ecbe120c4638f2b69ecf1576872669a3403c3391f691
RUN groupadd --system fuseki && useradd --system --gid fuseki --home /fuseki fuseki \
 && mkdir -p /fuseki/databases/vocabulary \
 && curl -fsSL -o /fuseki/fuseki-server.jar \
    "https://repo1.maven.org/maven2/org/apache/jena/jena-fuseki-server/${FUSEKI_VERSION}/jena-fuseki-server-${FUSEKI_VERSION}.jar" \
 && echo "${FUSEKI_SHA512}  /fuseki/fuseki-server.jar" | sha512sum -c - \
 && chown -R fuseki:fuseki /fuseki
USER fuseki
WORKDIR /fuseki
VOLUME /fuseki/databases
EXPOSE 3030
HEALTHCHECK --interval=5s --timeout=3s --start-period=10s --retries=20 \
  CMD curl -fsS -o /dev/null 'http://localhost:3030/vocabulary/query?query=ASK%7B%7D' || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/fuseki/fuseki-server.jar", \
            "--port=3030", "--tdb2", "--loc=/fuseki/databases/vocabulary", "--update", "/vocabulary"]
