package uk.gov.justice.digital.hmpps.oauth2server.api.specs


import groovy.json.JsonSlurper
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException

class AuthUserRoleSpecification extends TestSpecification {
    def jsonSlurper = new JsonSlurper()

    def "Auth User Roles add role endpoint adds a role to a user"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER_ADM", "password123456", "elite2apiclient", "clientsecret")

        when:
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        def addRoleResponse = oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/AUTH_RO_USER/roles/licence_vary", HttpMethod.PUT, null, String.class)

        then:
        addRoleResponse.statusCode == HttpStatus.NO_CONTENT

        and:
        getRolesForUser(oauthRestTemplate, 'AUTH_RO_USER') == ['GLOBAL_SEARCH', 'LICENCE_RO', 'LICENCE_VARY']
    }

    def "Auth User Roles remove role endpoint removes a role from a user"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER_ADM", "password123456", "elite2apiclient", "clientsecret")

        when:
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        def addRoleResponse = oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/AUTH_RO_USER_TEST/roles/licence_ro", HttpMethod.DELETE, null, String.class)

        then:
        addRoleResponse.statusCode == HttpStatus.NO_CONTENT

        and:
        getRolesForUser(oauthRestTemplate, 'AUTH_RO_USER_TEST') == ['GLOBAL_SEARCH']
    }

    def "Auth User Roles endpoint returns user roles"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/authuser/auth_ro_vary_user/roles", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        assert userData.collect { it.roleCode }.sort() == ['GLOBAL_SEARCH', 'LICENCE_RO', 'LICENCE_VARY']
    }

    def "Auth User Roles endpoint not accessible without valid token"() {

        when:
        def response = restTemplate.exchange("/api/authuser/auth_ro_vary_user/roles", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "Auth User Roles add role endpoint not accessible without valid token"() {
        when:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        then:
        try {
            oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/auth_ro_vary_user/roles/licence_ro", HttpMethod.PUT, null, String.class)
            assert false // should not get here
        } catch (OAuth2AccessDeniedException exception) {
            exception.message == "error='access_denied', error_description='Access is denied'"
        }
    }

    def "Auth User Roles remove role endpoint not accessible without valid token"() {
        when:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        then:
        try {
            oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/auth_ro_vary_user/roles/licence_ro", HttpMethod.DELETE, null, String.class)
            assert false // should not get here
        } catch (OAuth2AccessDeniedException exception) {
            exception.message == "error='access_denied', error_description='Access is denied'"
        }
    }

    private def getRolesForUser(def template, def user) {
        def response = template.exchange("${getBaseUrl()}/api/authuser/${user}/roles", HttpMethod.GET, null, String.class)
        def userData = jsonSlurper.parseText(response.body)
        userData.collect { it.roleCode }.sort()
    }
}
