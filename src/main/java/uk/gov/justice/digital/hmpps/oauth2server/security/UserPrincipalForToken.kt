package uk.gov.justice.digital.hmpps.oauth2server.security

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

class UserPrincipalForToken @JsonCreator constructor(@param:JsonProperty("username") val username: String)
