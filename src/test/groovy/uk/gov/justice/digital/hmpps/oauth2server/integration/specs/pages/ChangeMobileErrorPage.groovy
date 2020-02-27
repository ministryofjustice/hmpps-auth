package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

class ChangeMobileErrorPage extends ChangeMobilePage {

  static at = {
    title == 'Error: HMPPS Digital Services - Change Mobile Number'
  }

  static content = {
    errorText { $('#error-detail').text() }
  }
}
