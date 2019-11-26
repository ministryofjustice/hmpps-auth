package uk.gov.justice.digital.hmpps.oauth2server.security;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusService;

@Slf4j
@Component
public class DeliusLdapAuthenticationProvider extends LockingAuthenticationProvider {

    private final DeliusService deliusService;

    public DeliusLdapAuthenticationProvider(final UserDetailsService userDetailsService,
                                            final UserRetriesService userRetriesService,
                                            final TelemetryClient telemetryClient,
                                            @Value("${application.authentication.lockout-count}") final int accountLockoutCount, final DeliusService deliusService) {
        super(userDetailsService, userRetriesService, telemetryClient, accountLockoutCount);
        this.deliusService = deliusService;
    }

    protected boolean checkPassword(final UserDetails userDetails, final String password) {
        if (userDetails instanceof User) {
            return super.checkPassword(userDetails, password);
        }
        return deliusService.authenticateUser(userDetails.getUsername(), password);
    }
}
