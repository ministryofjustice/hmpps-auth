package uk.gov.justice.digital.hmpps.oauth2server.config;

import com.microsoft.applicationinsights.TelemetryClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import uk.gov.justice.digital.hmpps.oauth2server.security.BasicSha1PasswordEncoder;
import uk.gov.justice.digital.hmpps.oauth2server.security.LockingAuthenticationProvider;
import uk.gov.justice.digital.hmpps.oauth2server.security.OracleSha1PasswordEncoder;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserRetriesService;

@Configuration
public class AuthenticationProviderConfig {

    @Bean
    public DaoAuthenticationProvider nomisLockingAuthenticationProvider(final UserDetailsService nomisUserDetailsService,
                                                                        final UserRetriesService nomisUserRetriesService,
                                                                        final TelemetryClient telemetryClient,
                                                                        @Value("${application.authentication.lockout-count}") final int accountLockoutCount) {
        return new LockingAuthenticationProvider(nomisUserDetailsService, nomisUserRetriesService, telemetryClient, accountLockoutCount, new OracleSha1PasswordEncoder());
    }

    @Bean
    public DaoAuthenticationProvider oasysLockingAuthenticationProvider(final UserDetailsService oasysUserDetailsService,
                                                                        final UserRetriesService oasysUserRetriesService,
                                                                        final TelemetryClient telemetryClient,
                                                                        @Value("${application.authentication.lockout-count}") final int accountLockoutCount) {
        return new LockingAuthenticationProvider(oasysUserDetailsService, oasysUserRetriesService, telemetryClient, accountLockoutCount, new BasicSha1PasswordEncoder());
    }

}
