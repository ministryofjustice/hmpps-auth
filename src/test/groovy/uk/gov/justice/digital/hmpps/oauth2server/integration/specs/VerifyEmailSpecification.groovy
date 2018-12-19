package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import geb.spock.GebReportingSpec
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.*

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.DM_USER

class VerifyEmailSpecification extends GebReportingSpec {

    public static final String clientBaseUrl = 'http://localhost:8081/login'

    def "A user can cancel email verification"() {
        given: 'I login with a non verified email user'
        to LoginPage
        loginAs DM_USER, 'password123456'

        when: 'The Verify Email page is displayed'
        at VerifyEmailPage

        and: 'I canncel the verification process'
        cancel()

        then: 'The User Home page is displayed'
        at UserHomePage
    }

    def "A user can verify a previously chosen email"() {
        given: 'I login with a non verified email user'
        to LoginPage
        loginAs DM_USER, 'password123456'

        when: 'The Verify Email page is displayed'
        at VerifyEmailPage
        verifyExistingEmailAs 'dm_user@digital.justice.gov.uk'

        and: 'The Verify Email sent page is displayed'
        at VerifyEmailSentPage
        String verifyLink = $('#verifyLink').@href
        continueProcess()

        and: 'The User Home page is displayed'
        at UserHomePage

        and: 'I can then verify my email address'
        browser.go verifyLink

        then:
        at VerifyEmailConfirmPage
    }

    def "A user can verify a chosen email"() {
        given: 'I login with a non verified email user'
        to LoginPage
        loginAs DM_USER, 'password123456'

        when: 'The Verify Email page is displayed'
        at VerifyEmailPage
        verifyExistingEmailAs 'dm_user@digital.justice.gov.uk'

        and: 'The Verify Email sent page is displayed'
        at VerifyEmailSentPage
        String verifyLink = $('#verifyLink').@href
        continueProcess()

        and: 'The User Home page is displayed'
        at UserHomePage

        and: 'I can then verify my email address'
        browser.go verifyLink

        then:
        at VerifyEmailConfirmPage
    }
}
