package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.security.core.AuthenticationException

class PasswordValidationFailureException : AuthenticationException("Password cannot be reused")
