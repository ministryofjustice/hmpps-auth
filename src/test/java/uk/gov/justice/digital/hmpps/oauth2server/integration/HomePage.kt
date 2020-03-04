package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.openqa.selenium.support.FindBy

@PageUrl("/")
class HomePage : AuthPage("HMPPS Digital Services - Home", "Select service") {
  @FindBy(css = "#principal-name")
  private lateinit var principalName: FluentWebElement
  @FindBy(css = "#DETAILS")
  private lateinit var accountDetails: FluentWebElement

  fun assertNameDisplayedCorrectly(name: String) {
    assertThat(principalName.text()).isEqualTo(name)
  }

  fun navigateToAccountDetails(): AccountDetailsPage {
    accountDetails.click()

    val accountDetailsPage = newInstance(AccountDetailsPage::class.java)
    accountDetailsPage.isAt()
    return accountDetailsPage
  }
}
