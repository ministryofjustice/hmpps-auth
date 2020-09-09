package uk.gov.justice.digital.hmpps.oauth2server.config;


import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

@SuppressWarnings("deprecation")
public class JWTTokenEnhancer implements TokenEnhancer {
    private static final String ADD_INFO_AUTH_SOURCE = "auth_source";
    static final String ADD_INFO_NAME = "name";
    static final String ADD_INFO_USER_NAME = "user_name";
    static final String ADD_INFO_USER_ID = "user_id";
    static final String SUBJECT = "sub";
    private static final String ADD_INFO_AUTHORITIES = "authorities";
    private static final String REQUEST_PARAM_USER_NAME = "username";
    private static final String REQUEST_PARAM_AUTH_SOURCE = "auth_source";

    @Autowired
    private JdbcClientDetailsService clientsDetailsService;

    @Override
    public OAuth2AccessToken enhance(final OAuth2AccessToken accessToken, final OAuth2Authentication authentication) {
        final Map<String, Object> additionalInfo;

        if (authentication.isClientOnly()) {
            additionalInfo = addUserFromExternalId(authentication, accessToken);
        } else {
            final var userAuthentication = authentication.getUserAuthentication();
            final var userDetails = (UserPersonDetails) userAuthentication.getPrincipal();
            final var userId = StringUtils.defaultString(userDetails.getUserId(), userAuthentication.getName());

            final var clientDetails = clientsDetailsService.loadClientByClientId(authentication.getOAuth2Request().getClientId());
            // note that DefaultUserAuthenticationConverter will automatically add user_name to the access token, so
            // removal of user_name will only affect the authorisation code response and not the access token field.
            additionalInfo = filterAdditionalInfo(
                    Map.of(SUBJECT, userAuthentication.getName(),
                            ADD_INFO_AUTH_SOURCE, StringUtils.defaultIfBlank(userDetails.getAuthSource(), "none"),
                            ADD_INFO_USER_NAME, userAuthentication.getName(),
                            ADD_INFO_USER_ID, userId,
                            ADD_INFO_NAME, userDetails.getName()), clientDetails);
        }

        ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInfo);
        return accessToken;
    }

    private Map<String, Object> filterAdditionalInfo(final Map<String, Object> info, final ClientDetails clientDetails) {
        final var jwtFields = (String) clientDetails.getAdditionalInformation().getOrDefault("jwtFields", "");

        final var entries = StringUtils.isBlank(jwtFields) ? Collections.<Entry<String, Boolean>>emptySet() :
                Stream.of(jwtFields.split(",")).collect(
                        Collectors.toMap(f -> f.substring(1), f -> f.charAt(0) == '+')).entrySet();
        final var fieldsToKeep = entries.stream().filter(Entry::getValue).map(Entry::getKey).collect(Collectors.toSet());
        final var fieldsToRemove = entries.stream().filter(not(Entry::getValue)).map(Entry::getKey).collect(Collectors.toSet());

        // for field addition, just remove from deprecated fields
        fieldsToRemove.removeAll(fieldsToKeep);

        return info.entrySet().stream().filter(e -> !fieldsToRemove.contains(e.getKey())).collect(
                Collectors.toMap(Entry::getKey, Entry::getValue));
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

        final var username = getUsernameFromRequestParam(requestParams);
        if (username.isPresent()) {
            additionalInfo.put(ADD_INFO_USER_NAME, username.get());
            additionalInfo.put(SUBJECT, username.get());
        } else {
            additionalInfo.put(SUBJECT, authentication.getName());
        }
        additionalInfo.put(ADD_INFO_AUTH_SOURCE, getAuthSourceFromRequestParam(requestParams));

        return additionalInfo;
    }

    private String getAuthSourceFromRequestParam(final Map<String, String> requestParams) {
        if (requestParams.containsKey(REQUEST_PARAM_AUTH_SOURCE)) {
            final var authSource = StringUtils.lowerCase(requestParams.get(REQUEST_PARAM_AUTH_SOURCE));
            if (StringUtils.isNotBlank(authSource)) {
                return authSource;
            }
        }
        return "none";
    }

    @NotNull
    private Optional<String> getUsernameFromRequestParam(final Map<String, String> requestParams) {
        if (requestParams.containsKey(REQUEST_PARAM_USER_NAME)) {
            final var username = StringUtils.upperCase(requestParams.get(REQUEST_PARAM_USER_NAME));
            if (StringUtils.isNotBlank(username)) {
                return Optional.of(username);
            }
        }
        return Optional.empty();
    }
}
