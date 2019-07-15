package uk.gov.justice.digital.hmpps.oauth2server.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Authority;

@ApiModel(description = "User Role")
@Data
public class AuthUserRole {
    @ApiModelProperty(required = true, value = "Role Name", example = "Licence Responsible Officer", position = 1)
    private final String roleName;

    @ApiModelProperty(required = true, value = "Role Code", example = "LICENCE_RO", position = 2)
    private final String roleCode;

    public AuthUserRole(final Authority a) {
        roleCode = a.getRoleCode();
        roleName = a.getRoleName();
    }
}
