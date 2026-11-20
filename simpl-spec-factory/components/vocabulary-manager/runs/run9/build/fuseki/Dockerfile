# Vocabulary store: Apache Jena Fuseki Main (no UI, no admin endpoints), resolved from Maven Central with checksums.
FROM maven:3.9-eclipse-temurin-21-alpine@sha256:308cba8b638ed7e4658cea3f8399066219466211c805f6d5728c3c9c7614661b AS deps
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -ntp dependency:copy-dependencies -DoutputDirectory=/build/lib -DincludeScope=runtime
COPY ops/StoreOps.java ops/StoreOps.java
RUN javac --release 21 -d /build/ops ops/StoreOps.java

FROM eclipse-temurin:21-jre-alpine-3.24@sha256:ac05c3c42b7c2f1a52c9bd52f01041fa1958231b15ebeae7420429cbc5baf474
RUN apk upgrade --no-cache \
    && addgroup -S -g 10001 fuseki && adduser -S -u 10001 -G fuseki -h /fuseki fuseki \
    && mkdir -p /fuseki/databases /fuseki/run /fuseki/backups \
    && chown -R fuseki:fuseki /fuseki
COPY --from=deps /build/lib /opt/fuseki/lib
COPY --from=deps /build/ops /opt/fuseki/ops
COPY config.ttl log4j2.properties /opt/fuseki/
COPY --chmod=0755 entrypoint.sh /opt/fuseki/entrypoint.sh
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -Dlog4j.configurationFile=/opt/fuseki/log4j2.properties"
USER 10001
WORKDIR /fuseki
EXPOSE 3030
ENTRYPOINT ["/opt/fuseki/entrypoint.sh"]
