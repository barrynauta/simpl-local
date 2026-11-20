#!/bin/sh
# Writes the password file from the environment (D83, MACH-07) and starts Fuseki Main.
set -eu
: "${STORE_READ_PASSWORD:?STORE_READ_PASSWORD is required}"
: "${STORE_UPDATE_PASSWORD:?STORE_UPDATE_PASSWORD is required}"
: "${STORE_ADMIN_PASSWORD:?STORE_ADMIN_PASSWORD is required}"
umask 077
PASSWD=/fuseki/run/passwd
mkdir -p /fuseki/run
{
  echo "vmread: ${STORE_READ_PASSWORD}"
  echo "vmupdate: ${STORE_UPDATE_PASSWORD}"
  echo "vmadmin: ${STORE_ADMIN_PASSWORD}"
} > "$PASSWD" || { echo "cannot write $PASSWD" >&2; exit 1; }
exec java ${JAVA_OPTS:-} -cp '/opt/fuseki/lib/*' org.apache.jena.fuseki.main.cmds.FusekiMainCmd \
  --config=/opt/fuseki/config.ttl --passwd="$PASSWD" --auth=basic --ping --no-cors --port=3030
