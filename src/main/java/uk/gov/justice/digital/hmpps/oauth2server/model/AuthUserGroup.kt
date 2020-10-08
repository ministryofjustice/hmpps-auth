package uk.gov.justice.digital.hmpps.oauth2server.model

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group

@ApiModel(description = "User Group")
data class AuthUserGroup(
  @ApiModelProperty(required = true, value = "Group Code", example = "HDC_NPS_NE")
  val groupCode: String,

  @ApiModelProperty(required = true, value = "Group Name", example = "HDC NPS North East")
  val groupName: String
) {

  constructor(g: Group) : this(g.groupCode, g.groupName)
}
