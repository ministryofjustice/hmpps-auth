package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.ResetPasswordService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.Set;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InitialPasswordControllerTest {
    @Mock
    private ResetPasswordService resetPasswordService;
    @Mock
    private TokenService tokenService;
    @Mock
    private UserService userService;
    @Mock
    private TelemetryClient telemetryClient;
    @Mock
    private HttpServletRequest request;

    private InitialPasswordController controller;

    @Before
    public void setUp() {
        controller = new InitialPasswordController(resetPasswordService, tokenService, userService, telemetryClient, Set.of("password1"), "http://hdcurl");
    }

    @Test
    public void initialPasswordSuccess_checkView() {
        final var modelAndView = controller.initialPasswordSuccess(request);
        assertThat(modelAndView.getViewName()).isEqualTo("initialPasswordSuccess");
    }

    @Test
    public void initialPasswordSuccess_checkModel() {
        final var modelAndView = controller.initialPasswordSuccess(request);
        assertThat(modelAndView.getModel()).containsOnly(entry("hdcUrl", "http://hdcurl"));
    }

    @Test
    public void initialPasswordSuccess_hdcNotSet() {
        when(request.getContextPath()).thenReturn("/context");
        final var modelAndView = new InitialPasswordController(resetPasswordService, tokenService, userService, telemetryClient, Set.of("password1"), null).initialPasswordSuccess(request);
        assertThat(modelAndView.getModel()).containsOnly(entry("hdcUrl", "/context/login"));
    }

    @Test
    public void initialPassword_checkView() {
        setupCheckTokenValid();
        final var modelAndView = controller.initialPassword("token");
        assertThat(modelAndView.getViewName()).isEqualTo("setPassword");
    }

    @Test
    public void initialPassword_checkModel() {
        setupCheckTokenValid();
        final var modelAndView = controller.initialPassword("sometoken");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "sometoken"), entry("isAdmin", Boolean.FALSE), entry("initial", Boolean.TRUE));
    }

    @Test
    public void initialPassword_checkModelAdminUser() {
        final var user = setupGetUserCallForProfile();
        user.getAccountDetail().setProfile("TAG_ADMIN");
        setupCheckAndGetTokenValid();
        final var modelAndView = controller.initialPassword("sometoken");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "sometoken"), entry("isAdmin", Boolean.TRUE), entry("initial", Boolean.TRUE));
    }

    @Test
    public void initialPassword_FailureCheckView() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("invalid"));
        final var modelAndView = controller.initialPassword("token");
        assertThat(modelAndView.getViewName()).isEqualTo("resetPassword");
    }

    @Test
    public void initialPassword_FailureCheckModel() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.of("expired"));
        final var modelAndView = controller.initialPassword("sometoken");
        assertThat(modelAndView.getModel()).containsOnly(entry("error", "expired"));
    }

    private void setupCheckTokenValid() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty());
    }

    private void setupCheckAndGetTokenValid() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty());
        when(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(new UserToken(TokenType.RESET, UserEmail.of("user"))));
    }

    private StaffUserAccount setupGetUserCallForProfile() {
        final var user = new StaffUserAccount();
        user.setAccountDetail(new AccountDetail());
        when(userService.findUser(anyString())).thenReturn(Optional.of(user));
        return user;
    }
}
