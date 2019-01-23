package uk.gov.justice.digital.hmpps.oauth2server.verify;

import com.microsoft.applicationinsights.TelemetryClient;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TokenServiceTest {
    @Mock
    private UserTokenRepository userTokenRepository;
    @Mock
    private TelemetryClient telemetryClient;
    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;

    private TokenService tokenService;

    @Before
    public void setUp() {
        tokenService = new TokenService(userTokenRepository, telemetryClient);
    }

    @Test
    public void getToken_notfound() {
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.empty());
        assertThat(tokenService.getToken(TokenType.RESET, "token")).isEmpty();
    }

    @Test
    public void getToken_WrongType() {
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(new UserToken(TokenType.VERIFIED, null)));
        assertThat(tokenService.getToken(TokenType.RESET, "token")).isEmpty();
    }

    @Test
    public void getToken() {
        final var userToken = new UserToken(TokenType.RESET, null);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        assertThat(tokenService.getToken(TokenType.RESET, "token")).get().isSameAs(userToken);
    }

    @Test
    public void checkToken() {
        final var userToken = new UserToken(TokenType.RESET, null);
        userToken.setUserEmail(new UserEmail("user"));
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        assertThat(tokenService.checkToken(TokenType.RESET, "token")).isEmpty();
    }

    @Test
    public void checkToken_invalid() {
        final var userToken = new UserToken(TokenType.CHANGE, null);
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        assertThat(tokenService.checkToken(TokenType.RESET, "token")).get().isEqualTo("invalid");
    }

    @Test
    public void checkToken_expiredTelemetryUsername() {
        final var userToken = new UserToken(TokenType.RESET, null);
        userToken.setUserEmail(new UserEmail("user"));
        userToken.setTokenExpiry(LocalDateTime.now().minusHours(1));
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        tokenService.checkToken(TokenType.RESET, "token");
        verify(telemetryClient).trackEvent(eq("ResetPasswordFailure"), mapCaptor.capture(), isNull());
        final var value = mapCaptor.getValue();
        assertThat(value).containsOnly(MapEntry.entry("username", "user"), MapEntry.entry("reason", "expired"));
    }

    @Test
    public void checkToken_expired() {
        final var userToken = new UserToken(TokenType.RESET, new UserEmail("joe"));
        userToken.setTokenExpiry(LocalDateTime.now().minusHours(1));
        when(userTokenRepository.findById(anyString())).thenReturn(Optional.of(userToken));
        assertThat(tokenService.checkToken(TokenType.RESET, "token")).get().isEqualTo("expired");
    }
}
