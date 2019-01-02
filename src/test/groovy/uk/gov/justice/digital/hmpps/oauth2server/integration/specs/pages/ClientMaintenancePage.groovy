package uk.gov.justice.digital.hmpps.oauth2server.integration.specs.pages

import geb.Page

class ClientMaintenancePage extends Page {

    static url = "/auth/ui/clients/form"

    static at = {
        browser.currentUrl.contains(url)
        headingText startsWith('Edit client')
    }

    static content = {
        headingText { $('#main-content h1').text() }
    }
}
