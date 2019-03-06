package uk.gov.justice.digital.hmpps.oauth2server.verify;

import uk.gov.service.notify.NotificationClientException;

import java.util.Optional;

public interface ResetPasswordService extends PasswordService {
    Optional<String> requestResetPassword(String inputUsername, String url) throws NotificationClientException;

    void setPassword(String token, String password);
}
