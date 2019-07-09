package uk.gov.justice.digital.hmpps.oauth2server.verify;

import com.weddini.throttling.ThrottlingException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional(transactionManager = "authTransactionManager")
public class ResetPasswordServiceIntTest {
    @Autowired
    private ResetPasswordService resetPasswordService;
    @MockBean
    private HttpServletRequest request;

    @Test
    public void requestResetPassword_throttleRequests() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, null));

        when(request.getRemoteAddr()).thenReturn("request_ip:12345");
        // five initial requests
        resetPasswordService.requestResetPassword("user", "url");
        resetPasswordService.requestResetPassword("user", "url");
        resetPasswordService.requestResetPassword("user", "url");
        resetPasswordService.requestResetPassword("user", "url");
        resetPasswordService.requestResetPassword("user", "url");

        // same ip, but different port should still count towards total
        when(request.getRemoteAddr()).thenReturn("request_ip:23456");
        resetPasswordService.requestResetPassword("user", "url");

        // seventh request within a minute by same ip will throw the exception
        assertThatThrownBy(() -> resetPasswordService.requestResetPassword("user", "url")).isInstanceOf(ThrottlingException.class);
    }

    @Test
    public void requestResetPassword_throttleRequestsDifferentIp() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, null));

        when(request.getRemoteAddr()).thenReturn("throttle_ip:12345");
        // six initial requests
        resetPasswordService.requestResetPassword("user", "url");
        resetPasswordService.requestResetPassword("user", "url");
        resetPasswordService.requestResetPassword("user", "url");
        resetPasswordService.requestResetPassword("user", "url");
        resetPasswordService.requestResetPassword("user", "url");
        resetPasswordService.requestResetPassword("user", "url");

        // different ip so shouldn't count towards total and should be allowed
        when(request.getRemoteAddr()).thenReturn("different_ip:12345");
        resetPasswordService.requestResetPassword("user", "url");
    }

}
