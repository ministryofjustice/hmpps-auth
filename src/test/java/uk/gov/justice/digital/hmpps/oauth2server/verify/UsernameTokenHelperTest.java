package uk.gov.justice.digital.hmpps.oauth2server.verify;

import org.junit.Test;
import uk.gov.justice.digital.hmpps.oauth2server.verify.UsernameTokenHelper.UsernameToken;

import static org.assertj.core.api.Assertions.assertThat;

public class UsernameTokenHelperTest {

    static final String USER_TOKEN_BASE64 = "dXNlci10b2tlbg==";

    private final UsernameTokenHelper usernameTokenHelper = new UsernameTokenHelper();

    @Test
    public void createUsernameTokenEncodedString() {
        final var base64 = usernameTokenHelper.createUsernameTokenEncodedString("user", "token");
        assertThat(base64).isEqualTo(USER_TOKEN_BASE64);
    }

    @Test
    public void readUsernameTokenFromEncodedString() {
        final var usernameToken = usernameTokenHelper.readUsernameTokenFromEncodedString(USER_TOKEN_BASE64);
        assertThat(usernameToken).get().isEqualToComparingFieldByField(new UsernameToken("user", "token"));
    }

    @Test
    public void readUsernameTokenFromEncodedString_Invalid() {
        final var base64 = usernameTokenHelper.readUsernameTokenFromEncodedString("AAAAA");
        assertThat(base64).isEmpty();
    }

    @Test
    public void bothWaysTest() {
        final var base64 = usernameTokenHelper.createUsernameTokenEncodedString("user", "token");
        final var usernameToken = usernameTokenHelper.readUsernameTokenFromEncodedString(base64);
        assertThat(usernameToken).get().isEqualToComparingFieldByField(new UsernameToken("user", "token"));
    }

    @Test
    public void readUsernameTokenFromEncodedString_Invalid_emptyUser() {
        final var base64 = usernameTokenHelper.createUsernameTokenEncodedString("", "token");
        final var usernameToken = usernameTokenHelper.readUsernameTokenFromEncodedString(base64);
        assertThat(usernameToken).isEmpty();
    }

    @Test
    public void readUsernameTokenFromEncodedString_Invalid_emptyToken() {
        final var base64 = usernameTokenHelper.createUsernameTokenEncodedString("user", "");
        final var usernameToken = usernameTokenHelper.readUsernameTokenFromEncodedString(base64);
        assertThat(usernameToken).isEmpty();
    }
}
