# syntax=docker/dockerfile:1.7
# Apache Jena Fuseki 6.2.0 (decision E3) on a slim, pinned JRE. The server jar comes from Maven Central and is
# checked against a pinned SHA-256 before use (OWT-A08).
FROM eclipse-temurin:21-jre-alpine@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474

ARG FUSEKI_VERSION=6.2.0
ADD --checksum=sha256:2c92c598e65ab69d99820052e05f4277ed1d77980a8845f87545ad35b71a76ee \
    https://repo1.maven.org/maven2/org/apache/jena/jena-fuseki-server/${FUSEKI_VERSION}/jena-fuseki-server-${FUSEKI_VERSION}.jar \
    /opt/fuseki/fuseki-server.jar

RUN addgroup -S -g 10001 fuseki && adduser -S -u 10001 -G fuseki -h /opt/fuseki fuseki \
    && mkdir -p /fuseki/databases && chown -R fuseki:fuseki /fuseki /opt/fuseki \
    && chmod 0444 /opt/fuseki/fuseki-server.jar

USER 10001:10001
WORKDIR /opt/fuseki
VOLUME ["/fuseki/databases"]
EXPOSE 3030

ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError" FUSEKI_BASE=/tmp/fuseki-base
# No admin endpoints; the dataset is reachable only on the internal network of the stack.
ENTRYPOINT ["sh", "-c", "mkdir -p /fuseki/databases/vocabulary && exec java $JAVA_OPTS -jar /opt/fuseki/fuseki-server.jar --tdb2 --loc=/fuseki/databases/vocabulary --update --ping --no-cors --port=3030 /vocabulary"]

HEALTHCHECK --interval=5s --timeout=3s --start-period=20s --retries=12 \
  CMD wget -q -O /dev/null http://127.0.0.1:3030/\$/ping || exit 1
