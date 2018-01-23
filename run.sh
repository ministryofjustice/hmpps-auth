#!/bin/sh
echo "********************************************************"
echo "Starting Nomis OAUTH2 Server                            "
echo "********************************************************"
NAME=${NAME:-nomis-oauth2-server}
JAR=$(find . -name ${NAME}*.jar|head -1)
java ${JAVA_OPTS} -Dcom.sun.management.jmxremote.local.only=false -Djava.security.egd=file:/dev/./urandom -jar "${JAR}"

