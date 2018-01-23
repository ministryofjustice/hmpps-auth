FROM openjdk:8-jdk-alpine
MAINTAINER HMPPS Digital Studio <info@digital.justice.gov.uk>
RUN  apk update && apk upgrade && apk add netcat-openbsd && apk add --update curl && rm -rf /var/cache/apk/*

ENV NAME nomis-oauth2-service
ENV JAR_PATH build/libs
ARG VERSION

WORKDIR /app

COPY ${JAR_PATH}/${NAME}*.jar /app
COPY run.sh /app

RUN chmod a+x /app/run.sh
ENTRYPOINT /app/run.sh