package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.fluentlenium.core.annotation.PageUrl

@PageUrl("/existing-password")
open class PasswordPromptPage : AuthPage<PasswordPromptPage>("HMPPS Digital Services - Change Password Request", "What is your current password?")

@PageUrl("/reset-password")
open class resetPasswordPage : AuthPage<resetPasswordPage>("HMPPS Digital Services - Reset Password", "Create a new password")
