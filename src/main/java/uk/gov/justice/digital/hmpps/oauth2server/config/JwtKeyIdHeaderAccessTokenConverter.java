package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.util.JsonParser;
import org.springframework.security.oauth2.common.util.JsonParserFactory;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;

public class JwtKeyIdHeaderAccessTokenConverter extends JwtAccessTokenConverter {

    private JsonParser jsonParser = JsonParserFactory.create();
    private final RsaSigner signer;
    private final String keyId;

    JwtKeyIdHeaderAccessTokenConverter(final String keyId, final KeyPair keyPair) {
        super.setKeyPair(keyPair);
        this.signer = new RsaSigner((RSAPrivateKey) keyPair.getPrivate());
        this.keyId = keyId;
    }

    @Override
    protected String encode(final OAuth2AccessToken accessToken, final OAuth2Authentication authentication) {
        accessToken.getAdditionalInformation().put("iss", issuer());
        final String content;
        try {
            content = this.jsonParser.formatMap(getAccessTokenConverter().convertAccessToken(accessToken, authentication));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot convert access token to JSON", ex);
        }
        return JwtHelper.encode(content, this.signer).getEncoded();
    }

    private String issuer() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() + "/issuer";
    }

}
