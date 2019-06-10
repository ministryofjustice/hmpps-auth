package uk.gov.justice.digital.hmpps.oauth2server.resource.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.PublicKey;
import java.util.Map;

@Api(tags = {"jwt-public-key"})
@RestController
public class PublicKeyController {
    private final PublicKey publicKey;

    @Autowired
    public PublicKeyController(@Value("${jwt.signing.key.pair}") final String privateKeyPair,
                               @Value("${jwt.keystore.password}") final String keystorePassword,
                               @Value("${jwt.keystore.alias:elite2api}") final String keystoreAlias) {

        final var keyStoreKeyFactory = new KeyStoreKeyFactory(new ByteArrayResource(Base64.decodeBase64(privateKeyPair)),
                keystorePassword.toCharArray());
        publicKey = keyStoreKeyFactory.getKeyPair(keystoreAlias).getPublic();
    }

    @ApiOperation(value = "Public JWT Key",
            notes = "formatted and base 64 encoded version",
            nickname = "getFormattedKey")
    @RequestMapping(value = "jwt-public-key", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = Map.class)})
    public Map<String, Object> getJwtPublicKey() {
        final var formattedKey = getFormattedKey(publicKey);
        return Map.of("formatted", convertNewLinesToArray(formattedKey), "encoded", Base64.encodeBase64String(formattedKey.getBytes()));
    }

    private String[] convertNewLinesToArray(final String formattedKey) {
        return formattedKey.split("\n");
    }

    private String getFormattedKey(final PublicKey pk) {
        final var builder = new StringBuilder();
        final var encodeBase64String = Base64.encodeBase64String(pk.getEncoded());
        builder.append("-----BEGIN PUBLIC KEY-----");
        builder.append("\n");
        for (var i = 0; i < encodeBase64String.length(); i += 64) {
            builder.append(encodeBase64String, i, Math.min(i + 64, encodeBase64String.length()));
            builder.append("\n");
        }
        builder.append("-----END PUBLIC KEY-----");
        builder.append("\n");
        return builder.toString();
    }
}
