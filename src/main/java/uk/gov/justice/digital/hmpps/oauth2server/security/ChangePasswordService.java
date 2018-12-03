package uk.gov.justice.digital.hmpps.oauth2server.security;

public interface ChangePasswordService {
    void changePassword(final String username, final String password);

}
