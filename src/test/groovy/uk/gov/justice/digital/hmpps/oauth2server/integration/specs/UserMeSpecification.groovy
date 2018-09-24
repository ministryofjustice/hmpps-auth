package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import groovy.json.JsonSlurper
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class UserMeSpecification extends TestSpecification {

    def jsonSlurper = new JsonSlurper()

    def "User Me endpoint returns principle user data"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password","elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange (getBaseUrl() + "/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData.name == "ITAG_USER"
    }

    def "User Me endpoint not accessible without valid token"() {

        when:
        def response = restTemplate.exchange("/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }
}
