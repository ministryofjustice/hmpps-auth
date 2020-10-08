package uk.gov.justice.digital.hmpps.oauth2server.model

import io.swagger.annotations.ApiModelProperty

data class ErrorDetail(
  @ApiModelProperty(required = true, value = "Error", example = "Not Found", position = 1)
  val error: String,

  @ApiModelProperty(required = true, value = "Error description", example = "User not found.", position = 2)
  val error_description: String,

  @ApiModelProperty(required = false, value = "Field in error", example = "username", position = 3)
  val field: String? = null
)
