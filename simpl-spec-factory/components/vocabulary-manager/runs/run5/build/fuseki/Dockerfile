# syntax=docker/dockerfile:1.7
# Apache Jena Fuseki 6.2.0 for the 'vocabulary' dataset (decision E3).
# Base image pinned by digest (OWT-A03); the server jar is verified by its SHA-256 (OWT-A08).
FROM eclipse-temurin:21-jre-alpine@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474

RUN apk upgrade --no-cache \
    && addgroup -S fuseki && adduser -S -G fuseki -H fuseki \
    && mkdir -p /opt/fuseki /fuseki/databases \
    && chown -R fuseki:fuseki /fuseki

ADD --checksum=sha256:2c92c598e65ab69d99820052e05f4277ed1d77980a8845f87545ad35b71a76ee \
    https://repo1.maven.org/maven2/org/apache/jena/jena-fuseki-server/6.2.0/jena-fuseki-server-6.2.0.jar \
    /opt/fuseki/fuseki-server.jar
RUN chmod 0444 /opt/fuseki/fuseki-server.jar

USER fuseki
WORKDIR /fuseki
VOLUME ["/fuseki/databases"]
EXPOSE 3030
ENV JAVA_OPTS="-Xmx768m"

HEALTHCHECK --interval=5s --timeout=3s --start-period=20s --retries=20 \
    CMD wget -q -O /dev/null "http://127.0.0.1:3030/\$/ping" || exit 1

ENTRYPOINT ["sh", "-c", "mkdir -p /fuseki/databases/vocabulary && exec java $JAVA_OPTS -jar /opt/fuseki/fuseki-server.jar --tdb2 --loc=/fuseki/databases/vocabulary --update --ping --port 3030 /vocabulary"]
