package uk.gov.justice.digital.hmpps.oauth2server.config;


import lombok.Data;

import java.util.List;

@Data
public class OauthClientConfig {
    private String clientId;
    private String resourceIds;
    private String clientSecret;
    private List<String> scope;
    private List<String> authorizedGrantTypes;
    private List<String> webServerRedirectUri;
    private List<String> authorities;
    private Integer accessTokenValidity;
    private Integer refreshTokenValidity;
    private String additionalInformation;
    private List<String> autoApprove;
    private boolean autoApproved;
}
