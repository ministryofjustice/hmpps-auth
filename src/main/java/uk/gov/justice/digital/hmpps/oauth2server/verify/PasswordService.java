package uk.gov.justice.digital.hmpps.oauth2server.verify;

import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;

public interface PasswordService {
    void setPassword(String token, String password);

    void changeAuthPassword(UserEmail userEmail, String password);
}
