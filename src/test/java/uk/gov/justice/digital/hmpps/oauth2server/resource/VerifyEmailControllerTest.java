package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler;
import uk.gov.justice.digital.hmpps.oauth2server.verify.ReferenceCodesService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService;
import uk.gov.service.notify.NotificationClientException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VerifyEmailControllerTest {
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler;
    @Mock
    private VerifyEmailService verifyEmailService;
    @Mock
    private ReferenceCodesService referenceCodesService;

    private VerifyEmailController verifyEmailController;
    private final UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken("user", "pass");

    @Before
    public void setUp() {
        verifyEmailController = new VerifyEmailController(jwtAuthenticationSuccessHandler, verifyEmailService, referenceCodesService, true);
    }

    @Test
    public void verifyEmailRequest() throws IOException, ServletException {
        final var emails = Collections.singletonList("bob");
        when(verifyEmailService.getExistingEmailAddresses(anyString())).thenReturn(emails);
        final var modelAndView = verifyEmailController.verifyEmailRequest(principal, request, response);
        assertThat(modelAndView.getViewName()).isEqualTo("verifyEmail");
        assertThat(modelAndView.getModel()).containsExactly(MapEntry.entry("candidates", emails));
    }

    @Test
    public void verifyEmailRequest_existingUserEmail() throws IOException, ServletException {
        final var userEmail = new UserEmail("bob");
        userEmail.setEmail("email");
        when(verifyEmailService.getEmail(anyString())).thenReturn(Optional.of(userEmail));
        final var modelAndView = verifyEmailController.verifyEmailRequest(principal, request, response);
        assertThat(modelAndView.getViewName()).isEqualTo("verifyEmail");
        assertThat(modelAndView.getModel()).containsExactly(MapEntry.entry("email", userEmail.getEmail()));
    }

    @Test
    public void verifyEmailRequest_existingUserEmailVerified() throws IOException, ServletException {
        final var userEmail = new UserEmail("bob");
        userEmail.setVerified(true);
        when(verifyEmailService.getEmail(anyString())).thenReturn(Optional.of(userEmail));
        SecurityContextHolder.getContext().setAuthentication(principal);
        final var modelAndView = verifyEmailController.verifyEmailRequest(principal, request, response);
        assertThat(modelAndView).isNull();
        verify(jwtAuthenticationSuccessHandler).proceed(request, response, principal);
    }

    @Test
    public void verifyEmailContinue() throws IOException, ServletException {
        SecurityContextHolder.getContext().setAuthentication(principal);
        verifyEmailController.verifyEmailContinue(request, response);
        verify(jwtAuthenticationSuccessHandler).proceed(request, response, principal);
    }

    @Test
    public void verifyEmail() {
        verifyEmailFailure(null, "format");
    }

    @Test
    public void verifyEmail_NoAtSign() {
        verifyEmailFailure("a", "format");
    }

    @Test
    public void verifyEmail_MultipleAtSigns() {
        verifyEmailFailure("a@b.@.com", "format");
    }

    @Test
    public void verifyEmail_NoExtension() {
        verifyEmailFailure("a@bee", "format");
    }

    @Test
    public void verifyEmail_InvalidDomain() {
        verifyEmailFailure("a@b.com", "domain");
        verify(referenceCodesService).isValidEmailDomain("b.com");
    }

    @Test
    public void verifyEmail_InvalidCharacters() {
        verifyEmailFailure("a@b.&com", "characters");
    }

    @Test
    public void verifyEmail_Exception() throws NotificationClientException {
        when(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(Boolean.TRUE);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url"));
        when(verifyEmailService.requestVerification(anyString(), anyString(), anyString())).thenThrow(new NotificationClientException("something went wrong"));
        final var modelAndView = verifyEmailController.verifyEmail("a@b.com", principal, request);
        assertThat(modelAndView.getViewName()).isEqualTo("verifyEmail");
        assertThat(modelAndView.getModel()).containsExactly(MapEntry.entry("email", "a@b.com"), MapEntry.entry("error", "unknownerror"));
    }

    @Test
    public void verifyEmail_Success() throws NotificationClientException {
        when(referenceCodesService.isValidEmailDomain(anyString())).thenReturn(Boolean.TRUE);
        when(verifyEmailService.requestVerification(anyString(), anyString(), anyString())).thenReturn("link");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.url"));
        final var email = "o'there@b-c.d";

        final var modelAndView = verifyEmailController.verifyEmail(email, principal, request);

        assertThat(modelAndView.getViewName()).isEqualTo("verifyEmailSent");
        assertThat(modelAndView.getModel()).containsExactly(MapEntry.entry("verifyLink", "link"), MapEntry.entry("email", email));
        verify(verifyEmailService).requestVerification("user", email, "http://some.url-confirm");
    }

    private void verifyEmailFailure(final String email, final String domain) {
        final var modelAndView = verifyEmailController.verifyEmail(email, principal, request);
        assertThat(modelAndView.getViewName()).isEqualTo("verifyEmail");
        assertThat(modelAndView.getModel()).containsExactly(MapEntry.entry("email", email), MapEntry.entry("error", domain));
    }

    @Test
    public void verifyEmailConfirm() {
        when(verifyEmailService.confirmEmail(anyString())).thenReturn(Optional.empty());
        final var modelAndView = verifyEmailController.verifyEmailConfirm("token");
        assertThat(modelAndView.getViewName()).isEqualTo("verifyEmailSuccess");
        assertThat(modelAndView.getModel()).isEmpty();
    }

    @Test
    public void verifyEmailConfirm_Failure() {
        when(verifyEmailService.confirmEmail(anyString())).thenReturn(Optional.of("failed"));
        final var modelAndView = verifyEmailController.verifyEmailConfirm("token");
        assertThat(modelAndView.getViewName()).isEqualTo("verifyEmailFailure");
        assertThat(modelAndView.getModel()).containsExactly(MapEntry.entry("error", "failed"));
    }
}
