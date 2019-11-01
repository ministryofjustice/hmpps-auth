package uk.gov.justice.digital.hmpps.oauth2server.config;


import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import uk.gov.justice.digital.hmpps.oauth2server.security.ExternalIdAuthenticationHelper;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JWTTokenEnhancer implements TokenEnhancer {
    private static final String ADD_INFO_AUTH_SOURCE = "auth_source";
    static final String ADD_INFO_USER_NAME = "user_name";
    static final String ADD_INFO_USER_ID = "user_id";
    private static final String ADD_INFO_AUTHORITIES = "authorities";

    @Autowired
    private ExternalIdAuthenticationHelper externalIdAuthenticationHelper;

    @Override
    public OAuth2AccessToken enhance(final OAuth2AccessToken accessToken, final OAuth2Authentication authentication) {
        final Map<String, Object> additionalInfo;

        if (authentication.isClientOnly()) {
            additionalInfo = addUserFromExternalId(authentication, accessToken);
        } else {
            final var userAuthentication = authentication.getUserAuthentication();
            final var userDetails = (UserPersonDetails) userAuthentication.getPrincipal();
            final var userId = StringUtils.defaultString(userDetails.getUserId(), userAuthentication.getName());
            additionalInfo = Map.of(
                    ADD_INFO_AUTH_SOURCE, StringUtils.defaultIfBlank(userDetails.getAuthSource(), "none"),
                    ADD_INFO_USER_NAME, userAuthentication.getName(),
                    ADD_INFO_USER_ID, userId);
        }

        ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInfo);

        return accessToken;
    }

    // Checks for existence of request parameters that define an external user identifier type and identifier.
    // If both identifier type and identifier parameters are present, they will be used to attempt to identify
    // a system user account. If a system user account is not identified, an exception will be thrown to ensure
    // authentication fails. If a system user account is identified, the user id will be added to the token,
    // the token's scope will be 'narrowed' to include 'write' scope and the system user's roles will be added
    // to token authorities.
    private Map<String, Object> addUserFromExternalId(final OAuth2Authentication authentication, final OAuth2AccessToken accessToken) {
        final Map<String, Object> additionalInfo = new HashMap<>();

        // Determine if both user_id_type and user_id request parameters exist.
        final var request = authentication.getOAuth2Request();

        final var requestParams = request.getRequestParameters();
        final var skipUserCheck = request.getScope().contains("proxy-user");

        // Non-blank user_id_type and user_id to check - delegate to external identifier auth component
        final var userDetails = externalIdAuthenticationHelper.getUserDetails(requestParams, skipUserCheck);

        if (userDetails != null) {
            additionalInfo.put(ADD_INFO_AUTH_SOURCE, userDetails.getAuthSource());
            additionalInfo.put(ADD_INFO_USER_NAME, userDetails.getUsername());
            additionalInfo.put(ADD_INFO_USER_ID, userDetails.getUserId());

            // TODO: remove once write credential has been added to clients
            // Add "write" scope with those for client credentials
            final Set<String> scope = new HashSet<>(request.getScope());
            scope.add("write");
            ((DefaultOAuth2AccessToken) accessToken).setScope(scope);

            // Merge user authorities with those associated with client credentials
            additionalInfo.put(ADD_INFO_AUTHORITIES,
                    request.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()));
        } else {
            additionalInfo.put(ADD_INFO_AUTH_SOURCE, "none");
        }

        return additionalInfo;
    }
}
