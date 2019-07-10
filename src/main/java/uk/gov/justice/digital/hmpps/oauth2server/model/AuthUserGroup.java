package uk.gov.justice.digital.hmpps.oauth2server.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Group;

@ApiModel(description = "User Group")
@Data
public class AuthUserGroup {
    @ApiModelProperty(required = true, value = "Group Code", example = "HDC_NPS_NE", position = 1)
    private final String groupCode;

    @ApiModelProperty(required = true, value = "Group Name", example = "HDC NPS North East", position = 2)
    private final String groupName;

    public AuthUserGroup(final Group group) {
        groupCode = group.getGroupCode();
        groupName = group.getGroupName();
    }
}
