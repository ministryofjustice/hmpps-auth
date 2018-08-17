package uk.gov.justice.digital.hmpps.oauth2server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

@Component
@EndpointWebExtension(endpoint = HealthEndpoint.class)
public class HealthEndpointWebExtension {

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @ReadOperation
    public WebEndpointResponse<Health> health() {
        Health health = Health.up().withDetail("version", getVersion()).build();
        return new WebEndpointResponse<>(health, 200);
    }

    /**
     * @return health data. Note this is unsecured so no sensitive data allowed!
     */
    private String getVersion(){
        return buildProperties == null ? "version not available" : buildProperties.getVersion();
    }
}
