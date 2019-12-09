package uk.gov.justice.digital.hmpps.oauth2server.api.specs


import groovy.json.JsonSlurper
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class HealthSpecification extends TestSpecification {

    def jsonSlurper = new JsonSlurper()

    def "Health page reports ok"() {

        when:
        def response = restTemplate.exchange("/health", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def details = jsonSlurper.parseText(response.body)

        details.status == "UP"
        details.components.db.components.authDataSource.details == [database: 'H2', result: 1, validationQuery: 'SELECT 1']
    }

    def "Health ping reports ok"() {

        when:
        def response = restTemplate.exchange("/health/ping", HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def details = jsonSlurper.parseText(response.body)

        details.status == "UP"
    }
}
