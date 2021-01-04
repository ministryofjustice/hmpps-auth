package uk.gov.justice.digital.hmpps.oauth2server.model

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority

@ApiModel(description = "User Role")
data class AuthUserAssignableRole(
  @ApiModelProperty(required = true, value = "Role Code", example = "LICENCE_RO")
  val roleCode: String,

  @ApiModelProperty(required = true, value = "Role Name", example = "Licence Responsible Officer")
  val roleName: String,

  @ApiModelProperty(required = true, value = "automatic", example = "TRUE")
  val automatic: Boolean
) {

  constructor(a: Authority, automatic: Boolean) : this(a.roleCode, a.roleName, automatic)
}
