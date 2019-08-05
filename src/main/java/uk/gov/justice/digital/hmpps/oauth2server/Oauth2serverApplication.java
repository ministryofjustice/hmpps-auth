package uk.gov.justice.digital.hmpps.oauth2server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationProperties
@EnableScheduling
public class Oauth2serverApplication {

    public static void main(final String[] args) {
        SpringApplication.run(Oauth2serverApplication.class, args);
    }
}
