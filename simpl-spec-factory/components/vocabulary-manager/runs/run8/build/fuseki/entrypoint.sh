#!/bin/sh
# Writes the password file from the environment (D83) and starts Fuseki main without admin endpoints.
set -eu
: "${STORE_READ_USER:?}" "${STORE_READ_PASSWORD:?}" "${STORE_UPDATE_USER:?}" "${STORE_UPDATE_PASSWORD:?}"
: "${STORE_BACKUP_USER:?}" "${STORE_BACKUP_PASSWORD:?}"
umask 077
cat > /fuseki/run/passwd <<PASSWD
${STORE_READ_USER}: ${STORE_READ_PASSWORD}
${STORE_UPDATE_USER}: ${STORE_UPDATE_PASSWORD}
${STORE_BACKUP_USER}: ${STORE_BACKUP_PASSWORD}
PASSWD
sed -e "s/@READ_USER@/${STORE_READ_USER}/g" -e "s/@UPDATE_USER@/${STORE_UPDATE_USER}/g" \
    -e "s/@BACKUP_USER@/${STORE_BACKUP_USER}/g" /fuseki/config.ttl > /fuseki/run/config.ttl
exec java ${JAVA_OPTS:--Xmx768m} -Dlog4j.configurationFile=/fuseki/log4j2.properties \
  -cp /fuseki/fuseki-server.jar org.apache.jena.fuseki.main.cmds.FusekiMainCmd \
  --config=/fuseki/run/config.ttl --passwd=/fuseki/run/passwd --auth=basic --ping --port=3030
