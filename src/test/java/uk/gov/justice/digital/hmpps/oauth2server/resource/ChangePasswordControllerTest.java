package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
import org.assertj.core.data.MapEntry;
import org.assertj.core.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
import uk.gov.justice.digital.hmpps.oauth2server.security.*;
import uk.gov.justice.digital.hmpps.oauth2server.verify.TokenService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ChangePasswordControllerTest {
    @Mock
    private UserStateAuthenticationFailureHandler userStateAuthenticationFailureHandler;
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

    private ChangePasswordController controller;

    @Before
    public void setUp() {
        controller = new ChangePasswordController(jwtAuthenticationSuccessHandler,
                daoAuthenticationProvider, changePasswordService, tokenService, userService, telemetryClient);
    }

    @Test
    public void changePasswordRequest() {
        final var view = controller.changePasswordRequest();
        assertThat(view).isEqualTo("changePassword");
    }

    @Test
    public void changePassword_ValidationFailure() throws Exception {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        doThrow(new PasswordValidationFailureException()).when(changePasswordService).setPassword(anyString(), anyString());
        final var redirect = controller.changePassword("user", "password1", "password1", request, response);
        assertThat(redirect.getViewName()).isEqualTo("changePassword");
        assertThat(redirect.getModel()).containsOnly(entry("token", "user"), entry("error", Boolean.TRUE), entry("errornew", "validation"));
    }

    @Test
    public void changePassword_OtherException() {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        final var exception = new RuntimeException();
        doThrow(exception).when(changePasswordService).setPassword(anyString(), anyString());
        assertThatThrownBy(
                () -> controller.changePassword("user", "password1", "password1", request, response)
        ).isEqualTo(exception);
    }

    @Test
    public void changePassword_Success() throws Exception {
        final var token = new UsernamePasswordAuthenticationToken("bob", "pass");
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        when(daoAuthenticationProvider.authenticate(any())).thenReturn(token);
        final var redirect = controller.changePassword("user", "password1", "password1", request, response);
        assertThat(redirect).isNull();
        final var authCapture = ArgumentCaptor.forClass(Authentication.class);
        verify(daoAuthenticationProvider).authenticate(authCapture.capture());
        final var value = authCapture.getValue();
        assertThat(value.getPrincipal()).isEqualTo("USER");
        assertThat(value.getCredentials()).isEqualTo("password1");
        verify(changePasswordService).setPassword("user", "password1");
        verify(jwtAuthenticationSuccessHandler).onAuthenticationSuccess(request, response, token);
    }

    @Test
    public void changePassword_SuccessAccountExpired() throws Exception {
        setupCheckAndGetTokenValid();
        setupGetUserCallForProfile(null);
        when(daoAuthenticationProvider.authenticate(any())).thenThrow(new AccountExpiredException("msg"));
        final var redirect = controller.changePassword("user", "password1", "password1", request, response);
        assertThat(redirect.getViewName()).isEqualTo("redirect:/login?error=changepassword");
        final var authCapture = ArgumentCaptor.forClass(Authentication.class);
        verify(daoAuthenticationProvider).authenticate(authCapture.capture());
        final var value = authCapture.getValue();
        assertThat(value.getPrincipal()).isEqualTo("USER");
        assertThat(value.getCredentials()).isEqualTo("password1");
        verify(changePasswordService).setPassword("user", "password1");
    }

    private void setupGetUserCallForProfile(final String profile) {
        final var user = Optional.of(new StaffUserAccount());
        final var detail = new AccountDetail();
        detail.setProfile(profile);
        user.get().setAccountDetail(detail);
        when(userService.getUserByUsername(anyString())).thenReturn(user);
    }

    private void setupCheckAndGetTokenValid() {
        when(tokenService.checkToken(any(), anyString())).thenReturn(Optional.empty());
        when(tokenService.getToken(any(), anyString())).thenReturn(Optional.of(new UserToken(TokenType.RESET, new UserEmail("user"))));
    }

    private MapEntry<String, List<Object>> listEntry(final String key, final Object... values) {
        return MapEntry.entry(key, Arrays.asList(values));
    }
}
