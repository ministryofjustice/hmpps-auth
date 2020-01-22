package uk.gov.justice.digital.hmpps.oauth2server.integration.specs

import geb.spock.GebReportingSpec
import org.apache.commons.lang3.RandomStringUtils
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.HomePage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.LoginPage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.UserDetailsErrorPage
import uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages.UserDetailsPage

import static uk.gov.justice.digital.hmpps.oauth2server.integration.specs.model.UserAccount.AUTH_ADM

class UserDetailsSpecification extends GebReportingSpec {

  def "A user can change their user details"() {
    given: 'I login as an auth user'
    to LoginPage
    loginAs AUTH_ADM, 'password123456'

    when: 'I navigate to the user details page'
    at HomePage
    def currentName = principalName
    to UserDetailsPage

    then: 'The user details page is displayed with existing name'
    at UserDetailsPage
    "${firstNameInput} ${lastNameInput}" == currentName

    when: 'I change my name'
    def randomLastName = RandomStringUtils.random(6, true, true)
    userDetails("Joe", randomLastName)

    then: 'The Home page is displayed with my new name'
    at HomePage
    principalName == "Joe $randomLastName"
  }

  def "A user can cancel changing their user details"() {
    given: 'I login as an auth user'
    to LoginPage
    loginAs AUTH_ADM, 'password123456'

    when: 'I navigate to the user details page'
    at HomePage
    def currentName = principalName
    to UserDetailsPage

    and: 'I change my name'
    $('form').firstName = "Joe"
    $('form').lastName = "Cancel"

    and: 'I then cancel the change'
    cancel()

    then: 'The Home page is displayed with original name'
    at HomePage
    principalName == currentName
  }

  def "Errors are displayed to the user"() {
    given: 'I login as an auth user'
    to LoginPage
    loginAs AUTH_ADM, 'password123456'
    to UserDetailsPage

    when: 'The user details page is displayed'
    at UserDetailsPage

    and: 'I change my name'
    userDetails("Jo", "     ")

    then: 'The error page is displayed with messages'
    at UserDetailsErrorPage
    errorText == "Enter your last name"

    and: 'Fields are populated with the incorrect data'
    firstNameInput == "Jo"
    lastNameInput == "     "

    when: 'I change my name'
    def tooLongLastName = RandomStringUtils.random(51, true, true)
    userDetails("Jo", tooLongLastName)

    then: 'The error page is displayed with messages'
    at UserDetailsErrorPage
    errorText == "Your last name must be at 50 characters or less"
  }
}
