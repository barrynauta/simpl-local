#!/bin/sh
# Writes the password file of the three store users from the environment (D83) and starts Fuseki main.
set -eu
: "${FUSEKI_READ_USER:?}" "${FUSEKI_READ_PASSWORD:?}" "${FUSEKI_UPDATE_USER:?}" "${FUSEKI_UPDATE_PASSWORD:?}"
: "${FUSEKI_ADMIN_USER:?}" "${FUSEKI_ADMIN_PASSWORD:?}"
umask 077
printf '%s: %s\n%s: %s\n%s: %s\n' \
  "$FUSEKI_READ_USER" "$FUSEKI_READ_PASSWORD" \
  "$FUSEKI_UPDATE_USER" "$FUSEKI_UPDATE_PASSWORD" \
  "$FUSEKI_ADMIN_USER" "$FUSEKI_ADMIN_PASSWORD" > /run/fuseki/passwd
sed -e "s/@READ@/$FUSEKI_READ_USER/g" -e "s/@UPDATE@/$FUSEKI_UPDATE_USER/g" -e "s/@ADMIN@/$FUSEKI_ADMIN_USER/g" \
  /opt/fuseki/config.ttl > /run/fuseki/config.ttl
exec java ${JAVA_OPTS:-} -Dlog4j.configurationFile=/opt/fuseki/log4j2.properties \
  -cp /opt/fuseki/fuseki-server.jar org.apache.jena.fuseki.main.cmds.FusekiMainCmd \
  --config=/run/fuseki/config.ttl --passwd=/run/fuseki/passwd --auth=basic --port 3030
