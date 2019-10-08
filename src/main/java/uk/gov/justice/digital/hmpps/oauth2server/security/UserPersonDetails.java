package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.userdetails.UserDetails;

public interface UserPersonDetails extends UserDetails, CredentialsContainer {
    String getUserId();

    String getName();

    String getFirstName();

    boolean isAdmin();

    String getAuthSource();
}
