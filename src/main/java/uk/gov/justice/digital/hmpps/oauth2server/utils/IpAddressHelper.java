package uk.gov.justice.digital.hmpps.oauth2server.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Azure provides ip addresses with a port, which we need to strip out before using.  Don't want to nobble IP6 addresses
 * either though, so need to count to see how many colons are in the remote address first.
 */
public abstract class IpAddressHelper {
    public static String retrieveIpFromRemoteAddr(final HttpServletRequest request) {
        final var remoteAddr = request.getRemoteAddr();
        final var colonCount = remoteAddr.chars().filter(ch -> ch == ':').count();
        return colonCount == 1 ? StringUtils.split(remoteAddr, ":")[0] : remoteAddr;
    }

    /**
     * Used by {@link uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordServiceImpl} for throttling
     */
    @SuppressWarnings("WeakerAccess")
    public static String retrieveIpFromRequest() {
        final var requestAttributes = RequestContextHolder.currentRequestAttributes();
        return retrieveIpFromRemoteAddr(((ServletRequestAttributes) requestAttributes).getRequest());
    }
}
