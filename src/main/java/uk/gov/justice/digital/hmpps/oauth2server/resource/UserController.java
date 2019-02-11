package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class UserController {
    @GetMapping("/api/user/me")
    public Principal me(final Principal principal) {
        return principal;
    }

    @GetMapping("/api/user/me/roles")
    public Collection<Map<String, String>> myRoles(final Authentication authentication) {
        return authentication.getAuthorities().stream().
                map(a -> Map.of("roleCode", a.getAuthority().substring(5))). // remove ROLE_
                collect(Collectors.toList());
    }
}

