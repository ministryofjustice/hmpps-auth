package uk.gov.justice.digital.hmpps.oauth2server.api.specs

import groovy.json.JsonSlurper
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class PublicKeySpecification extends TestSpecification {

    def jsonSlurper = new JsonSlurper()

    def 'Public key values are correct'() {

        when:
        def response = restTemplate.exchange('/jwt-public-key', HttpMethod.GET, null, String.class)

        then:
        response.statusCode == HttpStatus.OK
        def details = jsonSlurper.parseText(response.body)

        details.encoded == 'LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQ0FROEFNSUlCQ2dLQ0FRRUFzT1BBdHNRQURkYlJ1L0VINkxQNQpCTTEvbUY0MFZEQm4xMmhKU1hQUGQ1V1lLMEhMWTIwVk03QXh4UjltbllDRjZTbzFXdDdmR05xVXgvV3llbUJwCklKTnJzLzdEendnM3V3aVF1Tmg0ektSK0VHeFdiTHdpM3l3N2xYUFV6eFV5QzV4dDg4ZS83dk8rbHoxb0NuaXoKamg0bXhOQW1zNlpZRjdxZm5oSkU5V3ZXUHdMTGtvamtadTFKZHVzTGFWb3dON0dUR05wTUU4ZHplSmthbTBncAo0b3hIUUdoTU44N0s2anFYM2NFd082RHZoZW1nOHdoczk2bnpRbDhuMkxGdkFLMnVwOVBycjlHaTJMRmdUdDdLCnFYQTA2a0M0S2d3MklSMWVGZ3pjQmxUT0V3bXpqcmU2NUhvTmFKQnI5dU5aelY1c0lMUE1jenpoUWovZk1oejMKL1FJREFRQUIKLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0tCg=='
        details.formatted == '''-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsOPAtsQADdbRu/EH6LP5
BM1/mF40VDBn12hJSXPPd5WYK0HLY20VM7AxxR9mnYCF6So1Wt7fGNqUx/WyemBp
IJNrs/7Dzwg3uwiQuNh4zKR+EGxWbLwi3yw7lXPUzxUyC5xt88e/7vO+lz1oCniz
jh4mxNAms6ZYF7qfnhJE9WvWPwLLkojkZu1JdusLaVowN7GTGNpME8dzeJkam0gp
4oxHQGhMN87K6jqX3cEwO6Dvhemg8whs96nzQl8n2LFvAK2up9Prr9Gi2LFgTt7K
qXA06kC4Kgw2IR1eFgzcBlTOEwmzjre65HoNaJBr9uNZzV5sILPMczzhQj/fMhz3
/QIDAQAB
-----END PUBLIC KEY-----
'''
    }
}
