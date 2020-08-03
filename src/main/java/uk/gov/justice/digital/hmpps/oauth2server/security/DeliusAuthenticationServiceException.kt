package uk.gov.justice.digital.hmpps.oauth2server.security

import org.springframework.security.authentication.InternalAuthenticationServiceException

class DeliusAuthenticationServiceException(username: String)
  : InternalAuthenticationServiceException(
    "Unable to retrieve information for $username from Delius.  We are unable to connect to Delius or there is an issue with $username in Delius")
