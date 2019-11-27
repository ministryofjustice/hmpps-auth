package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.stereotype.Component;

import javax.persistence.EntityNotFoundException;
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
    private static final String REQUEST_PARAM_USER_ID_TYPE = "user_id_type";
    private static final String REQUEST_PARAM_USER_ID = "user_id";
    private static final String REQUEST_PARAM_USER_NAME = "username";

    private final UserService userService;

    public UserPersonDetails getUserDetails(final Map<String, String> requestParameters, final boolean skipUserCheck) {
        if (requestParameters.containsKey(REQUEST_PARAM_USER_ID_TYPE) &&
                requestParameters.containsKey(REQUEST_PARAM_USER_ID)) {
            return loadByUserIdType(requestParameters.get(REQUEST_PARAM_USER_ID_TYPE), requestParameters.get(REQUEST_PARAM_USER_ID));
        }
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

    private UserPersonDetails loadByUserIdType(final String userIdType, final String userId) {
        // Extract values - if either are empty/null, throw auth failed exception
        if (StringUtils.isBlank(userIdType) || StringUtils.isBlank(userId)) {
            throw new OAuth2AccessDeniedException("Invalid external user identifier details.");
        }

        try {
            final var userDetail = userService.getUserByExternalIdentifier(userIdType, userId);
            // Get full user details, with authorities, etc.
            return userService.findMasterUserPersonDetails(userDetail.getUsername()).orElseThrow(() -> new OAuth2AccessDeniedException("User external identifier found, but no user found."));

        } catch (final EntityNotFoundException ex) {
            throw new OAuth2AccessDeniedException("No user found matching external user identifier details.");
        }
    }
}
