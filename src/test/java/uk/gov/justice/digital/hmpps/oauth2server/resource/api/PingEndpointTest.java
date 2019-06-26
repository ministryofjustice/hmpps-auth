package uk.gov.justice.digital.hmpps.oauth2server.resource.api;

import org.junit.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.TEXT_PLAIN;

public class PingEndpointTest {
    @Test
    public void ping() {
        assertThat(new PingEndpoint().ping()).isEqualTo(
                ResponseEntity.ok().contentType(TEXT_PLAIN).body("pong"));
    }
}
