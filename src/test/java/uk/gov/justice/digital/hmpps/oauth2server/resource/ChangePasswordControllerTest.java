package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
import org.assertj.core.data.MapEntry;
import org.assertj.core.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.security.ChangePasswordService;
import uk.gov.justice.digital.hmpps.oauth2server.security.JwtAuthenticationSuccessHandler;
import uk.gov.justice.digital.hmpps.oauth2server.security.PasswordValidationFailureException;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserService;
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ChangePasswordControllerTest {
    @Mock
    private JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler;
    @Mock
    private DaoAuthenticationProvider daoAuthenticationProvider;
    @Mock
    private ChangePasswordService changePasswordService;
    @Mock
    private UserService userService;
    @Mock
    private TokenService tokenService;
    @Mock
    private TelemetryClient telemetryClient;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;

    private ChangePasswordController controller;

    @Before
    public void setUp() {
        controller = new ChangePasswordController(jwtAuthenticationSuccessHandler,
                daoAuthenticationProvider, changePasswordService, tokenService, userService, telemetryClient, Set.of("password1"));
    }

    @Test
    public void changePasswordRequest() {
        final var view = controller.changePasswordRequest("token");
        assertThat(view.getViewName()).isEqualTo("changePassword");
    }

    @Test
    public void changePasswordRequest_adminUser() {
        setupCheckAndGetTokenValid();
        final var user = setupGetUserCallForProfile();
        user.getAccountDetail().setProfile("TAG_ADMIN");
        final var model = controller.changePasswordRequest("token");
        assertThat(model.getModel().get("isAdmin")).isEqualTo(Boolean.TRUE);
    }

    @Test
    public void changePasswordRequest_generalUser() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile();
        final var model = controller.changePasswordRequest("token");
        assertThat(model.getModel().get("isAdmin")).isEqualTo(Boolean.FALSE);
    }

    @Test
    public void changePasswordRequest_tokenInvalid() {
        final var model = controller.changePasswordRequest("token");
        assertThat(model.getModel().get("isAdmin")).isEqualTo(Boolean.FALSE);
    }

    @Test
    public void changePasswordRequest_NotAlphanumeric() throws IOException, ServletException {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile();
        final var modelAndView = controller.changePassword("d", "@fewfewfew1", "@fewfewfew1", request, response);
        assertThat(modelAndView.getViewName()).isEqualTo("changePassword");
        assertThat(modelAndView.getModel()).containsOnly(entry("token", "d"), entry("error", Boolean.TRUE), listEntry("errornew", "alphanumeric"), entry("isAdmin", Boolean.FALSE));
    }

    @Test
    public void changePassword_ValidationFailure() throws Exception {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile();
        doThrow(new PasswordValidationFailureException()).when(changePasswordService).setPassword(anyString(), anyString());
        final var redirect = controller.changePassword("user", "password2", "password2", request, response);
        assertThat(redirect.getViewName()).isEqualTo("changePassword");
        assertThat(redirect.getModel()).containsOnly(entry("token", "user"), entry("error", Boolean.TRUE), entry("errornew", "validation"), entry("isAdmin", Boolean.FALSE));
    }

    @Test
    public void changePassword_OtherException() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile();
        final var exception = new RuntimeException();
        doThrow(exception).when(changePasswordService).setPassword(anyString(), anyString());
        assertThatThrownBy(
                () -> controller.changePassword("user", "password2", "password2", request, response)
        ).isEqualTo(exception);
    }

    @Test
    public void changePassword_Success() throws Exception {
        final var token = new UsernamePasswordAuthenticationToken("bob", "pass");
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile();
        when(daoAuthenticationProvider.authenticate(any())).thenReturn(token);
        final var redirect = controller.changePassword("user", "password2", "password2", request, response);
        assertThat(redirect).isNull();
        final var authCapture = ArgumentCaptor.forClass(Authentication.class);
        verify(daoAuthenticationProvider).authenticate(authCapture.capture());
        final var value = authCapture.getValue();
        assertThat(value.getPrincipal()).isEqualTo("USER");
        assertThat(value.getCredentials()).isEqualTo("password2");
        verify(changePasswordService).setPassword("user", "password2");
        verify(jwtAuthenticationSuccessHandler).onAuthenticationSuccess(request, response, token);
    }

    @Test
    public void changePassword_Success_Telemetry() throws Exception {
        final var token = new UsernamePasswordAuthenticationToken("bob", "pass");
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile();
        when(daoAuthenticationProvider.authenticate(any())).thenReturn(token);
        controller.changePassword("user", "password2", "password2", request, response);

        verify(telemetryClient).trackEvent(eq("ChangePasswordSuccess"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).containsExactly(entry("username", "user"));
    }

    @Test
    public void changePassword_AuthenticateSuccess_Telemetry() throws Exception {
        final var token = new UsernamePasswordAuthenticationToken("bob", "pass");
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile();
        when(daoAuthenticationProvider.authenticate(any())).thenReturn(token);
        controller.changePassword("user", "password2", "password2", request, response);

        verify(telemetryClient).trackEvent(eq("ChangePasswordAuthenticateSuccess"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).containsExactly(entry("username", "user"));
    }

    @Test
    public void changePassword_SuccessAccountExpired() throws Exception {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile();
        when(daoAuthenticationProvider.authenticate(any())).thenThrow(new AccountExpiredException("msg"));
        final var redirect = controller.changePassword("user", "password2", "password2", request, response);
        assertThat(redirect.getViewName()).isEqualTo("redirect:/login?error=changepassword");
        final var authCapture = ArgumentCaptor.forClass(Authentication.class);
        verify(daoAuthenticationProvider).authenticate(authCapture.capture());
        final var value = authCapture.getValue();
        assertThat(value.getPrincipal()).isEqualTo("USER");
        assertThat(value.getCredentials()).isEqualTo("password2");
        verify(changePasswordService).setPassword("user", "password2");
    }

    @Test
    public void changePassword_SuccessAccountExpired_TelemetryFailure() throws Exception {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile();
        when(daoAuthenticationProvider.authenticate(any())).thenThrow(new AccountExpiredException("msg"));
        controller.changePassword("user", "password2", "password2", request, response);

        verify(telemetryClient).trackEvent(eq("ChangePasswordAuthenticateFailure"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).containsExactly(entry("username", "user"), entry("reason", "AccountExpiredException"));
    }

    @Test
    public void changePassword_SuccessAccountExpired_TelemetrySuccess() throws Exception {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile();
        when(daoAuthenticationProvider.authenticate(any())).thenThrow(new AccountExpiredException("msg"));
        controller.changePassword("user", "password2", "password2", request, response);

        verify(telemetryClient).trackEvent(eq("ChangePasswordSuccess"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).containsExactly(entry("username", "user"));
    }

    private StaffUserAccount setupGetUserCallForProfile() {
        final var user = new StaffUserAccount();
        user.setAccountDetail(new AccountDetail());
        when(userService.findUser(anyString())).thenReturn(Optional.of(user));
        return user;
    }

    private void setupCheckAndGetTokenValid() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty());
        when(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(new UserToken(TokenType.RESET, UserEmail.of("user"))));
    }

    private MapEntry<String, List<Object>> listEntry(final String key, final Object... values) {
        return MapEntry.entry(key, Arrays.asList(values));
    }
}
