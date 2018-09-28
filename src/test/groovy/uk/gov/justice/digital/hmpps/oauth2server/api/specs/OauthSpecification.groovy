package uk.gov.justice.digital.hmpps.oauth2server.api.specs

import groovy.json.JsonSlurper
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException

class OauthSpecification extends TestSpecification {

    def jsonSlurper = new JsonSlurper()

    def "Client Credentials Login"() {

        given:
        def oauthRestTemplate = getOauthClientGrant("deliusnewtech", "clientsecret")

        when:
        def token = oauthRestTemplate.getAccessToken()

        then:
        token.value != null

        and:
        token.expiresIn <= 3600
    }

    def "Client Credentials Login With Identifier"() {

        given:
        def oauthRestTemplate = getOauthClientGrant("yjaftrustedclient", "clientsecret", "user_id_type=YJAF&user_id=test@yjaf.gov.uk")

        when:
        def response = oauthRestTemplate.exchange (getBaseUrl() + "/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData.name == "ITAG_USER"
    }

    def "Client Credentials Login With username identifier"() {

        given:
        def oauthRestTemplate = getOauthClientGrant("omicadmin", "clientsecret", "username=CA_USER")

        when:
        def response = oauthRestTemplate.exchange (getBaseUrl() + "/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData.name == "CA_USER"
    }

    def "Password Credentials Login"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password","elite2apiclient", "clientsecret")

        when:
        def token = oauthRestTemplate.getAccessToken()

        then:
        token.value != null

        and:
        token.expiresIn >= 28790
    }

    def "Refresh token can be obtained"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password","elite2apiclient", "clientsecret")

        when:
        def token = refresh(oauthRestTemplate)

        then:
        token.value != null
    }

    def "Password Credentials Login with Bad password credentials"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password2","elite2apiclient", "clientsecret")

        when:
        def token = oauthRestTemplate.getAccessToken()

        then:
        OAuth2AccessDeniedException ex = thrown()
    }

    def "Password Credentials Login with Bad client credentials"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password","elite2apiclient", "clientsecretBAD")

        when:
        def token = oauthRestTemplate.getAccessToken()

        then:
        OAuth2AccessDeniedException ex = thrown()
    }

    def "Password Credentials Login with Wrong client Id"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password","elite2apiclientBAD", "clientsecret")

        when:
        def token = oauthRestTemplate.getAccessToken()

        then:
        OAuth2AccessDeniedException ex = thrown()
    }

    def "Password Credentials Login with Expired Login"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("EXPIRED_USER", "password123456","elite2apiclient", "clientsecret")

        when:
        oauthRestTemplate.getAccessToken()

        then:
        OAuth2AccessDeniedException ex = thrown()
    }

    def "Password Credentials Login with Locked Login"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("LOCKED_USER", "password123456","elite2apiclient", "clientsecret")

        when:
        oauthRestTemplate.getAccessToken()

        then:
        OAuth2AccessDeniedException ex = thrown()
    }

    def "Password Credentials Login can get api data"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password","elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange (getBaseUrl() + "/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData.name == "ITAG_USER"

    }

    def "Client Credentials Login can get api data"() {

        given:
        def oauthRestTemplate = getOauthClientGrant("omicadmin", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange (getBaseUrl() + "/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData.name == "omicadmin"

    }
}
