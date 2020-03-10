package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.fluentlenium.core.annotation.PageUrl

@PageUrl("/existing-email")
open class PasswordPromptForEmailPage : AuthPage<PasswordPromptForEmailPage>("HMPPS Digital Services - Change Email Request", "What is your current password?")
