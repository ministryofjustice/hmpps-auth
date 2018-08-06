# NOMIS Oauth2 Server

Spring Boot 2, Java 10 OAUTH2 Server intergrating with NOMIS DB

#### Run in docker

```bash
./gradlew assemble
docker build -t mojdigitalstudio/nomis-oauth2-server .
docker run -p8080:8080 --name nomis-oauth2-server --env SPRING_PROFILES_ACTIVE=dev mojdigitalstudio/nomis-oauth2-server:latest
``` 

Profiles:
- dev - in memory database with 1 user ITAG_USER / password
- oracle - oracle DB integration with NOMIS DB, specify datasource url, username and password


### Get a JWT token
```bash
TOKEN=$(curl -X POST "http://localhost:8080/oauth/token?grant_type=password&username=ITAG_USER&password=password" -H 'Authorization: Basic ZWxpdGUyYXBpY2xpZW50OmNsaWVudHNlY3JldA==' | grep access_token | awk -F"\"" '{print $4}')
```
