package uk.gov.justice.digital.hmpps.oauth2server.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description = "User email details")
@Data
@AllArgsConstructor
public class EmailAddress {
    @ApiModelProperty(required = true, value = "Username", example = "DEMO_USER1", position = 1)
    private final String username;

    @ApiModelProperty(required = true, value = "Email", example = "john.smith@digital.justice.gov.uk", position = 2)
    private final String email;

    public static EmailAddress fromUserEmail(final UserEmail u) {
        return new EmailAddress(u.getUsername(), u.getEmail());
    }
}
