# Apache Jena Fuseki 6.3.0 (E3), run as "Fuseki main" without the UI and admin modules (D83).
# The server jar comes from Maven Central and is checked against a pinned SHA-512 (OWT-A08).
FROM eclipse-temurin:21-jre-alpine@sha256:5d7689f47d670aca766b9503a70bccc2d73b4eb9221ae8f714ae958313c4ce70 AS download
ARG JENA_VERSION=6.3.0
ARG FUSEKI_SHA512=9381800dd5644abbb6a347424229d2f0cfbf721a7afebe33a19c812a1196c3f6d558c34c854a96c671ea7d7b23141ccb163d0673c69e99236f04a2e51ca82505
RUN wget -q -O /tmp/fuseki-server.jar \
      "https://repo1.maven.org/maven2/org/apache/jena/jena-fuseki-server/${JENA_VERSION}/jena-fuseki-server-${JENA_VERSION}.jar" \
 && echo "${FUSEKI_SHA512}  /tmp/fuseki-server.jar" | sha512sum -c -

FROM eclipse-temurin:21-jre-alpine@sha256:5d7689f47d670aca766b9503a70bccc2d73b4eb9221ae8f714ae958313c4ce70
RUN apk upgrade --no-cache \
 && addgroup -S -g 10001 fuseki && adduser -S -u 10001 -G fuseki fuseki \
 && mkdir -p /fuseki/databases /fuseki/backups /run/fuseki \
 && chown -R fuseki:fuseki /fuseki /run/fuseki
COPY --from=download /tmp/fuseki-server.jar /opt/fuseki/fuseki-server.jar
COPY config.ttl log4j2.properties entrypoint.sh /opt/fuseki/
RUN chmod 0555 /opt/fuseki/entrypoint.sh
USER 10001
WORKDIR /fuseki
EXPOSE 3030
VOLUME ["/fuseki/databases"]
HEALTHCHECK --interval=5s --timeout=3s --start-period=20s --retries=20 \
  CMD wget -q -O /dev/null --header "Authorization: Basic $(printf '%s:%s' "$FUSEKI_READ_USER" "$FUSEKI_READ_PASSWORD" | base64)" "http://localhost:3030/\$/ping" || exit 1
ENTRYPOINT ["/opt/fuseki/entrypoint.sh"]
