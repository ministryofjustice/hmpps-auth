package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OracleSha1PasswordEncoderTest {
    @Test
    public void matches_testMatchesOraclePassword() {
        final var encodedPassword = "S:39BA463D55E5C8936A6798CC37B1347BA8BEC37B6407397EB769BC356F0C";
        assertThat(new OracleSha1PasswordEncoder().matches("somepass1", encodedPassword)).isTrue();
    }

    @Test
    public void matches_invalidPasswordLength() {
        final var encodedPassword = "somepasswordthatshouldbeencoded";
        assertThat(new OracleSha1PasswordEncoder().matches("somepass1", encodedPassword)).isFalse();
    }

    @Test
    public void matches_nullInput() {
        assertThat(new OracleSha1PasswordEncoder().matches("somepass1", null)).isFalse();
    }
}
