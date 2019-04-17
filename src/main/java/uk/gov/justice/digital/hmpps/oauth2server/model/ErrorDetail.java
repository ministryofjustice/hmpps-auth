package uk.gov.justice.digital.hmpps.oauth2server.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ErrorDetail {
    @ApiModelProperty(required = true, value = "Error", example = "Not Found", position = 1)
    private final String error;
    @ApiModelProperty(required = true, value = "Error description", example = "User not found.", position = 2)
    private final String error_description;
    @ApiModelProperty(required = false, value = "Field in error", example = "username", position = 3)
    private final String field;
}
