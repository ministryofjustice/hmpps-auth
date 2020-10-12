package uk.gov.justice.digital.hmpps.oauth2server.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User

@JsonInclude(NON_NULL)
@ApiModel(description = "User email details")
data class EmailAddress(
  @ApiModelProperty(required = true, value = "Username", example = "DEMO_USER1", position = 1)
  val username: String,

  @ApiModelProperty(required = true, value = "Email", example = "john.smith@digital.justice.gov.uk", position = 2)
  val email: String,
) {

  constructor(u: User) : this(u.username, u.email)
}
