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
class DeleteDisabledUsersTest {
    @Mock
    private DeleteDisabledUsersService service;
    @Mock
    private TelemetryClient telemetryClient;

    private DeleteDisabledUsers deleteDisabledUsers;

    @BeforeEach
    void setUp() {
        deleteDisabledUsers = new DeleteDisabledUsers(service, telemetryClient);
    }

    @Test
    void findAndDeleteDisabledUsers() {
        when(service.processInBatches()).thenReturn(0);
        deleteDisabledUsers.findAndDeleteDisabledUsers();
        verify(service).processInBatches();
    }
}
