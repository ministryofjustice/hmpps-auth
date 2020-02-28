package uk.gov.justice.digital.hmpps.oauth2server.api.specs

import groovy.json.JsonSlurper
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.springframework.util.MimeType

@TestPropertySource(properties = "jwt.jwk.key.id=some-key-id")
class JwkSetSpecification extends TestSpecification {

    def jsonSlurper = new JsonSlurper()

    def "Jwk set page returns JwkSet"() {

        when:
        def response = restTemplate.exchange("/.well-known/jwks.json", HttpMethod.GET, null, String.class)
        def details = jsonSlurper.parseText(response.body)

        then:
        response.statusCode == HttpStatus.OK
        response.headers.getContentType() == MimeType.valueOf("application/json")
        details.keys[0].kid == "some-key-id"
    }
}
