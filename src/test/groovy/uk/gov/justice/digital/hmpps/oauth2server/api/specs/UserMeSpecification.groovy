package uk.gov.justice.digital.hmpps.oauth2server.api.specs

import groovy.json.JsonSlurper
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class UserMeSpecification extends TestSpecification {

    def jsonSlurper = new JsonSlurper()

    def "User Me endpoint returns principal user data"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password","elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange (getBaseUrl() + "/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData.name == "ITAG_USER"
    }

    def "User Me endpoint returns principal user data for auth user"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("AUTH_ONLY_USER", "password123456", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData.name == "AUTH_ONLY_USER"
    }

    def "User Me endpoint not accessible without valid token"() {

        when:
        def response = restTemplate.exchange("/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "User Roles endpoint returns principal user data"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/me/roles", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        assert userData.collect {
            it.roleCode
        }.sort() == ['APPROVE_CATEGORISATION', 'CREATE_CATEGORISATION', 'GLOBAL_SEARCH', 'MAINTAIN_ACCESS_ROLES_ADMIN', 'OMIC_ADMIN']
    }

    def "User Roles endpoint returns principal user data for auth user"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("AUTH_ONLY_ADM", "password123456", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/me/roles", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        assert userData.collect { it.roleCode }.sort() == ['MAINTAIN_ACCESS_ROLES', 'OAUTH_ADMIN']
    }
}
