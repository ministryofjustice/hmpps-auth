package uk.gov.justice.digital.hmpps.oauth2server.timed;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class DisableInactiveAuthUsersTest {
    @Mock
    private DisableInactiveAuthUsersService service;
    @Mock
    private TelemetryClient telemetryClient;

    private DisableInactiveAuthUsers disableInactiveAuthUsers;

    @BeforeEach
    void setUp() {
        disableInactiveAuthUsers = new DisableInactiveAuthUsers(service, telemetryClient);
    }

    @Test
    void findAndDisableInactiveAuthUsers() {
        when(service.processInBatches()).thenReturn(0);
        disableInactiveAuthUsers.findAndDisableInactiveAuthUsers();
        verify(service).processInBatches();
    }
}
