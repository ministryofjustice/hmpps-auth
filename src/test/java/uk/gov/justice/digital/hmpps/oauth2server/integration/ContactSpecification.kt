package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class ContactSpecification : AbstractAuthSpecification() {
  @Page
  private lateinit var contactPage: ContactPage

  @Test
  fun `The contact us link is present`() {
    goTo(loginPage).viewContact()
    contactPage.isAt()
  }

  @Test
  fun `View contact details`() {
    goTo(contactPage)
        .checkHdcDetails()
        .checkNomisDetails()
        .checkMoicDetails()
  }

  @Test
  fun `Return to login page`() {
    goTo(contactPage).back()
    loginPage.isAt()
  }
}

@PageUrl("/contact")
class ContactPage : AuthPage<ContactPage>("HMPPS Digital Services - Contact us", "Contact us") {
  @FindBy(css = "#HDC")
  private lateinit var hdcSection: FluentWebElement

  @FindBy(css = "#NOMIS")
  private lateinit var nomisSection: FluentWebElement

  @FindBy(css = "#POM")
  private lateinit var moicSection: FluentWebElement

  fun checkHdcDetails(): ContactPage {
    assertThat(hdcSection.el("h3").text()).isEqualTo("Home Detention Curfew")
    assertThat(hdcSection.el("a").text()).isEqualTo("hdcdigitalservice@digital.justice.gov.uk")
    assertThat(hdcSection.el("a").attribute("href")).isEqualTo("mailto:hdcdigitalservice@digital.justice.gov.uk")
    return this
  }

  fun checkNomisDetails(): ContactPage {
    assertThat(nomisSection.el("h3").text()).isEqualTo("Digital Prison Service")
    assertThat(nomisSection.el("a").text()).isEqualTo("feedback@digital.justice.gov.uk")
    assertThat(nomisSection.el("a").attribute("href")).isEqualTo("mailto:feedback@digital.justice.gov.uk")
    return this
  }

  fun checkMoicDetails(): ContactPage {
    assertThat(moicSection.el("h3").text()).isEqualTo("Allocate a POM Service")
    assertThat(moicSection.el("p").text()).isEqualTo("Contact us using the online form")
    assertThat(moicSection.el("a").text()).isEqualTo("online form")
    assertThat(moicSection.el("a").attribute("href")).isEqualTo("https://moic.service.justice.gov.uk/help")
    return this
  }

  fun back() {
    el("a[data-qa='back-link']").click()
  }
}
