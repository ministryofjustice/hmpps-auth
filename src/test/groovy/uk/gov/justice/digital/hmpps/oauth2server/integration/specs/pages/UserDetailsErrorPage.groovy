package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

class UserDetailsErrorPage extends UserDetailsPage {

  static at = {
    title == 'Error: HMPPS Digital Services - Update Personal Details'
  }

  static content = {
    errorText { $('#error-detail').text() }
    lastNameErrorText { $('#lastName-error').text() }
    firstNameErrorText { $('#firstName-error').text() }
  }
}
