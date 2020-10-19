package uk.gov.justice.digital.hmpps.oauth2server.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.digital.hmpps.oauth2server.config.CookieRequestCache;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    private final JwtCookieHelper jwtCookieHelper;
    private final JwtAuthenticationHelper jwtAuthenticationHelper;
    private final VerifyEmailService verifyEmailService;
    private final RestTemplate restTemplate;
    private final boolean tokenVerificationEnabled;

    @Autowired
    public JwtAuthenticationSuccessHandler(final JwtCookieHelper jwtCookieHelper,
                                           final JwtAuthenticationHelper jwtAuthenticationHelper,
                                           final CookieRequestCache cookieRequestCache,
                                           final VerifyEmailService verifyEmailService,
                                           @Qualifier("tokenVerificationApiRestTemplate") final RestTemplate restTemplate,
                                           @Value("${tokenverification.enabled:false}") final boolean tokenVerificationEnabled) {
        this.jwtCookieHelper = jwtCookieHelper;
        this.jwtAuthenticationHelper = jwtAuthenticationHelper;
        this.verifyEmailService = verifyEmailService;
        this.restTemplate = restTemplate;
        this.tokenVerificationEnabled = tokenVerificationEnabled;
        setRequestCache(cookieRequestCache);
        setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response,
                                        final Authentication authentication) throws IOException, ServletException {

        addAuthenticationToRequest(request, response, authentication);

           // we have successfully authenticated and added the cookie.  Now need to check that they have a validated email address
       if (authentication.getPrincipal() instanceof UserPersonDetails) {
           final var userDetails = (UserPersonDetails) authentication.getPrincipal();

           if (verifyEmailService.isNotVerified(authentication.getName(), AuthSource.fromNullableString(userDetails.getAuthSource()))) {
               getRedirectStrategy().sendRedirect(request, response, "/verify-email");
               return;
           }
       }
        proceed(request, response, authentication);
    }

    private void addAuthenticationToRequest(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) {
        final var optionalAuth = jwtCookieHelper.readValueFromCookie(request).flatMap(jwtAuthenticationHelper::readUserDetailsFromJwt);

        optionalAuth.ifPresent(udi -> {
            log.info("Found existing cookie for user {} with jwt of {}", udi.getUsername(), udi.getJwtId());
            if (tokenVerificationEnabled) restTemplate.delete("/token?authJwtId={authJwtId}", udi.getJwtId());
        });

        final var jwt = jwtAuthenticationHelper.createJwt(authentication);
        jwtCookieHelper.addCookieToResponse(request, response, jwt);
    }

    public void updateAuthenticationInRequest(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) {
        final var optionalAuth = jwtCookieHelper.readValueFromCookie(request).flatMap(jwtAuthenticationHelper::readUserDetailsFromJwt);

        final var jwt = optionalAuth.map(udi -> {
            // need to keep the same jwt id as all client sessions are linked to this value and changing it will
            // then effectively mean a new login from the token verification point of view
            return jwtAuthenticationHelper.createJwtWithId(authentication, udi.getJwtId());
        }).orElseGet(() -> jwtAuthenticationHelper.createJwt(authentication));

        jwtCookieHelper.addCookieToResponse(request, response, jwt);
    }

    public void proceed(final HttpServletRequest request, final HttpServletResponse response,
                        final Authentication authentication) throws ServletException, IOException {
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
