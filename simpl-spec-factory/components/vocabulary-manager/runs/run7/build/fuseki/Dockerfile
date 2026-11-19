# Apache Jena Fuseki 6.2.0 (store of decision E3), from the official Maven Central artefact,
# verified by its SHA-512 (OWT-A08). Base image pinned by digest (OWT-A03).
FROM eclipse-temurin:21-jre-alpine@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474 AS download
ARG FUSEKI_VERSION=6.2.0
ARG FUSEKI_SHA512=59c5d0fd4e3f2c51819816dd178ee7d619552aae12400286338cc55c6955653e6c503d58b6cb12f3124407c903a81a2ecf4a9b0d5ba8028e7fcdc7c5c98710d4
RUN wget -q -O /tmp/fuseki-server.jar \
      "https://repo1.maven.org/maven2/org/apache/jena/jena-fuseki-server/${FUSEKI_VERSION}/jena-fuseki-server-${FUSEKI_VERSION}.jar" \
 && echo "${FUSEKI_SHA512}  /tmp/fuseki-server.jar" | sha512sum -c -

FROM eclipse-temurin:21-jre-alpine@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474
RUN apk upgrade --no-cache \
 && addgroup -S fuseki && adduser -S -G fuseki -h /fuseki fuseki \
 && mkdir -p /fuseki/databases /fuseki/run && chown -R fuseki:fuseki /fuseki
COPY --from=download --chown=fuseki:fuseki /tmp/fuseki-server.jar /opt/fuseki/fuseki-server.jar
USER fuseki
WORKDIR /fuseki
ENV FUSEKI_BASE=/fuseki/run \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+ExitOnOutOfMemoryError"
EXPOSE 3030
HEALTHCHECK --interval=5s --timeout=3s --start-period=20s --retries=20 \
  CMD wget -q -O /dev/null http://127.0.0.1:3030/\$/ping || exit 1
ENTRYPOINT ["/bin/sh", "-c", "mkdir -p /fuseki/databases/vocabulary && exec java -jar /opt/fuseki/fuseki-server.jar --tdb2 --loc=/fuseki/databases/vocabulary --update --ping --port=3030 /vocabulary"]
