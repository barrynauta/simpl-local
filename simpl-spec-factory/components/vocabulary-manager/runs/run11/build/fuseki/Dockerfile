# Apache Jena Fuseki 6.2.0 (E3) on a slim Java 21 runtime. The server jar is fetched from Maven
# Central and checked against a pinned SHA-256 (OWT-A08); its SHA-1 matches the one Central publishes.
FROM eclipse-temurin:21-jre-alpine@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474
ADD --checksum=sha256:2c92c598e65ab69d99820052e05f4277ed1d77980a8845f87545ad35b71a76ee \
    https://repo1.maven.org/maven2/org/apache/jena/jena-fuseki-server/6.2.0/jena-fuseki-server-6.2.0.jar \
    /fuseki/fuseki-server.jar
COPY config.ttl /fuseki/config.ttl
COPY entrypoint.sh /fuseki/entrypoint.sh
RUN addgroup -S -g 10001 fuseki && adduser -S -u 10001 -G fuseki -H fuseki \
 && mkdir -p /fuseki/databases /fuseki/run /backup \
 && chown -R fuseki:fuseki /fuseki/databases /fuseki/run /backup \
 && chmod 0444 /fuseki/fuseki-server.jar /fuseki/config.ttl && chmod 0555 /fuseki/entrypoint.sh
USER fuseki
WORKDIR /fuseki
EXPOSE 3030
ENTRYPOINT ["/fuseki/entrypoint.sh"]
