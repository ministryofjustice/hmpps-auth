package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;


@Configuration
public class RestTemplateConfiguration {

    private final String deliusEndpointUrl;
    private final Duration healthTimeout;

    public RestTemplateConfiguration(@Value("${delius.endpoint.url}") @URL final String deliusEndpointUrl,
                                     @Value("${delius.health.timeout:1s") final Duration healthTimeout) {
        this.deliusEndpointUrl = deliusEndpointUrl;
        this.healthTimeout = healthTimeout;
    }

    @Bean(name = "deliusApiRestTemplate")
    public RestTemplate deliusApiRestTemplate(final RestTemplateBuilder restTemplateBuilder) {
        return getRestTemplate(restTemplateBuilder, deliusEndpointUrl);
    }

    @Bean(name = "deliusApiHealthRestTemplate")
    public RestTemplate deliusApiHealthRestTemplate(final RestTemplateBuilder restTemplateBuilder) {
        return getHealthRestTemplate(restTemplateBuilder, deliusEndpointUrl);
    }

    private RestTemplate getRestTemplate(final RestTemplateBuilder restTemplateBuilder, final String uri) {
        return restTemplateBuilder
                .rootUri(uri)
                .build();
    }

    private RestTemplate getHealthRestTemplate(final RestTemplateBuilder restTemplateBuilder, final String uri) {
        return restTemplateBuilder
                .rootUri(uri)
                .setConnectTimeout(healthTimeout)
                .setReadTimeout(healthTimeout)
                .build();
    }

}
