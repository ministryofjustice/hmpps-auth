package uk.gov.justice.digital.hmpps.oauth2server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

@SpringBootApplication
@ConfigurationProperties
@EnableResourceServer
public class Oauth2serverApplication {

    public static void main(String[] args) {
        SpringApplication.run(Oauth2serverApplication.class, args);
    }
}
