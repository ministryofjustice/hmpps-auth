package uk.gov.justice.digital.hmpps.oauth2server.timed;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeleteDisabledUsersTest {
    @Mock
    private DeleteDisabledUsersService service;
    @Mock
    private TelemetryClient telemetryClient;

    private DeleteDisabledUsers deleteDisabledUsers;

    @Before
    public void setUp() {
        deleteDisabledUsers = new DeleteDisabledUsers(service, telemetryClient);
    }

    @Test
    public void findAndDeleteDisabledUsers() {
        when(service.processInBatches()).thenReturn(0);
        deleteDisabledUsers.findAndDeleteDisabledUsers();
        verify(service).processInBatches();
    }
}
