package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.security.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

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
    private TelemetryClient telemetryClient;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private ChangePasswordController controller;

    @Before
    public void setUp() {
        controller = new ChangePasswordController(userStateAuthenticationFailureHandler, jwtAuthenticationSuccessHandler,
                daoAuthenticationProvider, changePasswordService, userService, telemetryClient);
    }

    @Test
    public void changePasswordRequest() {
        final var modelAndView = controller.changePasswordRequest(null, " ");
        assertThat(modelAndView.getViewName()).isEqualTo("changePassword");
        assertThat(modelAndView.getStatus()).isNull();
    }

    @Test
    public void changePasswordRequest_CurrentError() {
        final var modelAndView = controller.changePasswordRequest("bad", null);
        assertThat(modelAndView.getViewName()).isEqualTo("changePassword");
        assertThat(modelAndView.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void changePasswordRequest_NewError() {
        final var modelAndView = controller.changePasswordRequest(null, "bad");
        assertThat(modelAndView.getViewName()).isEqualTo("changePassword");
        assertThat(modelAndView.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void changePassword_MissingUsername() throws Exception {
        final var redirect = controller.changePassword("", "pass", "new", "new", request, response);
        assertThat(redirect).isEqualTo("redirect:/login?error&reason=missing");
    }

    @Test
    public void changePassword_MissingPassword() throws Exception {
        final var redirect = controller.changePassword("bob", "    ", "new", "new", request, response);
        assertThat(redirect).isEqualTo("redirect:/change-password?errorcurrent=missing&username=BOB");
    }

    @Test
    public void changePassword_NotAlphanumeric() throws Exception {
        final var redirect = controller.changePassword("d", "old", "@fewfewfew", "new", request, response);
        assertThat(redirect).isEqualTo("redirect:/change-password?errornew=alphanumeric&username=D");
    }

    @Test
    public void changePassword_Blank() throws Exception {
        setupGetUserCall(null);
        final var redirect = controller.changePassword("d", "old", "", "", request, response);
        assertThat(redirect).isEqualTo("redirect:/change-password?errornew=alphanumeric&username=D");
    }

    @Test
    public void changePassword_Length() throws Exception {
        setupGetUserCall(null);
        final var redirect = controller.changePassword("d", "old", "qwerqw12", "qwerqw12", request, response);
        assertThat(redirect).isEqualTo("redirect:/change-password?errornew=length9&username=D");
    }

    @Test
    public void changePassword_LengthAdmin() throws Exception {
        setupGetUserCall("TAG_ADMIN");
        final var redirect = controller.changePassword("d", "old", "qwerqwerqwe12", "qwerqwerqwe12", request, response);
        assertThat(redirect).isEqualTo("redirect:/change-password?errornew=length14&username=D");
    }

    @Test
    public void changePassword_IncorrectCurrentPassword() throws Exception {
        when(daoAuthenticationProvider.authenticate(any())).thenThrow(new BadCredentialsException("msg"));
        final var redirect = controller.changePassword("user", "old", "qwerqwerqwe12", "qwerqwerqwe12", request, response);
        assertThat(redirect).isEqualTo("redirect:/change-password?errorcurrent=invalid&username=USER");
    }

    @Test
    public void changePassword_SameAsUsername() throws Exception {
        final var redirect = controller.changePassword("someuser1", "old", "someuser1", "new", request, response);
        assertThat(redirect).isEqualTo("redirect:/change-password?errornew=username&username=SOMEUSER1");
    }

    @Test
    public void changePassword_ContainsUsername() throws Exception {
        final var redirect = controller.changePassword("someuser1", "old", "someuser12", "new", request, response);
        assertThat(redirect).isEqualTo("redirect:/change-password?errornew=username&username=SOMEUSER1");
    }

    @Test
    public void changePassword_FourDistinct() throws Exception {
        final var redirect = controller.changePassword("d", "old", "as1as1as1", "new", request, response);
        assertThat(redirect).isEqualTo("redirect:/change-password?errornew=four&username=D");
    }

    @Test
    public void changePassword_MissingDigits() throws Exception {
        final var redirect = controller.changePassword("d", "old", "asdasdasdb", "new", request, response);
        assertThat(redirect).isEqualTo("redirect:/change-password?errornew=nodigits&username=D");
    }

    @Test
    public void changePassword_OnlyDigits() throws Exception {
        final var redirect = controller.changePassword("d", "old", "1231231234", "new", request, response);
        assertThat(redirect).isEqualTo("redirect:/change-password?errornew=alldigits&username=D");
    }

    @Test
    public void changePassword_Mismatch() throws Exception {
        final var redirect = controller.changePassword("user", "old", "password1", "new", request, response);
        assertThat(redirect).isEqualTo("redirect:/change-password?errornew=mismatch&username=USER");
    }

    @Test
    public void changePassword_AccountLocked() throws Exception {
        final var lockedException = new LockedException("msg");
        setupGetUserCall(null);
        when(daoAuthenticationProvider.authenticate(any())).thenThrow(lockedException);
        final var redirect = controller.changePassword("user", "old", "password1", "password1", request, response);
        assertThat(redirect).isNull();
        verify(userStateAuthenticationFailureHandler).onAuthenticationFailure(request, response, lockedException);
    }

    @Test
    public void changePassword_AccountExpiredPasswordMismatch() throws Exception {
        final var expiredException = new AccountExpiredException("msg");
        setupGetUserCall(null);
        when(daoAuthenticationProvider.authenticate(any())).thenThrow(expiredException);
        final var redirect = controller.changePassword("user", "old", "password1", "new", request, response);
        assertThat(redirect).isEqualTo("redirect:/change-password?errornew=mismatch&username=USER");
        verifyZeroInteractions(userStateAuthenticationFailureHandler);
    }

    @Test
    public void changePassword_ValidationFailure() throws Exception {
        setupGetUserCall(null);
        doThrow(new PasswordValidationFailureException()).when(changePasswordService).changePassword(anyString(), anyString());
        final var redirect = controller.changePassword("user", "old", "password1", "password1", request, response);
        assertThat(redirect).isEqualTo("redirect:/change-password?errornew=validation&username=USER");
    }

    @Test
    public void changePassword_OtherException() {
        setupGetUserCall(null);
        final var exception = new RuntimeException();
        doThrow(exception).when(changePasswordService).changePassword(anyString(), anyString());
        assertThatThrownBy(
                () -> controller.changePassword("user", "old", "password1", "password1", request, response)
        ).isEqualTo(exception);
    }

    @Test
    public void changePassword_ReusedPassword() throws Exception {
        setupGetUserCall(null);
        doThrow(new ReusedPasswordException()).when(changePasswordService).changePassword(anyString(), anyString());
        final var redirect = controller.changePassword("user", "old", "password1", "password1", request, response);
        assertThat(redirect).isEqualTo("redirect:/change-password?errornew=reused&username=USER");
    }

    @Test
    public void changePassword_Success() throws Exception {
        final var token = new UsernamePasswordAuthenticationToken("bob", "pass");
        setupGetUserCall(null);
        when(daoAuthenticationProvider.authenticate(any())).thenReturn(token);
        final var redirect = controller.changePassword("user", "old", "password1", "password1", request, response);
        assertThat(redirect).isNull();
        final var authCapture = ArgumentCaptor.forClass(Authentication.class);
        verify(daoAuthenticationProvider, times(2)).authenticate(authCapture.capture());
        final var value = authCapture.getValue();
        assertThat(value.getPrincipal()).isEqualTo("USER");
        assertThat(value.getCredentials()).isEqualTo("password1");
        verify(changePasswordService).changePassword("USER", "password1");
        verify(jwtAuthenticationSuccessHandler).onAuthenticationSuccess(request, response, token);
    }

    @Test
    public void changePassword_SuccessAccountExpired() throws Exception {
        setupGetUserCall(null);
        when(daoAuthenticationProvider.authenticate(any())).thenThrow(new AccountExpiredException("msg"));
        final var redirect = controller.changePassword("user", "old", "password1", "password1", request, response);
        assertThat(redirect).isEqualTo("redirect:/login?error&reason=changepassword");
        final var authCapture = ArgumentCaptor.forClass(Authentication.class);
        verify(daoAuthenticationProvider, times(2)).authenticate(authCapture.capture());
        final var value = authCapture.getValue();
        assertThat(value.getPrincipal()).isEqualTo("USER");
        assertThat(value.getCredentials()).isEqualTo("password1");
        verify(changePasswordService).changePassword("USER", "password1");
    }

    private void setupGetUserCall(final String profile) {
        final var user = Optional.of(new StaffUserAccount());
        final var detail = new AccountDetail();
        detail.setProfile(profile);
        user.get().setAccountDetail(detail);
        when(userService.getUserByUsername(anyString())).thenReturn(user);
    }
}
