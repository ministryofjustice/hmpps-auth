package uk.gov.justice.digital.hmpps.oauth2server.verify;

public interface PasswordService {
    void setPassword(String token, String password);
}
