# Helper image of the backup and restore scripts (Q3): curl on a current, pinned Alpine with every package upgraded.
# Replaces curlimages/curl, whose newest tag still ships a krb5 with HIGH findings that Alpine has fixed.
FROM alpine:3.24.4@sha256:f59abe656b692d901e184be3445e084786602108599d2a51d229c0f5a48c71bf
RUN apk upgrade --no-cache && apk add --no-cache curl && adduser -S -D -H ops
USER ops
ENTRYPOINT ["curl"]
