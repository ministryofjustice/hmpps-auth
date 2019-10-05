package uk.gov.justice.digital.hmpps.oauth2server.verify;

import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;

public interface PasswordService {
    void setPassword(String token, String password);

    void changeAuthPassword(User user, String password);
}
