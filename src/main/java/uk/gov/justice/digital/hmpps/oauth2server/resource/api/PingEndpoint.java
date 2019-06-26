package uk.gov.justice.digital.hmpps.oauth2server.resource.api;

import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.springframework.http.MediaType.*;

@Configuration
@WebEndpoint(id = "ping")
public class PingEndpoint {

    @ReadOperation(produces = "plain/text")
    public String ping() {
        return "pong";
    }
}
