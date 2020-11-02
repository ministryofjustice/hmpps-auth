package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.security.authentication.AccountStatusException

internal class MissingCredentialsException : AccountStatusException("No credentials provided")
