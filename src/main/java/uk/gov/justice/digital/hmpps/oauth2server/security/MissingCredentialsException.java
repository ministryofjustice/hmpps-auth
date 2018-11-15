package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.security.core.AuthenticationException;

class MissingCredentialsException extends AuthenticationException {
    MissingCredentialsException() {
        super("No credentials provided");
    }
}
