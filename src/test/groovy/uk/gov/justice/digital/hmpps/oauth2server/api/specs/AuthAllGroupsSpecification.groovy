package uk.gov.justice.digital.hmpps.oauth2server.api.specs


import groovy.json.JsonSlurper
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class AuthAllGroupsSpecification extends TestSpecification {
    def jsonSlurper = new JsonSlurper()

    def "Auth Groups endpoint returns all possible auth groups"() {

        given:
        def oauthRestTemplate = getOauthPasswordGrant("AUTH_ADM", "password123456", "elite2apiclient", "clientsecret")

        when:
        def response = oauthRestTemplate.exchange(getBaseUrl() + "/api/authgroups", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def allGroups = jsonSlurper.parseText(response.body)

        assert allGroups.size() > 2
        assert allGroups.find { it.groupCode == 'SITE_1_GROUP_1' }.groupName == 'Site 1 - Group 1'
    }

    def "Auth Groups endpoint not accessible without valid token"() {

        when:
        def response = restTemplate.exchange("/api/authgroups", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }
}
