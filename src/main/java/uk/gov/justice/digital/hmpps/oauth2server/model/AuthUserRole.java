package uk.gov.justice.digital.hmpps.oauth2server.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@ApiModel(description = "User Role")
@Data
@AllArgsConstructor
public class AuthUserRole {
    @ApiModelProperty(required = true, value = "Role Name", example = "Licence Responsible Officer", position = 1)
    private final String roleName;

    @ApiModelProperty(required = true, value = "Role Code", example = "LICENCE_RO", position = 2)
    private final String roleCode;
}
