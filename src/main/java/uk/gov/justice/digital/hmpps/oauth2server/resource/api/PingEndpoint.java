package uk.gov.justice.digital.hmpps.oauth2server.resource.api;

import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.springframework.http.MediaType.*;

@Configuration
@WebEndpoint(id = "ping")
public class PingEndpoint {
    @ReadOperation
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok().contentType(TEXT_PLAIN).body("pong");
    }
}
