package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.gov.justice.digital.hmpps.oauth2server.security.OracleSha1PasswordEncoder;

import java.util.Map;

@Configuration
public class PasswordConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        final var bCryptPasswordEncoder = new BCryptPasswordEncoder();
        final var encoders = Map.of("bcrypt", bCryptPasswordEncoder, "oracle", new OracleSha1PasswordEncoder());
        final var delegatingPasswordEncoder = new DelegatingPasswordEncoder("bcrypt", encoders);
        delegatingPasswordEncoder.setDefaultPasswordEncoderForMatches(bCryptPasswordEncoder);
        return delegatingPasswordEncoder;
    }
}
