package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions
import org.fluentlenium.core.annotation.Page
import org.junit.jupiter.api.Test

class DeliusDownLoginSpecification : AbstractAuthSpecification() {

  @Page
  private lateinit var homePage: HomePage

  @Page
  private lateinit var accountDetailsPage: AccountDetailsPage

  @Test
  fun `Delius unavailable shows in error`() {

    goTo(loginPage)
      .loginError("DELIUS_USER", "password")
      .checkError(
        "Enter a valid username and password. You will be locked out if you enter the wrong details 3 times." +
          "\nDelius is experiencing issues. Please try later if you are attempting to sign in using your Delius credentials."
      )
  }

  @Test
  fun `Delius unavailable doesn't prevent logging in as auth user`() {
    goTo(loginPage)
      .loginAs("AUTH_USER", "password123456")
    homePage
      .isAtPage()
    Assertions.assertThat(accountDetailsPage.getCurrentName()).isEqualTo("A. Only")
  }

  @Test
  fun `Delius unavailable doesn't prevent logging in as nomis user`() {
    goTo(loginPage)
      .loginAs("ITAG_USER", "password")
    homePage
      .isAtPage()
    Assertions.assertThat(accountDetailsPage.getCurrentName()).isEqualTo("I. User")
  }
}
