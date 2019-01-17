package uk.gov.justice.digital.hmpps.oauth2server.security;

public interface ChangePasswordService {
    void changePassword(String username, String password);

    void changePasswordWithUnlock(String username, String password);
}
