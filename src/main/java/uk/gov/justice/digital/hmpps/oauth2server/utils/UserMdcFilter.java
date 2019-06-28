package uk.gov.justice.digital.hmpps.oauth2server.utils;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static uk.gov.justice.digital.hmpps.oauth2server.utils.MdcUtility.USER_ID_HEADER;

@Slf4j
@Component
@Order(1)
public class UserMdcFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(@NonNull final HttpServletRequest request, @NonNull final HttpServletResponse response,
                                    @NonNull final FilterChain filterChain) throws ServletException, IOException {
        final var currentUsername = getUser(request);

        try {
            if (currentUsername != null) {
                MDC.put(USER_ID_HEADER, currentUsername);
            }
            filterChain.doFilter(request, response);
        } finally {
            if (currentUsername != null) {
                MDC.remove(USER_ID_HEADER);
            }
        }
    }

    private String getUser(final HttpServletRequest req) {
        return req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : null;
    }
}
