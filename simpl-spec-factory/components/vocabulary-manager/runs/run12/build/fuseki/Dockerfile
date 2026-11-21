# syntax=docker/dockerfile:1.7
# Apache Jena Fuseki 6.2.0 (decision E3), TDB2 dataset 'vocabulary', Shiro basic auth with
# a read user, an update user and an admin user (decision D83). No host port (D107).
FROM eclipse-temurin:21-jre-alpine@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474

ARG JENA_VERSION=6.2.0
RUN apk upgrade --no-cache \
    && addgroup -S -g 10001 fuseki && adduser -S -u 10001 -G fuseki fuseki \
    && mkdir -p /opt/fuseki /fuseki/databases /fuseki/configuration /fuseki/backups /fuseki/run \
    && chown -R fuseki:fuseki /fuseki

ADD --checksum=sha256:2c92c598e65ab69d99820052e05f4277ed1d77980a8845f87545ad35b71a76ee \
    https://repo1.maven.org/maven2/org/apache/jena/jena-fuseki-server/${JENA_VERSION}/jena-fuseki-server-${JENA_VERSION}.jar \
    /opt/fuseki/fuseki-server.jar
COPY --chmod=0555 entrypoint.sh /opt/fuseki/entrypoint.sh
COPY --chmod=0444 vocabulary.ttl /opt/fuseki/vocabulary.ttl
COPY --chmod=0444 log4j2.properties /opt/fuseki/log4j2.properties
RUN chmod 0444 /opt/fuseki/fuseki-server.jar

USER 10001:10001
ENV FUSEKI_BASE=/fuseki \
    JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
VOLUME ["/fuseki/databases", "/fuseki/backups"]
EXPOSE 3030
HEALTHCHECK --interval=5s --timeout=3s --retries=30 --start-period=10s \
    CMD wget -q -O /dev/null http://127.0.0.1:3030/$/ping || exit 1
ENTRYPOINT ["/opt/fuseki/entrypoint.sh"]
