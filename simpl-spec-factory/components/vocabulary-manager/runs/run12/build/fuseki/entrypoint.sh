#!/bin/sh
# Writes shiro.ini from the passwords in the environment (never baked into the image),
# installs the dataset configuration and starts Fuseki (decision D83).
set -eu

: "${FUSEKI_READ_USER:=vmread}"
: "${FUSEKI_UPDATE_USER:=vmupdate}"
: "${FUSEKI_ADMIN_USER:=vmadmin}"
: "${FUSEKI_READ_PASSWORD:?FUSEKI_READ_PASSWORD is required}"
: "${FUSEKI_UPDATE_PASSWORD:?FUSEKI_UPDATE_PASSWORD is required}"
: "${FUSEKI_ADMIN_PASSWORD:?FUSEKI_ADMIN_PASSWORD is required}"

umask 077
cat > /fuseki/run/shiro.ini <<EOF
[main]
plainMatcher = org.apache.shiro.authc.credential.SimpleCredentialsMatcher
iniRealm.credentialsMatcher = \$plainMatcher

[users]
${FUSEKI_READ_USER} = ${FUSEKI_READ_PASSWORD}, reader
${FUSEKI_UPDATE_USER} = ${FUSEKI_UPDATE_PASSWORD}, reader, writer
${FUSEKI_ADMIN_USER} = ${FUSEKI_ADMIN_PASSWORD}, reader, writer, admin

[roles]
reader = *
writer = *
admin = *

[urls]
/\$/ping = anon
/\$/** = authcBasic, roles[admin]
/vocabulary/query = authcBasic, roles[reader]
/vocabulary/update = authcBasic, roles[writer]
/vocabulary/data = authcBasic, roles[admin]
/** = authcBasic, roles[admin]
EOF

rm -f /fuseki/configuration/vocabulary.ttl
cat /opt/fuseki/vocabulary.ttl > /fuseki/configuration/vocabulary.ttl

# shellcheck disable=SC2086
exec java ${JAVA_OPTS} -Dlog4j.configurationFile=/opt/fuseki/log4j2.properties -jar /opt/fuseki/fuseki-server.jar --shiro=/fuseki/run/shiro.ini --port=3030
