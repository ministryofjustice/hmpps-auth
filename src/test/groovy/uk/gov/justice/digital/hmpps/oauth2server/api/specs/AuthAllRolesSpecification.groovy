package uk.gov.justice.digital.hmpps.oauth2server.api.specs


import groovy.json.JsonSlurper
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class AuthAllRolesSpecification extends TestSpecification {
    def jsonSlurper = new JsonSlurper()

    def "Auth Roles endpoint returns all possible auth roles"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("AUTH_ADM", "password123456", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/authroles", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def allRoles = jsonSlurper.parseText(response.body)

        assert allRoles.size() > 2
        assert allRoles.find { it.roleCode == 'GLOBAL_SEARCH' }.roleName == 'Global Search'
    }

    def "Auth Roles endpoint not accessible without valid token"() {

        when:
        def response = restTemplate.exchange("/api/authroles", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }
}
