package uk.gov.justice.digital.hmpps.oauth2server.verify;

import org.springframework.security.crypto.password.PasswordEncoder;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.security.ReusedPasswordException;

import java.time.LocalDateTime;

public abstract class PasswordService {
    private final PasswordEncoder passwordEncoder;
    private final long passwordAge;

    public PasswordService(final PasswordEncoder passwordEncoder, final long passwordAge) {
        this.passwordEncoder = passwordEncoder;
        this.passwordAge = passwordAge;
    }

    public abstract void setPassword(String token, String password);

    protected void changeAuthPassword(final UserEmail userEmail, final String password) {
        // check user not setting password to existing password
        if (passwordEncoder.matches(password, userEmail.getPassword())) {
            throw new ReusedPasswordException();
        }

        userEmail.setPassword(passwordEncoder.encode(password));
        userEmail.setPasswordExpiry(LocalDateTime.now().plusDays(passwordAge));
    }
}
