package uk.gov.justice.digital.hmpps.oauth2server.security;

public interface AlterUserService {

    void changePassword(String username, String password);

    void changePasswordWithUnlock(String username, String password);

    void lockAccount(String username);
}
