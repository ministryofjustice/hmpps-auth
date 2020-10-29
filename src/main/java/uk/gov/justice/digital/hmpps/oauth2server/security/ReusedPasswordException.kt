package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.security.core.AuthenticationException

class ReusedPasswordException : AuthenticationException("Password cannot be reused")
