package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import uk.gov.justice.digital.hmpps.oauth2server.utils.IpAddressHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

@Slf4j
@Component
public class AuthIpSecurity {

    private final Set<String> whitelist;

    public AuthIpSecurity(@Value("${application.authentication.ui.whitelist}") final Set<String> whitelist) {
        this.whitelist = whitelist;
    }

    public boolean check(final HttpServletRequest request) {
        final var remoteIp = IpAddressHelper.retrieveIpFromRemoteAddr(request);
        final var matchIp = whitelist.stream().anyMatch(ip -> new IpAddressMatcher(ip).matches(remoteIp));
        if (!matchIp) {
            log.warn("Client IP {}, is not in whitelist {}", remoteIp, whitelist);
        }
        return matchIp;
    }

}
