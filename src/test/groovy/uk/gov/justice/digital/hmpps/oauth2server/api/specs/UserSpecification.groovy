package uk.gov.justice.digital.hmpps.oauth2server.api.specs

import groovy.json.JsonSlurper
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class UserSpecification extends TestSpecification {

    def jsonSlurper = new JsonSlurper()

    def "User Me endpoint returns principal user data"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData == ["username": "ITAG_USER", "active": true, "name": "Itag User", "staffId": 1, "activeCaseLoadId": "MDI", "authSource": "nomis", "userId": "1"]
    }

    def "User Me endpoint returns principal user data for client credentials grant"() {

        given:
        def oauthRestTemplate = getOauthClientGrant("deliusnewtech", "clientsecret", "username=ITAG_USER")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData == ["username": "ITAG_USER", "active": true, "name": "Itag User", "staffId": 1, "activeCaseLoadId": "MDI", "authSource": "nomis", "userId": "1"]
    }

    def "User Me endpoint returns principal user data for auth user"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("AUTH_USER", "password123456", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData == ["username": "AUTH_USER", "active": true, "name": "Auth Only", "authSource": "auth", 'userId': '608955ae-52ed-44cc-884c-011597a77949']
    }

    def "User username endpoint returns user data"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/RO_USER", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData == ["username": "RO_USER", "active": true, "name": "Licence Responsible Officer", "authSource": "nomis", "staffId": 4, "activeCaseLoadId": "BEL", "userId": "4"]
    }

    def "User username endpoint returns user data for auth user"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/AUTH_USER", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData == ["username": "AUTH_USER", "active": true, "name": "Auth Only", "authSource": "auth", 'userId': '608955ae-52ed-44cc-884c-011597a77949']
    }

    def "User email endpoint returns user data for auth user"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/AUTH_USER/email", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData == ["username": "AUTH_USER", "email": "auth_user@digital.justice.gov.uk"]
    }

    def "User email endpoint returns no user data for unverified email address"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/DM_USER/email", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.NO_CONTENT
    }

    def "User email endpoint returns user data for nomis user"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/ITAG_USER/email", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData == ["username": "ITAG_USER", "email": "itag_user@digital.justice.gov.uk"]
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
        }.sort() == ['APPROVE_CATEGORISATION', 'CATEGORISATION_SECURITY', 'CREATE_CATEGORISATION', 'GLOBAL_SEARCH', 'MAINTAIN_ACCESS_ROLES_ADMIN', 'OMIC_ADMIN']
    }

    def "User Roles endpoint returns principal user data for auth user"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("AUTH_ADM", "password123456", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/me/roles", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        assert userData.collect {
            it.roleCode
        }.sort() == ['MAINTAIN_ACCESS_ROLES', 'MAINTAIN_OAUTH_USERS', 'OAUTH_ADMIN']
    }

    def "User Me endpoint not accessible without valid token"() {

        when:
        def response = restTemplate.exchange("/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "User Me Roles endpoint not accessible without valid token"() {

        when:
        def response = restTemplate.exchange("/api/user/me/roles", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "User username endpoint not accessible without valid token"() {

        when:
        def response = restTemplate.exchange("/api/user/bob", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "User email endpoint not accessible without valid token"() {

        when:
        def response = restTemplate.exchange("/api/user/bob/email", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }
}
