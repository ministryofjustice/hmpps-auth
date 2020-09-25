# HMPPS Oauth2 / SSO Server

Spring Boot 2.1, Java 11 OAUTH2 Server integrating with NOMIS database, DELIUS (via community api) and an auth database for storing external users.

Please raise any questions on the server on the #dps_tech_team channel in slack - https://mojdt.slack.com/archives/CK0QLCQ1G.

To get started, either run an instance locally, or point to the dev (t3) instance - https://sign-in-dev.hmpps.service.justice.gov.uk/auth/.
For dev (t3) you will need client credentials to connect, ask in #dps_tech_team to get setup.

### Run locally on the command line
```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

The service should start up using the dev profile, perform the flyway migrations on its local HSQLDB and then seed local development data.
Can then be accessed in a browser on http://localhost:8080/auth/login

### Run locally with token verification and delius enabled (for integration tests) on the command line
```bash
SPRING_PROFILES_ACTIVE=dev,token-verification,delius ./gradlew bootRun
```

### Run integration tests locally against the development instance (in a separate terminal) with:
```bash
./gradlew testFluentIntegration
```

(Will require a matching version of chromedriver to be downloaded and available on the path - check the version of selenium in build.gradle.kts)

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
CONTAINER ID        IMAGE                                         COMMAND                 CREATED             STATUS                    PORTS           NAMES
d77af7e00910        quay.io/hmpps/hnpps-auth:latest   "/bin/sh /app/run.sh"   38 seconds ago      Up 36 seconds (healthy)   0.0.0.0:9090->8080/tcp   hmpps-auth
```

#### View logs in docker:
```docker logs hmpps-auth```


### H2 database consoles 

When running locally with the SPRING_ACTIVE_PROFILES=dev the seeded H2 database consoles are available at http://localhost:8080/auth/h2-console

| Database | JDBC connection     |  username | password |
|----------|---------------------|-----------|----------|
| NOMIS    | jdbc:h2:mem:nomisdb  |  <blank>  | <blank>  |
| AUTH     | jdbc:h2:mem:authdb |  <blank>  | <blank>  |

#### API Documentation

Is available on a running local server at http://localhost:9090/auth/swagger-ui.html.  Alternatively production
documentation can be found at https://sign-in.hmpps.service.justice.gov.uk/auth/swagger-ui.html.  Don't forget to
include /auth in the requests if calling an api endpoint.`

#### Health

- `/health/ping`: will respond `{"status":"UP"}` to all requests.  This should be used by dependent systems to check connectivity to auth,
rather than calling the `/health` endpoint.
- `/health`: provides information about the application health and its dependencies.  This should only be used
by auth health monitoring (e.g. pager duty) and not other systems who wish to find out the state of auth.
- `/info`: provides information about the version of deployed application.

### Profiles:
- dev - development configuration plus seeding of both databases
- dev-config - development configuration
- auth-seed - seed auth database with api clients and sample users
- nomis-seed - create tables and seed nomis database with sample users
- oracle - oracle DB integration with NOMIS DB, specify datasource url, username and password
- token-verification - turns on token verification, requires the token verification api server running
- delius - turns on integration with delius, requires community api server running

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
