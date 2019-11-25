package uk.gov.justice.digital.hmpps.oauth2server.delius.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.UserDetails;
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.UserRole;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeliusService {

    private RestTemplate restTemplate;

    public DeliusService(@Qualifier("deliusApiRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<DeliusUserPersonDetails> findUser(String username) {
        UserDetails userDetails;
        try {
            userDetails = restTemplate.getForObject("/users/{username}/details", UserDetails.class, username);
        } catch (Exception exception) {
            return Optional.empty();
        }
        return Optional.of(mapUserDetailsToDeliusUser(userDetails, username));
    }

    public boolean authenticateUser(String username, String password) {
        ResponseEntity<String> response;
        try {
            response = restTemplate.getForEntity("/authenticate?username={username}&password={password}", String.class, username, password);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                return true;
            }
        } catch (Exception exception) {
            return false;
        }
        return false;
    }

    private DeliusUserPersonDetails mapUserDetailsToDeliusUser(UserDetails userDetails, String username) {
        DeliusUserPersonDetails deliusUserPersonDetails = new DeliusUserPersonDetails();
        deliusUserPersonDetails.setFirstName(userDetails.getFirstName());
        deliusUserPersonDetails.setSurname(userDetails.getSurname());
        deliusUserPersonDetails.setRoles(mapUserRolesToAuthorities(userDetails.getRoles()));
        deliusUserPersonDetails.setUsername(username);
        deliusUserPersonDetails.setLocked(userDetails.isLocked());
        return deliusUserPersonDetails;
    }

    public Collection<? extends GrantedAuthority> mapUserRolesToAuthorities(List<UserRole> userRoles) {
        return Optional.ofNullable(userRoles)
                .map(roles -> roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName())).collect(Collectors.toSet()))
                .orElseGet(Set::of);
    }

    public void changePassword(String username, String password) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var requestEntity = new HttpEntity<>(new LinkedMultiValueMap<>(Map.of("password", List.of(password))), headers);
        restTemplate.postForEntity("/users/{username}/password", requestEntity, Void.class, Map.of("username", username));
    }

    public void lockAccount(String username) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        var requestEntity = new HttpEntity<>(new LinkedMultiValueMap<>(), headers);
        restTemplate.postForEntity( "/users/{username}/lock", requestEntity, Void.class, Map.of("username", username));
    }

    public void unlockAccount(String username) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        var requestEntity = new HttpEntity<>(new LinkedMultiValueMap<>(), headers);
        restTemplate.postForEntity( "/users/{username}/unlock", requestEntity, Void.class, Map.of("username", username));
    }
}
