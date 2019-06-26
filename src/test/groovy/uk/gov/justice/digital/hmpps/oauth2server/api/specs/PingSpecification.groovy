package uk.gov.justice.digital.hmpps.oauth2server.api.specs

import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.util.MimeType

class PingSpecification extends TestSpecification {

    def "Ping page returns pong"() {

        when:
        def response = restTemplate.exchange("/ping", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        response.headers.getContentType() == MimeType.valueOf("text/plain;charset=UTF-8")
        response.body == 'pong'
    }
}
