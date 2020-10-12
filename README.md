# HMPPS Oauth2 / SSO Server

[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-auth/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-auth)
[![API docs](https://img.shields.io/badge/API_docs-view-85EA2D.svg?logo=swagger)](https://sign-in-dev.hmpps.service.justice.gov.uk/auth/swagger-ui.html)

Spring Boot 2.1, Java 11 OAUTH2 Server integrating with NOMIS database, DELIUS (via community api) and an auth database for storing external users.

Please raise any questions on the server on the #dps_tech_team channel in slack - https://mojdt.slack.com/archives/CK0QLCQ1G.

To get started, either run an instance locally, or point to the dev (t3) instance - https://sign-in-dev.hmpps.service.justice.gov.uk/auth/.
For dev (t3) you will need client credentials to connect, ask in #dps_tech_team to get setup.

### Code style & formatting
```bash
./gradlew ktlintApplyToIdea addKtlintFormatGitPreCommitHook
```
will apply ktlint styles to intellij and also add a pre-commit hook to format all changed kotlin files.

### Run locally on the command line
```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

The service should start up using the dev profile, perform the flyway migrations on its local HSQLDB and then seed local development data.
Can then be accessed in a browser on http://localhost:8080/auth/login

### Run locally with token verification and delius enabled (for integration tests) on the command line
```bash
SPRING_PROFILES_ACTIVE=dev,token-verification,delius,azure-oidc-int-test ./gradlew bootRun
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

### Run locally against a SQL Server database
Auth by default runs against an in memory h2 database.  Sometimes it is necessary to run against an sql server database
i.e. if making database changes and need to verify that they work before being deployed to a test environment.

Steps are:

* Run a local docker container
```
docker stop sql1 && docker rm sql1 && docker run -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=YourStrong!Passw0rd' -p 1433:1433 --name sql1 -d mcr.microsoft.com/mssql/server:2017-latest
```
* Within Intellij set the active profile to `dev` and override the following parameters
```
auth.datasource.url=jdbc:sqlserver://localhost\sql1:1433
auth.datasource.username=sa
auth.datasource.password=YourStrong!Passw0rd
auth.jpa.hibernate.dialect=org.hibernate.dialect.SQLServer2012Dialect
```

### H2 database consoles 

When running locally with the SPRING_ACTIVE_PROFILES=dev the seeded H2 database consoles are available at http://localhost:8080/auth/h2-console

| Database | JDBC connection     |  username | password |
|----------|---------------------|-----------|----------|
| NOMIS    | jdbc:h2:mem:nomisdb  |  `<blank>`  | `<blank>`  |
| AUTH     | jdbc:h2:mem:authdb |  `<blank>`  | `<blank>`  |

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

### Enabling Azure AD OIDC as an additional authentication provider

It is possible to enable Azure AD as an external OIDC provider. This functionality is enabled by adding a client registration, detailed below.

#### Prerequisites
- Access to an Azure instance, and permissions to create app registrations. For development purposes we have been
creating registrations on the `DEVL` Azure instance, which is operated by the PTTP team. 

#### Creating an Azure app registration
1. Navigate to the app registrations page https://portal.azure.com/#blade/Microsoft_AAD_IAM/ActiveDirectoryMenuBlade/RegisteredApps
2. Create a new app registration. For "Supported account types", select: "Accounts in any organizational directory (Any Azure AD directory - Multitenant)". Set the redirect URL to `http://localhost:8080/auth/login/oauth2/code/microsoft`
3. Save the registration. Note the Client ID and Directory ID.
4. Navigate to the Certificates & Secrets tab, and add a new client secret, and note the secret value

#### Configuring Auth to use Azure AD OIDC
Set the active Spring profiles to additionally include the `azure-oidc` profile, then provide values for the following Spring properties:
```
auth.azureoidc.client_id=<client_id>
auth.azureoidc.client_secret=<client_secret>
auth.azureoidc.tenant_id=<tenant_id>
``` 
