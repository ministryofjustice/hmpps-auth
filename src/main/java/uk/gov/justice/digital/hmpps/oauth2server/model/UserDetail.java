package uk.gov.justice.digital.hmpps.oauth2server.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description = "User Details")
@Data
@Builder
@AllArgsConstructor
public class UserDetail {
    @ApiModelProperty(required = true, value = "Username", example = "DEMO_USER1", position = 1)
    private final String username;

    @ApiModelProperty(required = true, value = "Active", example = "false", position = 2)
    private final boolean active;

    @ApiModelProperty(required = true, value = "Name", example = "John Smith", position = 3)
    private final String name;

    @ApiModelProperty(required = true, value = "Authentication Source", example = "nomis", position = 4)
    private final AuthSource authSource;

    @ApiModelProperty(value = "Staff Id", example = "231232")
    private final Long staffId;

    @ApiModelProperty(value = "Current Active Caseload", example = "MDI", position = 5)
    @Deprecated
    private final String activeCaseLoadId;

    public static UserDetail fromPerson(final UserPersonDetails u) {
        final var authSource = AuthSource.fromNullableString(u.getAuthSource());
        final var builder = builder().
                username(u.getUsername()).
                active(u.isEnabled()).
                name(u.getName()).
                authSource(authSource);

        if (authSource == AuthSource.NOMIS) {
            final var staffUserAccount = (StaffUserAccount) u;
            builder.staffId(staffUserAccount.getStaff().getStaffId());
            if (staffUserAccount.getActiveCaseLoadId() != null) {
                builder.activeCaseLoadId(staffUserAccount.getActiveCaseLoadId());
            }
        }
        return builder.build();
    }

    public static UserDetail fromUsername(final String username) {
        return builder().username(username).build();
    }
}
