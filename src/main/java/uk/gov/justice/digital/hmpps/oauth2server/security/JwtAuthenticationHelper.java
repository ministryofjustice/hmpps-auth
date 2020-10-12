package uk.gov.justice.digital.hmpps.oauth2server.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("deprecation")
@Component
@Slf4j
public class JwtAuthenticationHelper {
    private final KeyPair keyPair;
    private final Duration expiryTime;

    @Autowired
    public JwtAuthenticationHelper(@Value("${jwt.signing.key.pair}") final String privateKeyPair,
                                   @Value("${jwt.keystore.password}") final String keystorePassword,
                                   @Value("${jwt.keystore.alias:elite2api}") final String keystoreAlias,
                                   final JwtCookieConfigurationProperties properties) {

        final var keyStoreKeyFactory = new KeyStoreKeyFactory(new ByteArrayResource(Base64.decodeBase64(privateKeyPair)),
                keystorePassword.toCharArray());
        keyPair = keyStoreKeyFactory.getKeyPair(keystoreAlias);
        expiryTime = properties.getExpiryTime();
    }

    String createJwt(final Authentication authentication) {
        return authentication instanceof OAuth2AuthenticationToken ?
                createJwtWithIdFromOidcAuthentication((OAuth2AuthenticationToken) authentication, UUID.randomUUID().toString())
                : createJwtWithId(authentication, UUID.randomUUID().toString());
    }

    String createJwtWithIdFromOidcAuthentication(final OAuth2AuthenticationToken authentication, final String jwtId) {
        final var userDetails = (DefaultOidcUser) authentication.getPrincipal();
        final var username = userDetails.getName().toUpperCase();
        final var authorities = Optional.ofNullable(authentication.getAuthorities()).orElse(Collections.emptyList());
        final var authoritiesAsString = authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
        return Jwts.builder()
                .setId(jwtId)
                .setSubject(username)
                .addClaims(Map.of("authorities", authoritiesAsString, "name", userDetails.getFullName(), "auth_source", AuthSource.azuread.getSource(), "user_id", username))
                .setExpiration(new Date(System.currentTimeMillis() + expiryTime.toMillis()))
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())
                .compact();
    }

    String createJwtWithId(final Authentication authentication, final String jwtId) {
        final var userDetails = (UserPersonDetails) authentication.getPrincipal();
        final var username = userDetails.getUsername();
        log.debug("Creating jwt cookie for user {}", username);
        final var authorities = Optional.ofNullable(authentication.getAuthorities()).orElse(Collections.emptyList());
        final var authoritiesAsString = authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
        return Jwts.builder()
                .setId(jwtId)
                .setSubject(username)
                .addClaims(Map.of("authorities", authoritiesAsString, "name", userDetails.getName(), "auth_source", userDetails.getAuthSource(), "user_id", userDetails.getUserId()))
                .setExpiration(new Date(System.currentTimeMillis() + expiryTime.toMillis()))
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())
                .compact();
    }

    /**
     * This method takes a user JWT post authentication and uses it to create a token that is then sent to the client
     *
     * @param jwt String
     * @return token for client
     */
    public Optional<UsernamePasswordAuthenticationToken> readAuthenticationFromJwt(final String jwt) {
        return readUserDetailsFromJwt(jwt).map(u -> new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
    }

    public Optional<UserDetailsImpl> readUserDetailsFromJwt(final String jwt) {
        try {
            final var body = Jwts.parser()
                    .setSigningKey(keyPair.getPublic())
                    .parseClaimsJws(jwt)
                    .getBody();
            final var username = body.getSubject();
            final var authoritiesString = body.get("authorities", String.class);
            final var name = Optional.ofNullable(body.get("name", String.class)).orElse(username);
            final var userId = Optional.ofNullable(body.get("user_id", String.class)).orElse(username);
            final Collection<GrantedAuthority> authorities = Stream.of(authoritiesString.split(","))
                    .filter(StringUtils::isNotEmpty)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            final var authSource = Optional.ofNullable(body.get("auth_source", String.class)).orElse("none");

            log.debug("Set authentication for {} with jwt id of {}", username, body.getId());
            return Optional.of(new UserDetailsImpl(username, name, authorities, authSource, userId, body.getId()));
        } catch (final ExpiredJwtException eje) {
            // cookie set to expire at same time as JWT so unlikely really get an expired one
            log.info("Expired JWT found for user {}", eje.getClaims().getSubject());
            return Optional.empty();
        }
    }
}
