package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.junit.jupiter.api.Test

class ChangeEmailSpecification : AbstractAuthSpecification() {

  @Page
  private lateinit var changeEmail: PasswordPromptForEmailPage

  @Test
  fun `Change email flow`() {
    goTo(loginPage)
      .loginExistingPasswordChangeEmail("AUTH_CHANGE_EMAIL", "password123456")
    changeEmail
      .isAtPage()
  }
}

@PageUrl("/existing-email")
open class PasswordPromptForEmailPage : AuthPage<PasswordPromptForEmailPage>(
  "HMPPS Digital Services - Change Email Request",
  "What is your current password?"
)

@PageUrl("/verify-email")
open class ChangeEmailPage :
  AuthPage<ChangeEmailPage>("HMPPS Digital Services - Change Email", "What is your new email address?")
