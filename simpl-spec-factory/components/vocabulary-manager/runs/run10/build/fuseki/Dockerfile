# Apache Jena Fuseki (E3) for the Vocabulary Manager. Runs Fuseki Main in plain mode: no UI and no admin endpoints
# (D83). The server jar comes from Maven Central and is verified against a pinned SHA-256 (OWT-A08).
FROM eclipse-temurin:21-jre-alpine@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474

ARG JENA_VERSION=6.2.0
ARG JENA_SHA256=2c92c598e65ab69d99820052e05f4277ed1d77980a8845f87545ad35b71a76ee

RUN apk upgrade --no-cache \
    && addgroup -S -g 10001 fuseki \
    && adduser -S -u 10001 -G fuseki -h /fuseki fuseki \
    && mkdir -p /fuseki/databases /fuseki/run \
    && wget -q -O /fuseki/jena-fuseki-server.jar \
       "https://repo1.maven.org/maven2/org/apache/jena/jena-fuseki-server/${JENA_VERSION}/jena-fuseki-server-${JENA_VERSION}.jar" \
    && echo "${JENA_SHA256}  /fuseki/jena-fuseki-server.jar" | sha256sum -c - \
    && chown -R fuseki:fuseki /fuseki

COPY --chown=root:root config.ttl /fuseki/config.ttl
COPY --chown=root:root --chmod=0755 entrypoint.sh /fuseki/entrypoint.sh

USER 10001
WORKDIR /fuseki
VOLUME ["/fuseki/databases"]
EXPOSE 3030

HEALTHCHECK --interval=5s --timeout=3s --start-period=20s --retries=20 \
  CMD wget -q -O /dev/null http://127.0.0.1:3030/\$/ping || exit 1

ENTRYPOINT ["/fuseki/entrypoint.sh"]
