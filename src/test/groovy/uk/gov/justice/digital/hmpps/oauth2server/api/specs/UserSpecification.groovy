package uk.gov.justice.digital.hmpps.oauth2server.api.specs

import groovy.json.JsonSlurper
import org.junit.Rule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.CommunityApiMockServer

@SuppressWarnings("GrDeprecatedAPIUsage")
class UserSpecification extends TestSpecification {
    @Rule
    CommunityApiMockServer communityApi = new CommunityApiMockServer()

    @Autowired
    OAuth2RestTemplate deliusApiRestTemplate

    def jsonSlurper = new JsonSlurper()

    void setup() {
        // need to override port as random port only assigned on server startup
        deliusApiRestTemplate.getResource().accessTokenUri = "http://localhost:${randomServerPort}/auth/oauth/token"
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
        }.sort() == ['APPROVE_CATEGORISATION', 'CATEGORISATION_SECURITY', 'CREATE_CATEGORISATION', 'GLOBAL_SEARCH', 'KW_MIGRATION', 'MAINTAIN_ACCESS_ROLES_ADMIN', 'OMIC_ADMIN', 'PRISON']
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

    def "User Roles endpoint returns principal user data for delius user"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("delius", "password123456", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/user/me/roles", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def userData = jsonSlurper.parseText(response.body)

        assert userData.collect {
            it.roleCode
        }.sort() == ['GLOBAL_SEARCH', 'LICENCE_RO', 'PROBATION']
    }
}
