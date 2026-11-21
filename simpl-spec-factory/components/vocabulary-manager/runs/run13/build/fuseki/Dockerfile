# Apache Jena Fuseki 6.2.0 (decision E3) on a pinned, slim JRE base image.
FROM eclipse-temurin:21-jre-alpine@sha256:5d7689f47d670aca766b9503a70bccc2d73b4eb9221ae8f714ae958313c4ce70

ARG FUSEKI_VERSION=6.2.0
# SHA-512 of jena-fuseki-server-6.2.0.jar, computed on download and cross-checked with the
# SHA-1 published on Maven Central (d3490295a2b95677c1227d25bca8564d40f993a7). OWT-A08.
ARG FUSEKI_SHA512=59c5d0fd4e3f2c51819816dd178ee7d619552aae12400286338cc55c6955653e6c503d58b6cb12f3124407c903a81a2ecf4a9b0d5ba8028e7fcdc7c5c98710d4

RUN apk upgrade --no-cache \
 && addgroup -S fuseki && adduser -S -G fuseki -h /fuseki fuseki \
 && mkdir -p /fuseki/databases /fuseki/run \
 && wget -q -O /fuseki/fuseki-server.jar \
      "https://repo1.maven.org/maven2/org/apache/jena/jena-fuseki-server/${FUSEKI_VERSION}/jena-fuseki-server-${FUSEKI_VERSION}.jar" \
 && echo "${FUSEKI_SHA512}  /fuseki/fuseki-server.jar" | sha512sum -c - \
 && chown -R fuseki:fuseki /fuseki

COPY --chown=fuseki:fuseki config.ttl entrypoint.sh /fuseki/
RUN chmod 0755 /fuseki/entrypoint.sh

USER fuseki
WORKDIR /fuseki
EXPOSE 3030
VOLUME ["/fuseki/databases"]
ENTRYPOINT ["/fuseki/entrypoint.sh"]
