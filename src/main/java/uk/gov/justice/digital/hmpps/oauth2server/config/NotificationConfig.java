package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientApi;

@Configuration
public class NotificationConfig {
    @Bean
    public NotificationClientApi notificationClient(@Value("${application.notify.key}") final String key) {
        return new NotificationClient(key);
    }
}
