package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import groovy.json.JsonSlurper
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class InfoSpecification extends TestSpecification {

    def jsonSlurper = new JsonSlurper()

    def "Info page reports ok"() {

        when:
        def response = restTemplate.exchange("/info", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def details = jsonSlurper.parseText(response.body)

        details == [:]
    }
}
