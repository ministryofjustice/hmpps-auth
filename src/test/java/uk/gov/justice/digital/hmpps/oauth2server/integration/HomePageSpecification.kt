package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy


class HomePageSpecification : AbstractAuthSpecification() {
  @Page
  private lateinit var homePage: HomePage

  @Test
  fun `Log in with licences user`() {
    goTo(loginPage).loginAs("CA_USER")
    homePage.isAt()

    homePage.checkNomisLink()
        .checkHdcLink()
  }

  @Test
  fun `Log in with nomis user`() {
    goTo(loginPage).loginAs("ITAG_USER", "password")
    homePage.isAt()

    homePage.checkNomisLink()
        .checkHdcLinkNotPresent()
  }
}

@PageUrl("/")
class HomePage : AuthPage<HomePage>("HMPPS Digital Services - Home", "Select service") {
  @FindBy(css = "#DETAILS")
  private lateinit var accountDetails: FluentWebElement

  fun navigateToAccountDetails() {
    assertThat(accountDetails.text()).isEqualTo("Manage account details")
    accountDetails.click()
  }

  fun checkNomisLink(): HomePage {
    assertThat(el("#NOMIS").text()).isEqualTo("Digital Prison Service")
    return this
  }

  fun checkHdcLink() {
    assertThat(el("#HDC").text()).isEqualTo("Home Detention Curfew")
  }

  fun checkHdcLinkNotPresent() {
    assertThat(find("#HDC")).isEmpty()
  }
}
