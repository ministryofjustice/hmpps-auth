package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.security.core.AuthenticationException;

public class PasswordValidationFailureException extends AuthenticationException {
    public PasswordValidationFailureException() {
        super("Password cannot be reused");
    }
}
