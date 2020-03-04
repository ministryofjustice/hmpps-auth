package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.PageUrl
import org.junit.jupiter.api.Test


class AccountDetailsSpecification : AbstractAuthSpecification() {
  @Test
  fun `account details`() {
    val accountDetails = goTo(loginPage)
        .loginAs("AUTH_RO_USER", "password123456")
        .navigateToAccountDetails()
    accountDetails.checkDetails()
  }
}

@PageUrl("/account-details")
class AccountDetailsPage : AuthPage("HMPPS Digital Services - Account Details", "Account details") {
  fun checkDetails(): AccountDetailsPage {
    assertThat(el("[data-qa='username']").text()).isEqualTo("AUTH_RO_USER")
    assertThat(el("[data-qa='name']").text()).isEqualTo("Ryan-Auth Orton")
    assertThat(el("[data-qa='lastLoggedIn']").text()).isNotBlank()
    assertThat(el("[data-qa='passwordExpiry']").text()).isEqualTo("28 January 3013 13:23")
    assertThat(el("[data-qa='email']").text()).isEqualTo("auth_ro_user@digital.justice.gov.uk")
    assertThat(el("[data-qa='verified']").text()).isEqualTo("yes")
    assertThat(el("[data-qa='mobile']").text()).isBlank()
    assertThat(el("[data-qa='mobileVerified']").text()).isEqualTo("no")
    return this
  }
}
