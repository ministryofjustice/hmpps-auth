# HMPPS Oauth2 / SSO Server

Spring Boot 2.1, Java 11 OAUTH2 Server integrating with NOMIS DB and New Auth DB.

To get started, either run an instance locally, or point to the dev (t3) instance - https://gateway.t3.nomis-api.hmpps.dsd.io/auth/.
For t3 you will need client credentials to connect, ask in #newnomis_projectteam to get setup.

#### Run in docker-compose
```bash
docker-compose pull && docker-compose up -d
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

#### API Documentation

Is available on a running local server at http://localhost:9090/auth/swagger-ui.html.  Alternatively production
documentation can be found at https://gateway.prod.nomis-api.service.hmpps.dsd.io/auth/swagger-ui.html.  Don't forget to
include /auth in the requests if calling an api endpoint.`

#### Health

- `/ping`: will respond `pong` to all requests.  This should be used by dependent systems to check connectivity to auth,
rather than calling the `/health` endpoint.
- `/health`: provides information about the application health and its dependencies.  This should only be used
by auth health monitoring (e.g. pager duty) and not other systems who wish to find out the state of auth.
- `/info`: provides information about the version of deployed application.

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
