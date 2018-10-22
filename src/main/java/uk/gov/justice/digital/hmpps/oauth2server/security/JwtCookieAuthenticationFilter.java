package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

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
        final Optional<String> jwt = jwtCookieHelper.readValueFromCookie(request);

        jwt.flatMap(jwtAuthenticationHelper::readAuthenticationFromJwt)
                .ifPresent(a -> SecurityContextHolder.getContext().setAuthentication(a));

        filterChain.doFilter(request, response);
    }
}
