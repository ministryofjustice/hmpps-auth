package uk.gov.justice.digital.hmpps.oauth2server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "saved-request.cookie")
@Data
public class SavedRequestCookieConfigurationProperties {
    private String name;
    private Duration expiryTime;
}
