#!/bin/sh
# Writes the password file from the environment (secrets never in the image, MACH-07) and starts Fuseki.
set -eu
: "${FUSEKI_READ_PASSWORD:?FUSEKI_READ_PASSWORD is required}"
: "${FUSEKI_UPDATE_PASSWORD:?FUSEKI_UPDATE_PASSWORD is required}"
: "${FUSEKI_ADMIN_PASSWORD:?FUSEKI_ADMIN_PASSWORD is required}"
umask 077
{
  echo "reader: ${FUSEKI_READ_PASSWORD}"
  echo "writer: ${FUSEKI_UPDATE_PASSWORD}"
  echo "admin: ${FUSEKI_ADMIN_PASSWORD}"
} > /fuseki/run/passwd
exec java ${JAVA_OPTS:-} -jar /fuseki/fuseki-server.jar \
  --config=/fuseki/config.ttl --passwd=/fuseki/run/passwd --auth=basic --compact --port=3030
