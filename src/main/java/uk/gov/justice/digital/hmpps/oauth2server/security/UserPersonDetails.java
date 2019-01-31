package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.security.core.userdetails.UserDetails;

public interface UserPersonDetails extends UserDetails {
    String getName();

    String getFirstName();
}
