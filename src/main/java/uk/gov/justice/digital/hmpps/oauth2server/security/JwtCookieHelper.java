package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.justice.digital.hmpps.oauth2server.utils.CookieHelper;

@Component
public class JwtCookieHelper extends CookieHelper {
    @Autowired
    public JwtCookieHelper(final JwtCookieConfigurationProperties properties) {
        super(properties.getName(), properties.getExpiryTime(), properties.isSecure());
    }
}
