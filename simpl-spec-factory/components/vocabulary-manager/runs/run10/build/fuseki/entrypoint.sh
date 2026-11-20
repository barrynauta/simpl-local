#!/bin/sh
# Writes the password file from the environment (never baked into the image, D83) and starts Fuseki Main in plain
# mode with basic authentication and the access lists of config.ttl.
set -eu

: "${STORE_READ_PASSWORD:?STORE_READ_PASSWORD is required}"
: "${STORE_UPDATE_PASSWORD:?STORE_UPDATE_PASSWORD is required}"
: "${STORE_ADMIN_PASSWORD:?STORE_ADMIN_PASSWORD is required}"

PASSWD=/fuseki/run/passwd
umask 077
{
  echo "${STORE_READ_USER:-reader}: ${STORE_READ_PASSWORD}"
  echo "${STORE_UPDATE_USER:-writer}: ${STORE_UPDATE_PASSWORD}"
  echo "${STORE_ADMIN_USER:-admin}: ${STORE_ADMIN_PASSWORD}"
} > "${PASSWD}"

exec java ${JAVA_OPTS:-} -cp /fuseki/jena-fuseki-server.jar \
  org.apache.jena.fuseki.main.cmds.FusekiServerPlainCmd \
  --config=/fuseki/config.ttl --passwd="${PASSWD}" --auth=basic --port=3030
