package uk.gov.justice.digital.hmpps.oauth2server.verify;

import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordServiceImpl.NotificationClientRuntimeException;

import java.util.Optional;

public interface ResetPasswordService extends PasswordService {
    Optional<String> requestResetPassword(String inputUsername, String url) throws NotificationClientRuntimeException;

    void setPassword(String token, String password);
}
