package uk.gov.justice.digital.hmpps.oauth2server.security;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Configuration
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {
    private final JwtCookieHelper jwtCookieHelper;
    private final JwtAuthenticationHelper jwtAuthenticationHelper;

    public JwtCookieAuthenticationFilter(final JwtCookieHelper jwtCookieHelper,
                                         final JwtAuthenticationHelper jwtAuthenticationHelper) {
        this.jwtCookieHelper = jwtCookieHelper;
        this.jwtAuthenticationHelper = jwtAuthenticationHelper;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        final var jwt = jwtCookieHelper.readValueFromCookie(request);

        try {
            jwt.flatMap(jwtAuthenticationHelper::readAuthenticationFromJwt)
                    .ifPresent(a -> SecurityContextHolder.getContext().setAuthentication(a));
        } catch (final JwtException e) {
            log.info("Unable to read authentication from JWT", e);
        } catch (final Exception e) {
            // filter errors don't get logged by spring boot, so log here
            log.error("Failed to read authentication due to {}", e.getClass().getName(), e);
            throw e;
        }

        filterChain.doFilter(request, response);
    }
}
