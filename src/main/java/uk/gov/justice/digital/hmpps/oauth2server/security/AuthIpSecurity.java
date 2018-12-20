package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

@Slf4j
@Component
public class AuthIpSecurity {

    private final Set<String> whitelist;

    public AuthIpSecurity(@Value("${application.authentication.ui.whitelist}") final Set<String> whitelist) {
        this.whitelist = whitelist;
    }

    public boolean check(HttpServletRequest request)
    {
        var remoteAddr = request.getRemoteAddr();
        var colonCount = remoteAddr.chars().filter(ch -> ch == ':').count();
        var remoteIp = colonCount == 1 ? StringUtils.split(remoteAddr, ":")[0] : remoteAddr;
        boolean matchIp = whitelist.stream().anyMatch(ip -> new IpAddressMatcher(ip).matches(remoteIp));
        if (!matchIp) {
            log.warn("Client IP {}, is not in whitelist {}", remoteIp, whitelist);
        }
        return matchIp;
    }

}
