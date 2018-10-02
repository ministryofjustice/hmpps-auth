package uk.gov.justice.digital.hmpps.oauth2server.config;


import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class OauthClientConfig {
    private String clientId;
    private String resourceIds;
    private String clientSecret;
    private List<String> scope;
    private List<String> authorizedGrantTypes;
    private Set<String> webServerRedirectUri;
    private List<String> authorities;
    private Integer accessTokenValidity;
    private Integer refreshTokenValidity;
    private String additionalInformation;
    private List<String> autoApprove;
    private boolean autoApproved;
}
