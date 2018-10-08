package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "oauth_client_details")
@Data()
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"id"})
@ToString(of = { "id", "authorizedGrantTypes"})
public class ClientDetail {

    @Id()
    @Column(name = "client_id", nullable = false, length = 64 )
    private String id;

    @Column(name = "resource_ids")
    private String resourceIds;

    @Column(name = "client_secret", nullable = false, length = 100 )
    private String clientSecret;

    @Column(name = "scope", length = 200 )
    private String scope;

    @Column(name = "authorized_grant_types", nullable = false, length = 200 )
    private String authorizedGrantTypes;

    @Column(name = "web_server_redirect_uri")
    private String webServerRedirectUri;

    @Column(name = "authorities")
    private String authorities;

    @Column(name = "access_token_validity")
    private Long accessTokenValidity;

    @Column(name = "refresh_token_validity" )
    private Long refreshTokenValidity;

    @Column(name = "autoapprove", length = 200 )
    private String autoApprove;

    @Column(name = "additional_information")
    private String additionalInformation;

}
