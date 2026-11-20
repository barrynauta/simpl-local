# Store tools for backup and restore (Q3, D89): curl on a current Alpine, run on the internal store network only.
FROM alpine:3@sha256:f59abe656b692d901e184be3445e084786602108599d2a51d229c0f5a48c71bf
RUN apk upgrade --no-cache && apk add --no-cache curl \
    && addgroup -S -g 10001 tools && adduser -S -u 10001 -G tools tools
USER 10001
ENTRYPOINT ["curl"]
