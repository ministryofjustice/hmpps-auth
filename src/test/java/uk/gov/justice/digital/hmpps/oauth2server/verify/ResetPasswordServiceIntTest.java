package uk.gov.justice.digital.hmpps.oauth2server.verify;

import com.weddini.throttling.ThrottlingException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.service.notify.NotificationClientException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional
public class ResetPasswordServiceIntTest {
    @Autowired
    private ResetPasswordService resetPasswordService;

    @Test
    public void requestResetPassword_throttleRequests() throws NotificationClientException {
        resetPasswordService.requestResetPassword("user", "url");
        resetPasswordService.requestResetPassword("user", "url");
        resetPasswordService.requestResetPassword("user", "url");
        resetPasswordService.requestResetPassword("user", "url");
        resetPasswordService.requestResetPassword("user", "url");
        resetPasswordService.requestResetPassword("user", "url");

        // seventh request within a minute by same ip will throw the exception
        assertThatThrownBy(() -> resetPasswordService.requestResetPassword("user", "url")).isInstanceOf(ThrottlingException.class);
    }
}
