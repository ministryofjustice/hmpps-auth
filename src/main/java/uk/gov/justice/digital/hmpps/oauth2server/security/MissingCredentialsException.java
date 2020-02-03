package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.security.authentication.AccountStatusException;

class MissingCredentialsException extends AccountStatusException {
    MissingCredentialsException() {
        super("No credentials provided");
    }
}
