package uk.gov.justice.digital.hmpps.oauth2server.api.specs


import groovy.json.JsonSlurper
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException

class AuthUserGroupSpecification extends TestSpecification {
    def jsonSlurper = new JsonSlurper()

    def "Auth User Groups add group endpoint adds a group to a user"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER_ADM", "password123456", "elite2apiclient", "clientsecret")

        when:
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        def addGroupResponse = oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/AUTH_RO_USER/groups/site_1_group_2", HttpMethod.PUT, null, String.class)

        then:
        addGroupResponse.statusCode == HttpStatus.NO_CONTENT

        and:
        getGroupsForUser(oauthRestTemplate, 'AUTH_RO_USER') == ['SITE_1_GROUP_1', 'SITE_1_GROUP_2']
    }

    def "Auth User Groups remove group endpoint removes a group from a user"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER_ADM", "password123456", "elite2apiclient", "clientsecret")

        when:
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        def addGroupResponse = oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/AUTH_RO_USER_TEST/groups/SITE_1_GROUP_1", HttpMethod.DELETE, null, String.class)

        then:
        addGroupResponse.statusCode == HttpStatus.NO_CONTENT

        and:
        getGroupsForUser(oauthRestTemplate, 'AUTH_RO_USER_TEST') == ['SITE_2_GROUP_1']
    }

    def "Auth User Groups endpoint returns user groups"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/authuser/auth_ro_vary_user/groups", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        assert userData.collect { it.groupCode }.sort() == ['SITE_1_GROUP_1', 'SITE_1_GROUP_2']
    }

    def "Auth User Groups endpoint not accessible without valid token"() {

        when:
        def response = restTemplate.exchange("/api/authuser/auth_ro_vary_user/groups", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "Auth User Groups add group endpoint not accessible without valid token"() {
        when:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        then:
        try {
            oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/auth_ro_vary_user/groups/licence_ro", HttpMethod.PUT, null, String.class)
            assert false // should not get here
        } catch (OAuth2AccessDeniedException exception) {
            exception.message == "error='access_denied', error_description='Access is denied'"
        }
    }

    def "Auth User Groups remove group endpoint not accessible without valid token"() {
        when:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        then:
        try {
            oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/auth_ro_vary_user/groups/licence_ro", HttpMethod.DELETE, null, String.class)
            assert false // should not get here
        } catch (OAuth2AccessDeniedException exception) {
            exception.message == "error='access_denied', error_description='Access is denied'"
        }
    }

    private def getGroupsForUser(def template, def user) {
        def response = template.exchange("${getBaseUrl()}/api/authuser/${user}/groups", HttpMethod.GET, null, String.class)
        def userData = jsonSlurper.parseText(response.body)
        userData.collect { it.groupCode }.sort()
    }
}
