package uk.gov.justice.digital.hmpps.oauth2server.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@ApiModel(description = "User Role")
@Data
@AllArgsConstructor
public class UserRole {
    @ApiModelProperty(required = true, value = "Role Code", example = "LEI", position = 1)
    private final String roleCode;
}
