package uk.gov.justice.digital.hmpps.oauth2server.config;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.extensibility.TelemetryModule;
import com.microsoft.applicationinsights.web.extensibility.modules.WebTelemetryModule;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.security.KeyPair;

@Configuration
public class ClientTrackingTelemetryModule implements WebTelemetryModule, TelemetryModule {
    private final KeyPair keyPair;

    @Autowired
    public ClientTrackingTelemetryModule(@Value("${jwt.signing.key.pair}") final String privateKeyPair,
                                         @Value("${jwt.keystore.password}") final String keystorePassword,
                                         @Value("${jwt.keystore.alias:elite2api}") final String keystoreAlias) {

        final var keyStoreKeyFactory = new KeyStoreKeyFactory(new ByteArrayResource(Base64.decodeBase64(privateKeyPair)),
                keystorePassword.toCharArray());
        keyPair = keyStoreKeyFactory.getKeyPair(keystoreAlias);
    }

    @Override
    public void onBeginRequest(final ServletRequest req, final ServletResponse res) {

        HttpServletRequest httpServletRequest = (HttpServletRequest) req;
        final var token = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        final var bearer = "Bearer ";
        if (StringUtils.startsWithIgnoreCase(token, bearer)) {

            try {
                final var jwtBody = getClaimsFromJWT(token);

                final var properties = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getProperties();

                properties.put("user_name", String.valueOf(jwtBody.get("user_name")));
                properties.put("client_id", String.valueOf(jwtBody.get("client_id")));

            } catch ( ExpiredJwtException e){
                // Expired token which spring security will handle
            }

        }
    }

    private Claims getClaimsFromJWT(final String token) throws ExpiredJwtException {
        return Jwts.parser()
                .setSigningKey(keyPair.getPublic())
                .parseClaimsJws(token.substring(7))
                .getBody();
    }

    @Override
    public void onEndRequest(final ServletRequest req, final ServletResponse res) {
    }

    @Override
    public void initialize(final TelemetryConfiguration configuration) {

    }
}
