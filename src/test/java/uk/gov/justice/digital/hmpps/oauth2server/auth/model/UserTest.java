package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import org.junit.Test;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserToken.TokenType;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTest {
    @Test
    public void testCreateTokenOverwritesPrevious() {
        final var user = User.of("user");
        user.createToken(TokenType.RESET);
        final var changeToken = user.createToken(TokenType.CHANGE);
        final var resetToken = user.createToken(TokenType.RESET);

        assertThat(user.getTokens()).containsOnly(changeToken, resetToken);
        assertThat(user.getTokens()).extracting(UserToken::getToken).containsOnly(changeToken.getToken(), resetToken.getToken());
    }
}
