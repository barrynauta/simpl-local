#!/bin/sh
# Writes the store users from the environment (D83, MACH-07) and starts Fuseki without admin endpoints.
set -eu
: "${FUSEKI_READ_PASSWORD:?missing}" "${FUSEKI_UPDATE_PASSWORD:?missing}" "${FUSEKI_BACKUP_PASSWORD:?missing}"
umask 077
mkdir -p /fuseki/run
{
  echo "reader: ${FUSEKI_READ_PASSWORD}"
  echo "updater: ${FUSEKI_UPDATE_PASSWORD}"
  echo "backup: ${FUSEKI_BACKUP_PASSWORD}"
} > /fuseki/run/passwd
exec java ${JAVA_OPTS:-} -cp /fuseki/fuseki-server.jar org.apache.jena.fuseki.main.cmds.FusekiServerPlainCmd \
  --config=/fuseki/config.ttl --port=3030 --ping
