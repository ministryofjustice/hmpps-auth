package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Intended for use with OAuth2 client credentials (system-to-system) authentication
 * to supplement access token with system user identified by an external user identifier
 * and identifier type (provided as authentication request parameters).
 */
@SuppressWarnings("deprecation")
@Component
@AllArgsConstructor
public class ExternalIdAuthenticationHelper {
    private static final String REQUEST_PARAM_USER_NAME = "username";

    private final UserService userService;

    public UserPersonDetails getUserDetails(final Map<String, String> requestParameters, final boolean skipUserCheck) {
        if (requestParameters.containsKey(REQUEST_PARAM_USER_NAME)) {
            final var username = requestParameters.get(REQUEST_PARAM_USER_NAME);
            if (skipUserCheck) {
                return new UserDetailsImpl(username, null, List.of(), "none", null);
            }
            return loadByUsername(username);
        }
        return null;
    }

    private UserPersonDetails loadByUsername(final String username) {
        if (StringUtils.isBlank(username)) {
            throw new OAuth2AccessDeniedException("Invalid username identifier details.");
        }

        return userService.findMasterUserPersonDetails(username).orElseThrow(() -> new OAuth2AccessDeniedException("No user found matching username."));
    }
}
