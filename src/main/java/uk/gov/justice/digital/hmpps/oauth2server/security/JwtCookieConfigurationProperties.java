package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "jwt.cookie")
@Data
public class JwtCookieConfigurationProperties {
    private String name;
    private Duration expiryTime;
}
