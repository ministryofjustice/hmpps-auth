FROM openjdk:10-slim
MAINTAINER HMPPS Digital Studio <info@digital.justice.gov.uk>

WORKDIR /app

COPY build/libs/oauth2server*.jar /app/app.jar
COPY run.sh /app

ENTRYPOINT ["/bin/sh", "/app/run.sh"]
