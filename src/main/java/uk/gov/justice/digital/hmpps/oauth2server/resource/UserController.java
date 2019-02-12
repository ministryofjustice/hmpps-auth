package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.google.common.collect.ImmutableMap;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;

import java.security.Principal;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class UserController {
    private final UserService userService;

    public UserController(final UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/user/me")
    public Map<String, Object> me(final Principal principal) {
        final var user = userService.findUser(principal.getName());

        return user.map(this::mapUser).orElse(Map.of("username", principal.getName()));
    }

    private Map<String, Object> mapUser(final UserPersonDetails u) {
        final var builder = ImmutableMap.<String, Object>builder().
                put("username", u.getUsername()).
                put("active", u.isEnabled()).
                put("name", u.getName()).
                put("authSource", u.getAuthSource());

        if (AuthSource.fromNullableString(u.getAuthSource()) == AuthSource.NOMIS) {
            final var staffUserAccount = (StaffUserAccount) u;
            builder.put("staffId", staffUserAccount.getStaff().getStaffId());
            if (staffUserAccount.getActiveCaseLoadId() != null) {
                builder.put("activeCaseLoadId", staffUserAccount.getActiveCaseLoadId());
            }
        }
        return builder.build();
    }

    @GetMapping("/api/user/me/roles")
    public Collection<Map<String, String>> myRoles(final Authentication authentication) {
        return authentication.getAuthorities().stream().
                map(a -> Map.of("roleCode", a.getAuthority().substring(5))). // remove ROLE_
                collect(Collectors.toList());
    }
}
