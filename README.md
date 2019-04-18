# HMPPS Oauth2 / SSO Server

Spring Boot 2.1, Java 11 OAUTH2 Server integrating with NOMIS DB and New Auth DB

#### Run in docker

```bash
./gradlew assemble
docker build -t mojdigitalstudio/nomis-oauth2-server .
docker run -p9090:8080 --name nomis-oauth2-server -d --health-cmd='curl -f http://localhost:8080/auth/health' --env SPRING_PROFILES_ACTIVE=dev mojdigitalstudio/nomis-oauth2-server:latest
``` 

### Run against oracle DB (T3 example)
```bash
docker run -p9090:8080 --name nomis-oauth2-server -d --health-cmd='curl -f http://localhost:8080/auth/health' \
 --env SPRING_PROFILES_ACTIVE=dev-config,oracle,auth-seed --env SPRING_DATASOURCE_PASSWORD=************ --env SPRING_DATASOURCE_URL=jdbc:oracle:thin:@docker.for.mac.localhost:1521/CNOMT3 --env SPRING_DATASOURCE_USERNAME=API_PROXY_USER \
 mojdigitalstudio/nomis-oauth2-server:latest
```
Or, where 'docker.for.mac.localhost' or its windows equivalent is not available e.g. Ubuntu, we can connect to the host network instead of publishing the port:
```
docker run --network=host --name nomis-oauth2-server-oracle -d \
 --health-cmd='curl -f http://localhost:9090/auth/health' \
 --env SPRING_DATASOURCE_USERNAME=API_PROXY_USER \
 --env SPRING_DATASOURCE_PASSWORD=xxxxxxxxxx  \
 --env SPRING_PROFILES_ACTIVE=dev,oracle,auth-seed \
 --env SPRING_DATASOURCE_URL=jdbc:oracle:thin:@localhost:1521:CNOMT3 \
 --env SERVER_PORT=9090 \
 mojdigitalstudio/nomis-oauth2-server:latest
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
- dev-config - development configuration
- auth-seed - seed auth database with api clients and sample users
- nomis-seed - create tables and seed nomis database with sample users
- dev - development configuration plus seeding of both databases
- oracle - oracle DB integration with NOMIS DB, specify datasource url, username and password

### Get a JWT token
```bash
curl -sX POST "http://localhost:9090/auth/oauth/token?grant_type=password&username=ITAG_USER&password=password" -H 'Authorization: Basic ZWxpdGUyYXBpY2xpZW50OmNsaWVudHNlY3JldA==' | jq .access_token
```
This requires `jq` - https://stedolan.github.io/jq/.  Note that this example uses the client id of elite2apiclient in 
the authorization header, which allows password, authorization_code and refresh token grants.

#### Update govuk toolkit:
``` ./get-govuk-frontend.bash <version>```

This will go to github to get the specified version of the front end toolkit.  Very noddy at present, so if the assets have changed at https://github.com/alphagov/govuk-frontend/tree/master/dist/assets/fonts for the specified version in question, then the list in the bash script will need updating to cope.

It will sort out the css references to `/assets` as we run in a `/auth` context instead. 
