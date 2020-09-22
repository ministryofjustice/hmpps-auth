package uk.gov.justice.digital.hmpps.oauth2server.model

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel(description = "User Role")
data class UserRole(
    @ApiModelProperty(required = true, value = "Role Code", example = "GLOBAL_SEARCH")
    val roleCode: String)
