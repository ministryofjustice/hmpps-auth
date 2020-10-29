package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.security.core.AuthenticationException;

public class ReusedPasswordException extends AuthenticationException {
    public ReusedPasswordException() {
        super("Password cannot be reused");
    }
}
