package uk.gov.justice.digital.hmpps.oauth2server.resource.api;

import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwkSetRestController {

    private JWKSet jwkSet;

    public JwkSetRestController(@Autowired final JWKSet jwkSet) {
        this.jwkSet = jwkSet;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> keys() {
        return this.jwkSet.toJSONObject();
    }

}
