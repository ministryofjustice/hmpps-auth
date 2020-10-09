package uk.gov.justice.digital.hmpps.oauth2server.utils;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static uk.gov.justice.digital.hmpps.oauth2server.utils.MdcUtility.REQUEST_DURATION;
import static uk.gov.justice.digital.hmpps.oauth2server.utils.MdcUtility.RESPONSE_STATUS;
import static uk.gov.justice.digital.hmpps.oauth2server.utils.MdcUtility.SKIP_LOGGING;
import static uk.gov.justice.digital.hmpps.oauth2server.utils.MdcUtility.isLoggingAllowed;


@Component
@Slf4j
@Order(2)
public class RequestLogFilter extends OncePerRequestFilter {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS");

    private final Pattern excludeUriRegex;

    @Autowired
    public RequestLogFilter(@Value("${logging.uris.exclude.regex}") final String excludeUris) {
        excludeUriRegex = Pattern.compile(excludeUris);
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, @NonNull final HttpServletResponse response, @NonNull final FilterChain filterChain)
            throws ServletException, IOException {

        if (excludeUriRegex.matcher(request.getRequestURI()).matches()) {
            MDC.put(SKIP_LOGGING, "true");
        }

        try {
            final var start = LocalDateTime.now();
            if (log.isTraceEnabled() && isLoggingAllowed()) {
                log.trace("Request: {} {}", request.getMethod(), request.getRequestURI());
            }

            filterChain.doFilter(request, response);

            final var duration = Duration.between(start, LocalDateTime.now()).toMillis();
            MDC.put(REQUEST_DURATION, String.valueOf(duration));
            final var status = response.getStatus();
            MDC.put(RESPONSE_STATUS, String.valueOf(status));
            if (log.isTraceEnabled() && isLoggingAllowed()) {
                log.trace("Response: {} {} - Status {} - Start {}, Duration {} ms", request.getMethod(), request.getRequestURI(), status, start.format(formatter), duration);
            }
        } finally {
            MDC.remove(REQUEST_DURATION);
            MDC.remove(RESPONSE_STATUS);
            MDC.remove(SKIP_LOGGING);
        }
    }
}
