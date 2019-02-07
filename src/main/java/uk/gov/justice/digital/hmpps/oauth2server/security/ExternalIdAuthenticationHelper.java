package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.stereotype.Component;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;

import javax.persistence.EntityNotFoundException;
import java.util.Map;

/**
 * Intended for use with OAuth2 client credentials (system-to-system) authentication
 * to supplement access token with system user identified by an external user identifier
 * and identifier type (provided as authentication request parameters).
 */
@Component
public class ExternalIdAuthenticationHelper {
    private static final String REQUEST_PARAM_USER_ID_TYPE = "user_id_type";
    private static final String REQUEST_PARAM_USER_ID = "user_id";
    private static final String REQUEST_PARAM_USER_NAME = "username";

    private final UserService userService;
    private final UserDetailsService userDetailsService;

    public ExternalIdAuthenticationHelper(final UserService userService, final UserDetailsService userDetailsService) {
        this.userService = userService;
        this.userDetailsService = userDetailsService;
    }

    public UserDetails getUserDetails(final Map<String, String> requestParameters) {
        if (requestParameters.containsKey(REQUEST_PARAM_USER_ID_TYPE) &&
                requestParameters.containsKey(REQUEST_PARAM_USER_ID)) {
            // Extract values - if either are empty/null, throw auth failed exception
            final var userIdType = requestParameters.get(REQUEST_PARAM_USER_ID_TYPE);
            final var userId = requestParameters.get(REQUEST_PARAM_USER_ID);

            if (StringUtils.isBlank(userIdType) || StringUtils.isBlank(userId)) {
                throw new OAuth2AccessDeniedException("Invalid external user identifier details.");
            }

            final StaffUserAccount userDetail;

            try {
                userDetail = userService.getUserByExternalIdentifier(userIdType, userId, true);
            } catch (final EntityNotFoundException ex) {
                throw new OAuth2AccessDeniedException("No user found matching external user identifier details.");
            }
            // Get full user details, with authorities, etc.
            return userDetailsService.loadUserByUsername(userDetail.getUsername());
        }
        if (requestParameters.containsKey(REQUEST_PARAM_USER_NAME)) {
            final var username = requestParameters.get(REQUEST_PARAM_USER_NAME);

            if (StringUtils.isBlank(username)) {
                throw new OAuth2AccessDeniedException("Invalid username identifier details.");
            }

            try {
                return userDetailsService.loadUserByUsername(username);
            } catch (final UsernameNotFoundException e) {
                throw new OAuth2AccessDeniedException("No user found matching username.");
            }
        }
        return null;
    }
}
