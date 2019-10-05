package uk.gov.justice.digital.hmpps.oauth2server.verify;

import org.springframework.security.crypto.password.PasswordEncoder;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.security.ReusedPasswordException;

import java.time.LocalDateTime;

public abstract class PasswordServiceImpl implements PasswordService {
    private final PasswordEncoder passwordEncoder;
    private final long passwordAge;

    public PasswordServiceImpl(final PasswordEncoder passwordEncoder, final long passwordAge) {
        this.passwordEncoder = passwordEncoder;
        this.passwordAge = passwordAge;
    }

    @Override
    public void changeAuthPassword(final User user, final String password) {
        // check user not setting password to existing password
        if (passwordEncoder.matches(password, user.getPassword())) {
            throw new ReusedPasswordException();
        }

        user.setPassword(passwordEncoder.encode(password));
        user.setPasswordExpiry(LocalDateTime.now().plusDays(passwordAge));
    }
}
