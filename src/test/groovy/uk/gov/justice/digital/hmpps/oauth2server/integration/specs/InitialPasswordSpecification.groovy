package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import geb.spock.GebReportingSpec
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.api.specs.AuthUserSpecification.NewUser
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.*

class InitialPasswordSpecification extends DeliusIntegrationSpec {

    def "A licences user can be created and new password saved"() {
        given: 'A licence RO user has been created'
        def username = RandomStringUtils.randomAlphabetic(10)
        def link = createLicenceUser(username)

        when: 'I click the link in the email'
        browser.go link

        then: 'I am shown the set password page'
        at SetPasswordPage

        when: "I set an initial password"
        setPasswordAs 'helloworld2', 'helloworld2'

        and: 'My credentials are accepted and I am shown the confirmation page'
        at InitialPasswordSuccessPage

        and: 'I can try to login using new password'
        signInLink.click()
        at LoginPage
        loginAs username, 'helloworld2'

        then: 'I am logged in with new password'
        at HomePage
    }

    def "A licences user can be created and password validated"() {
        given: 'A licence RO user has been created'
        def username = RandomStringUtils.randomAlphabetic(10)
        def link = createLicenceUser(username)

        when: 'I click the link in the email'
        browser.go(link)

        and: 'I am shown the set password page'
        at SetPasswordPage

        and: "I set an initial password in the blacklist"
        setPasswordAs 'password1', 'password1'

        at SetPasswordErrorPage
        assert errorText == 'Your password is commonly used and may not be secure'

        setPasswordAs 'helloworld2', 'helloworld2'

        then: 'My credentials are accepted and I am shown the initial password success page'
        at InitialPasswordSuccessPage
    }

    private String createLicenceUser(username) {
        def restTemplate = new RestTemplate()
        def tokenHeaders = new HttpHeaders()
        tokenHeaders.put('Authorization', ['Basic ZWxpdGUyYXBpY2xpZW50OmNsaWVudHNlY3JldA=='])
        def tokenEntity = new HttpEntity<>('', tokenHeaders)
        String response = restTemplate.postForEntity("$baseUrl/auth/oauth/token?grant_type=password&username=ITAG_USER_ADM&password=password123456", tokenEntity, String.class).getBody()
        String token = new JsonSlurper().parseText(response).access_token

        def headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        headers.put('Authorization', ["Bearer " + token])

        def user = [email: "bob@bobdigital.justice.gov.uk", firstName: "Bob", lastName: "Smith"] as NewUser
        HttpEntity<String> entity = new HttpEntity<String>(new JsonBuilder(user).toPrettyString(), headers)
        restTemplate.exchange("$baseUrl/auth/api/authuser/${username}", HttpMethod.PUT, entity, String.class).body
    }
}
