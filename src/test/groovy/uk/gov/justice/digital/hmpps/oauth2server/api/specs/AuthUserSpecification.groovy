package uk.gov.justice.digital.hmpps.oauth2server.api.specs


import groovy.json.JsonSlurper
import org.junit.Rule
import org.springframework.http.*
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException
import org.springframework.web.client.HttpClientErrorException
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.CommunityApiMockServer

class AuthUserSpecification extends TestSpecification {
    @Rule
    CommunityApiMockServer communityApi = new CommunityApiMockServer()

    class NewUser {
        String email
        String firstName
        String lastName
        String groupCode
    }

    def jsonSlurper = new JsonSlurper()

    def 'Auth User Enable endpoint enables user'() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant('ITAG_USER_ADM', 'password123456', 'elite2apiclient', 'clientsecret')

        when:
        def enableResponse = oauthRestTemplate.exchange(getBaseUrl() + '/api/authuser/AUTH_STATUS/enable', HttpMethod.PUT, null, String.class)

        then:
        enableResponse.statusCode == HttpStatus.NO_CONTENT
        def response = oauthRestTemplate.exchange(getBaseUrl() + '/api/authuser/AUTH_STATUS', HttpMethod.GET, null, String.class)
        def userData = jsonSlurper.parseText(response.body)
        userData.remove 'lastLoggedIn'
        userData == ['username': 'AUTH_STATUS', 'email': null, 'enabled': true, 'locked': false, 'verified': true, 'firstName': 'Auth', 'lastName': 'Status', 'userId': 'fc494152-f9ad-48a0-a87c-9adc8bd75255']
    }

    def 'Group manager Enable endpoint enables user'() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER_ADM", "password123456", "elite2apiclient", "clientsecret")
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/AUTH_STATUS/groups/site_1_group_2", HttpMethod.PUT, null, String.class)
        def oauthRestTemplate1 = getOauthPasswordGrant('AUTH_GROUP_MANAGER', 'password123456', 'elite2apiclient', 'clientsecret')

        when:
        def enableResponse = oauthRestTemplate1.exchange(getBaseUrl() + '/api/authuser/AUTH_STATUS/enable', HttpMethod.PUT, null, String.class)

        then:
        enableResponse.statusCode == HttpStatus.NO_CONTENT
        def response = oauthRestTemplate.exchange(getBaseUrl() + '/api/authuser/AUTH_STATUS', HttpMethod.GET, null, String.class)
        def userData = jsonSlurper.parseText(response.body)
        userData.remove 'lastLoggedIn'
        userData == ['username': 'AUTH_STATUS', 'email': null, 'enabled': true, 'locked': false, 'verified': true, 'firstName': 'Auth', 'lastName': 'Status', 'userId': 'fc494152-f9ad-48a0-a87c-9adc8bd75255']
    }

    def 'Group manager Enable endpoint fails user not in group manager group conflict'() {

        when:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER_ADM", "password123456", "elite2apiclient", "clientsecret")
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/AUTH_STATUS/groups/site_1_group_2", HttpMethod.DELETE, null, String.class)
        def oauthRestTemplate1 = getOauthPasswordGrant('AUTH_GROUP_MANAGER', 'password123456', 'elite2apiclient', 'clientsecret')

        then:
        try {
            oauthRestTemplate1.exchange(getBaseUrl() + '/api/authuser/AUTH_STATUS/enable', HttpMethod.PUT, null, String.class)
            assert false // should not get here
        } catch (HttpClientErrorException exception) {
            exception.statusCode == HttpStatus.CONFLICT
        }
    }

    def 'Auth User Enable endpoint fails is not an admin user'() {

        when:
        def oauthRestTemplate = getOauthPasswordGrant('ITAG_USER', 'password123456', 'elite2apiclient', 'clientsecret')

        then:
        try {
            oauthRestTemplate.exchange(getBaseUrl() + '/api/authuser/AUTH_STATUS/enable', HttpMethod.PUT, null, String.class)
            assert false // should not get here
        } catch (OAuth2AccessDeniedException exception) {
            exception.message == "error='access_denied', error_description='Access is denied'"
        }
    }

    def 'Auth User Disable endpoint disables user'() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant('ITAG_USER_ADM', 'password123456', 'elite2apiclient', 'clientsecret')

        when:
        def disableResponse = oauthRestTemplate.exchange(getBaseUrl() + '/api/authuser/AUTH_STATUS/disable', HttpMethod.PUT, null, String.class)

        then:
        disableResponse.statusCode == HttpStatus.NO_CONTENT
        def response = oauthRestTemplate.exchange(getBaseUrl() + '/api/authuser/AUTH_STATUS', HttpMethod.GET, null, String.class)
        def userData = jsonSlurper.parseText(response.body)
        userData.remove 'lastLoggedIn'
        userData == ['username': 'AUTH_STATUS', 'email': null, 'enabled': false, 'locked': false, 'verified': true, 'firstName': 'Auth', 'lastName': 'Status', 'userId': 'fc494152-f9ad-48a0-a87c-9adc8bd75255']
    }

    def 'Group manager Disable endpoint enables user'() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER_ADM", "password123456", "elite2apiclient", "clientsecret")
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/AUTH_STATUS/groups/site_1_group_2", HttpMethod.PUT, null, String.class)
        oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/AUTH_STATUS/enable", HttpMethod.PUT, null, String.class)
        def oauthRestTemplate1 = getOauthPasswordGrant('AUTH_GROUP_MANAGER', 'password123456', 'elite2apiclient', 'clientsecret')

        when:
        def disableResponse = oauthRestTemplate1.exchange("${getBaseUrl()}/api/authuser/AUTH_STATUS/disable", HttpMethod.PUT, null, String.class)

        then:
        disableResponse.statusCode == HttpStatus.NO_CONTENT
        def response = oauthRestTemplate.exchange(getBaseUrl() + '/api/authuser/AUTH_STATUS', HttpMethod.GET, null, String.class)
        def userData = jsonSlurper.parseText(response.body)
        userData.remove 'lastLoggedIn'
        userData == ['username': 'AUTH_STATUS', 'email': null, 'enabled': false, 'locked': false, 'verified': true, 'firstName': 'Auth', 'lastName': 'Status', 'userId': 'fc494152-f9ad-48a0-a87c-9adc8bd75255']
    }


    def 'Group manager Disable endpoint fails user not in group manager group conflict'() {

        when:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER_ADM", "password123456", "elite2apiclient", "clientsecret")
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/AUTH_STATUS/groups/site_1_group_2", HttpMethod.DELETE, null, String.class)
        def oauthRestTemplate1 = getOauthPasswordGrant('AUTH_GROUP_MANAGER', 'password123456', 'elite2apiclient', 'clientsecret')

        then:
        try {
            oauthRestTemplate1.exchange(getBaseUrl() + '/api/authuser/AUTH_STATUS/disable', HttpMethod.PUT, null, String.class)
            assert false // should not get here
        } catch (HttpClientErrorException exception) {
            exception.statusCode == HttpStatus.CONFLICT
        }
    }

    def 'Auth User Disable endpoint fails is not an admin user'() {

        when:
        def oauthRestTemplate = getOauthPasswordGrant('ITAG_USER', 'password123456', 'elite2apiclient', 'clientsecret')

        then:
        try {
            oauthRestTemplate.exchange(getBaseUrl() + '/api/authuser/AUTH_STATUS/disable', HttpMethod.PUT, null, String.class)
            assert false // should not get here
        } catch (OAuth2AccessDeniedException exception) {
            exception.message == "error='access_denied', error_description='Access is denied'"
        }
    }

    def 'Amend User endpoint succeeds to alter user email'() {
        given:
        def oauthRestTemplate = getOauthPasswordGrant('ITAG_USER_ADM', 'password123456', 'elite2apiclient', 'clientsecret')

        when:
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        HttpEntity<String> entity = new HttpEntity<String>('{"email": "bobby.b@digital.justice.gov.uk" }', headers)
        def createUserResponse = oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/AUTH_NEW_USER", HttpMethod.POST, entity, String.class)

        then:
        createUserResponse.statusCode == HttpStatus.OK

        def response = oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/AUTH_NEW_USER", HttpMethod.GET, null, String.class)
        def userData = jsonSlurper.parseText(response.body as String)

        userData.email == 'bobby.b@digital.justice.gov.uk'
    }

    def 'Amend User endpoint fails if no privilege'() {
        given:
        def oauthRestTemplate = getOauthPasswordGrant('ITAG_USER', 'password', 'elite2apiclient', 'clientsecret')

        when:
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        then:
        try {
            HttpEntity<String> entity = new HttpEntity<String>('{"email": "bobby.b@digital.justice.gov.uk" }', headers)
            oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/AUTH_NEW_USER", HttpMethod.POST, entity, String.class)
            assert false // should not get here
        } catch (OAuth2AccessDeniedException exception) {
            exception.message == "error='access_denied', error_description='Access is denied'"
        }
    }

    def "Auth User Assignable Groups endpoint for normal user returns their own groups"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("AUTH_RO_VARY_USER", "password123456", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/authuser/me/assignable-groups", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        assert userData.collect { it.groupCode } == ['SITE_1_GROUP_1', 'SITE_1_GROUP_2']
    }

    def "Auth User Assignable Groups endpoint for super user returns all groups"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER_ADM", "password123456", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/authuser/me/assignable-groups", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        assert userData.collect { it.groupCode }.findAll({
            it.startsWith("SITE")
        }) == ['SITE_1_GROUP_1', 'SITE_1_GROUP_2', 'SITE_2_GROUP_1', 'SITE_3_GROUP_1']
    }

}
