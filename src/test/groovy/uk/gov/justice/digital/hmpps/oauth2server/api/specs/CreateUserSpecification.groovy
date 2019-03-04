package uk.gov.justice.digital.hmpps.oauth2server.api.specs

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.http.*
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException

class CreateUserSpecification extends TestSpecification {
    class NewUser {
        String email
        String firstName
        String lastName
    }

    def jsonSlurper = new JsonSlurper()

    def "Create User endpoint succeeds to create user data"() {
        def username = RandomStringUtils.randomAlphanumeric(10)
        def user = new NewUser(email: "bob@bobdigital.justice.gov.uk", firstName: "Bob", lastName: "Smith")

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER_ADM", "password123456", "elite2apiclient", "clientsecret")

        when:
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<String>(new JsonBuilder(user).toPrettyString(), headers);
        def createUserResponse = oauthRestTemplate.exchange("${getBaseUrl()}/api/user/${username}", HttpMethod.PUT, entity, String.class)

        then:
        createUserResponse.statusCode == HttpStatus.OK

        def response = oauthRestTemplate.exchange("${getBaseUrl()}/api/user/${username}", HttpMethod.GET, null, String.class)
        def userData = jsonSlurper.parseText(response.body as String)

        userData == ["username": username.toUpperCase(), "active": true, "name": "Bob Smith", "authSource": "auth"]
    }

    def "Create User endpoint fails if no privilege"() {
        def username = RandomStringUtils.randomAlphanumeric(10)
        def user = new NewUser(email: "bob@bobdigital.justice.gov.uk", firstName: "Bob", lastName: "Smith")

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        when:
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<String>(new JsonBuilder(user).toPrettyString(), headers);

        then:
        try {
            def response = oauthRestTemplate.exchange("${getBaseUrl()}/api/user/${username}", HttpMethod.PUT, entity, String.class)
        } catch (OAuth2AccessDeniedException exception) {
            exception.message == "error=\"access_denied\", error_description=\"Access is denied\""
        }
    }
}
