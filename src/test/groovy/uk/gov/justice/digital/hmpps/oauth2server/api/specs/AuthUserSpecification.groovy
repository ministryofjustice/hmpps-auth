package uk.gov.justice.digital.hmpps.oauth2server.api.specs

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.http.*
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException
import org.springframework.web.client.HttpClientErrorException.NotFound

class AuthUserSpecification extends TestSpecification {
    class NewUser {
        String email
        String firstName
        String lastName
        String groupCode
    }

    def jsonSlurper = new JsonSlurper()

    def 'Create User endpoint succeeds to create user data'() {
        def username = RandomStringUtils.randomAlphanumeric(10)
        def user = [email: 'bob@bobdigital.justice.gov.uk', firstName: 'Bob', lastName: 'Smith'] as NewUser

        given:
        def oauthRestTemplate = getOauthPasswordGrant('ITAG_USER_ADM', 'password123456', 'elite2apiclient', 'clientsecret')

        when:
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        HttpEntity<String> entity = new HttpEntity<String>(new JsonBuilder(user).toPrettyString(), headers)
        def createUserResponse = oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/${username}", HttpMethod.PUT, entity, String.class)

        then:
        createUserResponse.statusCode == HttpStatus.OK

        def response = oauthRestTemplate.exchange("${getBaseUrl()}/api/user/${username}", HttpMethod.GET, null, String.class)
        def userData = jsonSlurper.parseText(response.body as String)

        userData == ['username': username.toUpperCase(), 'active': true, 'name': 'Bob Smith', 'authSource': 'auth']
    }

    def 'Create User endpoint succeeds to create user data with group and roles'() {
        def username = RandomStringUtils.randomAlphanumeric(10)
        def user = [email: 'bob@bobdigital.justice.gov.uk', firstName: 'Bob', lastName: 'Smith', groupCode: 'SITE_1_GROUP_1'] as NewUser

        given:
        def oauthRestTemplate = getOauthPasswordGrant('AUTH_GROUP_MANAGER', 'password123456', 'elite2apiclient', 'clientsecret')

        when:
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        HttpEntity<String> entity = new HttpEntity<String>(new JsonBuilder(user).toPrettyString(), headers)
        def createUserResponse = oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/${username}", HttpMethod.PUT, entity, String.class)

        then:
        createUserResponse.statusCode == HttpStatus.OK

        def response = oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/${username}/groups", HttpMethod.GET, null, String.class)
        def groupData = jsonSlurper.parseText(response.body as String)

        groupData == [['groupCode': 'SITE_1_GROUP_1', 'groupName': 'Site 1 - Group 1']]

        def response2 = oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/${username}/roles", HttpMethod.GET, null, String.class)
        def roleData = jsonSlurper.parseText(response2.body as String)

        roleData.collect({ it.roleCode }) == ['GLOBAL_SEARCH', 'LICENCE_RO']
    }

    def 'Create User endpoint fails if no privilege'() {
        def username = RandomStringUtils.randomAlphanumeric(10)
        def user = [email: 'bob@bobdigital.justice.gov.uk', firstName: 'Bob', lastName: 'Smith'] as NewUser

        given:
        def oauthRestTemplate = getOauthPasswordGrant('ITAG_USER', 'password', 'elite2apiclient', 'clientsecret')

        when:
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        HttpEntity<String> entity = new HttpEntity<String>(new JsonBuilder(user).toPrettyString(), headers)

        then:
        try {
            oauthRestTemplate.exchange("${getBaseUrl()}/api/authuser/${username}", HttpMethod.PUT, entity, String.class)
            assert false // should not get here
        } catch (OAuth2AccessDeniedException exception) {
            exception.message == "error='access_denied', error_description='Access is denied'"
        }
    }

    def 'Auth User endpoint returns user data'() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant('ITAG_USER', 'password', 'elite2apiclient', 'clientsecret')

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + '/api/authuser/AUTH_USER', HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData == ['username': 'AUTH_USER', 'email': 'auth_user@digital.justice.gov.uk', 'enabled': true, 'locked': false, 'verified': true, 'firstName': 'Auth', 'lastName': 'Only']
    }

    def 'Auth User endpoint returns no data for nomis user'() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant('ITAG_USER', 'password', 'elite2apiclient', 'clientsecret')

        when:
        String responseBody
        try {
            oauthRestTemplate.exchange(getBaseUrl() + '/api/authuser/ITAG_USER', HttpMethod.GET, null, String.class)
            assert false // should not get here
            responseBody = 'none'
        } catch (NotFound e) {
            responseBody = e.getResponseBodyAsString()
        }

        then:
        def userData = jsonSlurper.parseText(responseBody)

        userData == ['error': 'Not Found', 'error_description': 'Account for username ITAG_USER not found', 'field': 'username']
    }

    def 'Auth User email endpoint returns user data'() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant('ITAG_USER', 'password', 'elite2apiclient', 'clientsecret')

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + '/api/authuser?email=auth_test2@digital.justice.gov.uk', HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userDataList = jsonSlurper.parseText(response.body)

        userDataList == [
                ['username': 'AUTH_ADM', 'email': 'auth_test2@digital.justice.gov.uk', 'enabled': true, 'locked': false, 'verified': true, 'firstName': 'Auth', 'lastName': 'Adm'],
                ['username': 'AUTH_EXPIRED', 'email': 'auth_test2@digital.justice.gov.uk', 'enabled': true, 'locked': false, 'verified': true, 'firstName': 'Auth', 'lastName': 'Expired'],
        ]
    }

    def 'Auth User search endpoint returns user data'() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant('ITAG_USER', 'password', 'elite2apiclient', 'clientsecret')

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + '/api/authuser/search?name=test2', HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userDataList = jsonSlurper.parseText(response.body)

        userDataList.collect({ it.username }) == ['AUTH_ADM', 'AUTH_EXPIRED']
    }

    def 'Auth User email endpoint returns no data if not found'() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant('ITAG_USER', 'password', 'elite2apiclient', 'clientsecret')

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + '/api/authuser?email=nobody@nowhere', HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.NO_CONTENT
    }

    def 'Auth User Enable endpoint enables user'() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant('ITAG_USER_ADM', 'password123456', 'elite2apiclient', 'clientsecret')

        when:
        def enableResponse = oauthRestTemplate.exchange(getBaseUrl() + '/api/authuser/AUTH_STATUS/enable', HttpMethod.PUT, null, String.class)

        then:
        enableResponse.statusCode == HttpStatus.NO_CONTENT
        def response = oauthRestTemplate.exchange(getBaseUrl() + '/api/authuser/AUTH_STATUS', HttpMethod.GET, null, String.class)
        def userData = jsonSlurper.parseText(response.body)

        userData == ['username': 'AUTH_STATUS', 'email': null, 'enabled': true, 'locked': false, 'verified': true, 'firstName': 'Auth', 'lastName': 'Status']
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

        userData == ['username': 'AUTH_STATUS', 'email': null, 'enabled': false, 'locked': false, 'verified': true, 'firstName': 'Auth', 'lastName': 'Status']
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
