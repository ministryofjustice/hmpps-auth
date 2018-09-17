package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class UserController {
    @GetMapping("/api/user/me")
    public Principal user(Principal principal) {
        return principal;
    }
}

