package uk.gov.justice.digital.hmpps.oauth2server.delius.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails;
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.UserDetails;
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.UserRole;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
public class DeliusUserService {

    private final RestTemplate restTemplate;

    public DeliusUserService(@Qualifier("deliusApiRestTemplate") final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<DeliusUserPersonDetails> getDeliusUserByUsername(final String username) {
        try {
            final var userDetails = (UserDetails) null;
//            restTemplate.getForObject("/users/{username}/details", UserDetails.class, username);
            return Optional.ofNullable(userDetails).map(u -> mapUserDetailsToDeliusUser(u, username));
        } catch (final HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.debug("User not found in delius due to {}", e.getMessage());
            } else {
                log.warn("Unable to get delius user details for user {}", username, e);
            }
            return Optional.empty();
        } catch (final Exception e) {
            log.warn("Unable to get delius user details for user {}", username, e);
            return Optional.empty();
        }
    }

    public boolean authenticateUser(final String username, final String password) {
        try {
//            restTemplate.postForEntity("/authenticate", new AuthUser(username, password), String.class);
            return false;
        } catch (final HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.debug("User not authorised in delius due to {}", e.getMessage());
            } else {
                log.warn("Unable to authenticate user {}", username, e);
            }
            return false;
        } catch (final Exception e) {
            log.warn("Unable to authenticate user for user {}", username, e);
            return false;
        }
    }

    private DeliusUserPersonDetails mapUserDetailsToDeliusUser(final UserDetails userDetails, final String username) {
        return DeliusUserPersonDetails.builder()
                .firstName(userDetails.getFirstName())
                .surname(userDetails.getSurname())
                // TODO: Sort out removal of roles in community api
                .roles(mapUserRolesToAuthorities(Collections.emptyList()))
                .username(username)
                .locked(userDetails.isLocked()).build();
    }

    private Collection<? extends GrantedAuthority> mapUserRolesToAuthorities(final List<UserRole> userRoles) {
        return Optional.ofNullable(userRoles)
                .map(roles -> roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName())).collect(Collectors.toSet()))
                .orElseGet(Set::of);
    }

//    public void changePassword(final String username, final String password) {
//        final var headers = new HttpHeaders();
//        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//
//        final var requestEntity = new HttpEntity<>(new LinkedMultiValueMap<>(Map.of("password", List.of(password))), headers);
//        restTemplate.postForEntity("/users/{username}/password", requestEntity, Void.class, Map.of("username", username));
//    }
//
//    public void lockAccount(final String username) {
//        final var headers = new HttpHeaders();
//        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//        final var requestEntity = new HttpEntity<>(new LinkedMultiValueMap<>(), headers);
//        restTemplate.postForEntity("/users/{username}/lock", requestEntity, Void.class, Map.of("username", username));
//    }
//
//    public void unlockAccount(final String username) {
//        final var headers = new HttpHeaders();
//        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//        final var requestEntity = new HttpEntity<>(new LinkedMultiValueMap<>(), headers);
//        restTemplate.postForEntity("/users/{username}/unlock", requestEntity, Void.class, Map.of("username", username));
//    }

    @Getter
    @AllArgsConstructor
    private static class AuthUser {
        private final String username;
        private final String password;
    }
}
