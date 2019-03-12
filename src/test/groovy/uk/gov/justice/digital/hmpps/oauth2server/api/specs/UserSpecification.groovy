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

        userData == ["username": "ITAG_USER", "active": true, "name": "Itag User", "staffId": 1, "activeCaseLoadId": "MDI", "authSource": "nomis"]
    }

    def "User Me endpoint returns principal user data for client credentials grant"() {

        given:
        def oauthRestTemplate = getOauthClientGrant("deliusnewtech", "clientsecret", "username=ITAG_USER")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData == ["username": "ITAG_USER", "active": true, "name": "Itag User", "staffId": 1, "activeCaseLoadId": "MDI", "authSource": "nomis"]
    }

    def "User Me endpoint returns principal user data for auth user"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("AUTH_USER", "password123456", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData == ["username": "AUTH_USER", "active": true, "name": "Auth Only", "authSource": "auth"]
    }

    def "User username endpoint returns user data"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/RO_USER", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData == ["username": "RO_USER", "active": true, "name": "Licence Responsible Officer", "authSource": "nomis", "staffId": 4, "activeCaseLoadId": "BEL"]
    }

    def "User username endpoint returns user data for auth user"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/AUTH_USER", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData == ["username": "AUTH_USER", "active": true, "name": "Auth Only", "authSource": "auth"]
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
        def oauthRestTemplate = getOauthPasswordGrant("AUTH_ADM", "password123456", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/me/roles", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        assert userData.collect { it.roleCode }.sort() == ['MAINTAIN_ACCESS_ROLES', 'OAUTH_ADMIN']
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
}
