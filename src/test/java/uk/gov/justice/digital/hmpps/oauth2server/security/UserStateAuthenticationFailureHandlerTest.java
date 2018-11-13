package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.web.RedirectStrategy;
import uk.gov.justice.digital.hmpps.oauth2server.security.ApiAuthenticationProvider.MissingCredentialsException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class UserStateAuthenticationFailureHandlerTest {
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private RedirectStrategy redirectStrategy;

    private final UserStateAuthenticationFailureHandler handler = new UserStateAuthenticationFailureHandler();

    @Before
    public void setUp() {
        handler.setRedirectStrategy(redirectStrategy);
    }

    @Test
    public void onAuthenticationFailure_locked() throws IOException {
        handler.onAuthenticationFailure(request, response, new LockedException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/login?error&reason=locked");
    }

    @Test
    public void onAuthenticationFailure_expired() throws IOException {
        handler.onAuthenticationFailure(request, response, new AccountExpiredException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/login?error&reason=expired");
    }


    @Test
    public void onAuthenticationFailure_missing() throws IOException {
        handler.onAuthenticationFailure(request, response, new MissingCredentialsException());

        verify(redirectStrategy).sendRedirect(request, response, "/login?error&reason=missing");
    }

    @Test
    public void onAuthenticationFailure_other() throws IOException {
        handler.onAuthenticationFailure(request, response, new BadCredentialsException("msg"));

        verify(redirectStrategy).sendRedirect(request, response, "/login?error");
    }
}
