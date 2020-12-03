package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentList
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy
import uk.gov.justice.digital.hmpps.oauth2server.resource.AzureOIDCExtension

class HomePageSpecification : AbstractDeliusAuthSpecification() {
  @Page
  private lateinit var homePage: HomePage

  @Test
  fun `Log in with licences user`() {
    goTo(loginPage).loginAs("CA_USER")
    homePage.isAt()

    homePage.checkLinks("NOMIS", "HDC", "DETAILS")
  }

  @Test
  fun `Log in with nomis user`() {
    goTo(loginPage).loginAs("ITAG_USER", "password")
    homePage.isAt()

    homePage.checkLinks("CATTOOL", "NOMIS", "KW", "DETAILS", "USERADMIN")
  }

  @Test
  fun `Log in with azuread user`() {
    AzureOIDCExtension.azureOIDC.stubToken("email_not_matched@digital.justice.gov.uk")
    goTo(loginPage).clickAzureOIDCLink()
    homePage.isAt()

    homePage.checkLinks("DETAILS")
  }

  @Test
  fun `Log in with azuread user mapped to auth users`() {
    AzureOIDCExtension.azureOIDC.stubToken("auth_test2@digital.justice.gov.uk")
    goTo(loginPage).clickAzureOIDCLink()
    homePage.isAt()

    homePage.checkLinks("DETAILS", "USERADMIN", "OAUTHADMIN")
  }
}

@PageUrl("/")
class HomePage : AuthPage<HomePage>("HMPPS Digital Services - Home", "Select service") {
  @FindBy(css = "#DETAILS")
  private lateinit var accountDetails: FluentWebElement

  @FindBy(css = ".govuk-heading-m a.govuk-link")
  private lateinit var serviceLinks: FluentList<FluentWebElement>

  fun navigateToAccountDetails() {
    assertThat(accountDetails.text()).isEqualTo("Manage account details")
    accountDetails.click()
  }

  fun checkLinks(vararg links: String) {
    assertThat(serviceLinks.attributes("id")).containsExactlyInAnyOrderElementsOf(links.toList())
  }
}
