package uk.gov.justice.digital.hmpps.oauth2server.api.specs

import groovy.json.JsonSlurper
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.util.MimeType

class AuthorizationMetadataSpecification extends TestSpecification {

    def jsonSlurper = new JsonSlurper()

    def "Metadata page responds and can be parsed"() {

        when:
        def response = restTemplate.exchange("/issuer/.well-known/openid-configuration", HttpMethod.GET, null, String.class)
        def details = jsonSlurper.parseText(response.body)

        then:
        response.statusCode == HttpStatus.OK
        response.headers.getContentType() == MimeType.valueOf("application/json")
        details.issuer.contains("http://localhost")
    }
}
