package uk.gov.justice.digital.hmpps.oauth2server.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails;
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

    @ApiModelProperty(required = true, value = "Authentication Source", notes = "auth for auth users, nomis for nomis authenticated users", example = "nomis", position = 4)
    private final AuthSource authSource;

    @ApiModelProperty(value = "Staff Id", notes = "Deprecated, use userId instead", example = "231232", position = 5)
    @Deprecated
    private final Long staffId;

    @ApiModelProperty(value = "Current Active Caseload", notes = "Deprecated, retrieve from elite2 API rather than auth", example = "MDI", position = 6)
    @Deprecated
    private final String activeCaseLoadId;

    @ApiModelProperty(value = "User Id", notes = "Unique identifier for user, will be UUID for auth users or staff ID for nomis users", example = "231232", position = 7)
    private final String userId;

    @ApiModelProperty(value = "Delius Id", notes = "The Delius username for the user, if one exists", example = "JaneDoeNPS", position = 8)
    @Setter
    private String deliusId;

    public static UserDetail fromPerson(final UserPersonDetails u) {
        final var authSource = AuthSource.fromNullableString(u.getAuthSource());
        final var builder = builder()
                .username(u.getUsername())
                .active(u.isEnabled())
                .name(u.getName())
                .authSource(authSource);

        if (authSource == AuthSource.nomis) {
            final var staffUserAccount = (NomisUserPersonDetails) u;
            final var staffId = staffUserAccount.getStaff().getStaffId();
            builder.staffId(staffId);
            if (staffUserAccount.getActiveCaseLoadId() != null) {
                builder.activeCaseLoadId(staffUserAccount.getActiveCaseLoadId());
            }
        }
        builder.userId = u.getUserId();
        return builder.build();
    }

    public static UserDetail fromUsername(final String username) {
        return builder().username(username).build();
    }
}
