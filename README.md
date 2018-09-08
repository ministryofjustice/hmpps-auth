# NOMIS Oauth2 Server

Spring Boot 2, Java 10 OAUTH2 Server integrating with NOMIS DB

#### Run in docker

```bash
./gradlew assemble
docker build -t mojdigitalstudio/nomis-oauth2-server .
docker run -p8080:8080 --name nomis-oauth2-server --env SPRING_PROFILES_ACTIVE=dev mojdigitalstudio/nomis-oauth2-server:latest
``` 

#### Run in docker-compose
```bash
docker-compose up -d
```
The container should start in a few seconds and be available on port 9090

#### View running container:

```bash
docker ps
```
Will show a line like this:
```
CONTAINER ID        IMAGE                                         COMMAND                 CREATED             STATUS                    PORTS                    NAMES
d77af7e00910        mojdigitalstudio/nomis-oauth2-server:latest   "/bin/sh /app/run.sh"   38 seconds ago      Up 36 seconds (healthy)   0.0.0.0:9090->8080/tcp   nomis-oauth2-server
```

#### View logs in docker:
```docker logs nomis-oauth2-server```

### Profiles:
- dev - in memory database with 1 user ITAG_USER / password
- oracle - oracle DB integration with NOMIS DB, specify datasource url, username and password


### Get a JWT token
```bash
TOKEN=$(curl -X POST "http://localhost:8080/oauth/token?grant_type=password&username=ITAG_USER&password=password" -H 'Authorization: Basic ZWxpdGUyYXBpY2xpZW50OmNsaWVudHNlY3JldA==' | grep access_token | awk -F"\"" '{print $4}')
```
