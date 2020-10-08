package uk.gov.justice.digital.hmpps.oauth2server.delius.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class UserDetails @JsonCreator constructor(
  @JsonProperty("userId") val userId: String,
  @JsonProperty("username") val username: String,
  @JsonProperty("surname") val surname: String,
  @JsonProperty("firstName") val firstName: String,
  @JsonProperty("email") val email: String,
  @JsonProperty("enabled") val enabled: Boolean,
  @JsonProperty("roles") val roles: List<UserRole>,
)

data class UserRole @JsonCreator constructor(@JsonProperty("name") val name: String)
