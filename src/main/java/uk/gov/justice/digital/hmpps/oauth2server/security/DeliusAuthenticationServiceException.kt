package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.security.authentication.InternalAuthenticationServiceException;

public class DeliusAuthenticationServiceException extends InternalAuthenticationServiceException {
    public DeliusAuthenticationServiceException() {
        super("Delius is currently not responding");
    }
}
