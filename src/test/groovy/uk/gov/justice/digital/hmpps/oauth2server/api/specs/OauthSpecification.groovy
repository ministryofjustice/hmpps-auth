package uk.gov.justice.digital.hmpps.oauth2server.api.specs

import com.auth0.jwt.JWTDecoder
import groovy.json.JsonSlurper
import org.junit.Rule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.CommunityApiMockServer

@SuppressWarnings("GrDeprecatedAPIUsage")
class OauthSpecification extends TestSpecification {
    @Rule
    CommunityApiMockServer communityApi = new CommunityApiMockServer()

    @Autowired
    OAuth2RestTemplate deliusApiRestTemplate

    def jsonSlurper = new JsonSlurper()

    void setup() {
        // need to override port as random port only assigned on server startup
        deliusApiRestTemplate.getResource().accessTokenUri = "http://localhost:${randomServerPort}/auth/oauth/token"
    }

    def "Client Credentials Login"() {

        given:
        def oauthRestTemplate = getOauthClientGrant("deliusnewtech", "clientsecret")

        when:
        def token = oauthRestTemplate.getAccessToken()

        then:
        token.value != null

        and:
        token.expiresIn <= 3600

        and: 'refresh token deos not exist'
        token.refreshToken == null
    }

    def "Client Credentials Login With username identifier"() {

        given:
        def oauthRestTemplate = getOauthClientGrant("omicadmin", "clientsecret", "username=CA_USER")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData.name == "Licence Case Admin"
    }

    def "Client Credentials Login access token"() {

        given:
        def oauthRestTemplate = getOauthClientGrant("omicadmin", "clientsecret", "username=CA_USER")

        when:
        def token = oauthRestTemplate.getAccessToken()

        then:
        token.value != null

        and: 'expiry is in 1 hour'
        token.expiresIn >= 3590
        token.expiresIn <= 3600

        and: 'refresh token does not exist'
        token.refreshToken == null

        and: 'authentication source is nomis'
        token.additionalInformation.auth_source == 'nomis'
    }

    def "Client Credentials Login access token for auth user"() {

        given:
        def oauthRestTemplate = getOauthClientGrant("omicadmin", "clientsecret", "username=AUTH_USER")

        when:
        def token = oauthRestTemplate.getAccessToken()

        then:
        token.value != null

        and: 'expiry is in 1 hour'
        token.expiresIn >= 3590
        token.expiresIn <= 3600

        and: 'refresh token does not exist'
        token.refreshToken == null

        and: 'authentication source is auth'
        token.additionalInformation.auth_source == 'auth'
    }

    def "Client Credentials Login access token for non auth or nomis user"() {

        given:
        def oauthRestTemplate = getOauthClientGrant("community-api-client", "clientsecret", "username=NPSUser")

        when:
        def token = oauthRestTemplate.getAccessToken()

        then:
        token.value != null

        and: 'expiry is in 1 hour'
        token.expiresIn >= 3590
        token.expiresIn <= 3600

        and: 'refresh token does not exist'
        token.refreshToken == null

        and: 'authentication source is auth'
        token.additionalInformation.auth_source == 'none'
    }

    def "Client Credentials Login failure for non auth or nomis user without scope priv"() {

        given:
        def oauthRestTemplate = getOauthClientGrant("omicadmin", "clientsecret", "username=NPSUser")

        when:
        def token = oauthRestTemplate.getAccessToken()

        then:
        OAuth2AccessDeniedException ex = thrown()
    }


    def "Client Credentials Login With username identifier for auth user"() {

        given:
        def oauthRestTemplate = getOauthClientGrant("omicadmin", "clientsecret", "username=AUTH_USER")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData.name == "Auth Only"
    }

    def "Password Credentials Login"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        when:
        def token = oauthRestTemplate.getAccessToken()

        then:
        token.value != null

        and: 'expiry is in 8 hours'
        token.expiresIn >= 28790

        and: 'refresh token exists'
        token.refreshToken.value != null

        and: 'authentication source is nomis'
        token.additionalInformation.auth_source == 'nomis'

    }

    def "Password Credentials Login for auth user"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("AUTH_USER", "password123456", "elite2apiclient", "clientsecret")

        when:
        def token = oauthRestTemplate.getAccessToken()

        then:
        token.value != null

        and: 'expiry is in 8 hours'
        token.expiresIn >= 28790

        and: 'refresh token exists'
        token.refreshToken.value != null

        and: 'authentication source is auth'
        token.additionalInformation.auth_source == 'auth'
    }

    def "Password Credentials Login for delius user"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("delius", "password", "elite2apiclient", "clientsecret")

        when:
        def token = oauthRestTemplate.getAccessToken()

        then:
        token.value != null

        and: 'expiry is in 8 hours'
        token.expiresIn >= 28790

        and: 'refresh token exists'
        token.refreshToken.value != null

        and: 'authentication source is auth'
        token.additionalInformation.auth_source == 'delius'
    }


    def "Refresh token can be obtained"() {

        given: 'I create an access token'
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")
        def accessToken = oauthRestTemplate.getAccessToken()

        when: 'I request a refresh token'
        def newAccessToken = refresh(oauthRestTemplate)

        then: 'refresh token is returned'
        newAccessToken.refreshToken.value != null

        and: 'access tokens are different'
        accessToken.getRefreshToken().value != newAccessToken.getRefreshToken().value
        accessToken.value != newAccessToken.value
    }

    def "Refresh token can be obtained for auth user"() {

        given: 'I create an access token'
        def oauthRestTemplate = getOauthPasswordGrant("AUTH_USER", "password123456", "elite2apiclient", "clientsecret")
        def accessToken = oauthRestTemplate.getAccessToken()

        when: 'I request a refresh token'
        def newAccessToken = refresh(oauthRestTemplate)

        then: 'refresh token is returned'
        newAccessToken.refreshToken.value != null

        and: 'access tokens are different'
        accessToken.getRefreshToken().value != newAccessToken.getRefreshToken().value
        accessToken.value != newAccessToken.value
    }

    def "Password Credentials Login with Bad password credentials"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password2", "elite2apiclient", "clientsecret")

        when:
        def token = oauthRestTemplate.getAccessToken()

        then:
        OAuth2AccessDeniedException ex = thrown()
    }

    def "Password Credentials Login with Bad client credentials"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecretBAD")

        when:
        def token = oauthRestTemplate.getAccessToken()

        then:
        OAuth2AccessDeniedException ex = thrown()
    }

    def "Password Credentials Login with Wrong client Id"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclientBAD", "clientsecret")

        when:
        def token = oauthRestTemplate.getAccessToken()

        then:
        OAuth2AccessDeniedException ex = thrown()
    }

    def "Password Credentials Login with Expired Login"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("EXPIRED_USER", "password123456", "elite2apiclient", "clientsecret")

        when:
        oauthRestTemplate.getAccessToken()

        then:
        OAuth2AccessDeniedException ex = thrown()
    }

    def "Password Credentials Login with Locked Login"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("LOCKED_USER", "password123456", "elite2apiclient", "clientsecret")

        when:
        oauthRestTemplate.getAccessToken()

        then:
        OAuth2AccessDeniedException ex = thrown()
    }

    def "Password Credentials Login with Locked Login for Delius User"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("dellocked", "password123456", "elite2apiclient", "clientsecret")

        when:
        oauthRestTemplate.getAccessToken()

        then:
        OAuth2AccessDeniedException ex = thrown()
    }

    def "Password Credentials Login can get api data"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData.name == "Itag User"

    }

    def "Client Credentials Login can get api data"() {

        given:
        def oauthRestTemplate = getOauthClientGrant("omicadmin", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/me", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        userData.username == "omicadmin"

    }

    def "Kid header is returned"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("ITAG_USER", "password", "elite2apiclient", "clientsecret")

        when:
        def accessToken = oauthRestTemplate.getAccessToken()

        then:
        new JWTDecoder(accessToken.value).getHeaderClaim("kid").asString() == "dps-client-key"
    }
}
