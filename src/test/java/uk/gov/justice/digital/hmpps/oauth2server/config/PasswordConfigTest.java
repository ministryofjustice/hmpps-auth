package uk.gov.justice.digital.hmpps.oauth2server.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
public class PasswordConfigTest {
    @Autowired
    private PasswordConfig passwordConfig;

    @Test
    public void encodePassword() {
        final var encoded = passwordConfig.passwordEncoder().encode("some_password_123456");
        assertThat(encoded.length()).isEqualTo(60);
    }
}
