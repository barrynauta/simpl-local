# Apache Jena Fuseki (E3) built from the official release jar on Maven Central, checksum-verified (OWT-A08).
# Runs FusekiMainCmd: no admin endpoints, no UI (D83). Users and passwords come from the environment.
FROM eclipse-temurin:21-jre-alpine@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474

ARG JENA_VERSION=6.2.0
ARG JENA_SHA512=59c5d0fd4e3f2c51819816dd178ee7d619552aae12400286338cc55c6955653e6c503d58b6cb12f3124407c903a81a2ecf4a9b0d5ba8028e7fcdc7c5c98710d4

RUN apk upgrade --no-cache \
    && addgroup -S fuseki && adduser -S -G fuseki -h /fuseki fuseki \
    && mkdir -p /fuseki/databases /fuseki/run \
    && wget -q -O /fuseki/fuseki-server.jar \
       "https://repo1.maven.org/maven2/org/apache/jena/jena-fuseki-server/${JENA_VERSION}/jena-fuseki-server-${JENA_VERSION}.jar" \
    && echo "${JENA_SHA512}  /fuseki/fuseki-server.jar" | sha512sum -c - \
    && chown -R fuseki:fuseki /fuseki

COPY --chown=fuseki:fuseki config.ttl log4j2.properties entrypoint.sh /fuseki/
RUN chmod 0555 /fuseki/entrypoint.sh

USER fuseki
WORKDIR /fuseki
EXPOSE 3030
VOLUME /fuseki/databases
HEALTHCHECK --interval=5s --timeout=3s --retries=30 CMD wget -q -O /dev/null http://127.0.0.1:3030/\$/ping || exit 1
ENTRYPOINT ["/fuseki/entrypoint.sh"]
