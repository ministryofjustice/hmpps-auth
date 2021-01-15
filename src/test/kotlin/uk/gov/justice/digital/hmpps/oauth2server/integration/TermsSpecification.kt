package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.junit.jupiter.api.Test

class TermsSpecification : AbstractAuthSpecification() {
  @Page
  private lateinit var termsPage: TermsPage

  @Test
  fun `The terms link is present`() {
    goTo(loginPage).viewTerms()
    termsPage.isAt()
  }

  @Test
  fun `Accept terms and conditions`() {
    goTo(loginPage).viewTerms()
    termsPage.accept()
    loginPage.isAt()
  }
}

@PageUrl("/terms")
class TermsPage : AuthPage<TermsPage>("HMPPS Digital Services - Terms and conditions", "Terms and conditions") {
  fun accept() {
    el("a[data-qa='back-link']").click()
  }
}
