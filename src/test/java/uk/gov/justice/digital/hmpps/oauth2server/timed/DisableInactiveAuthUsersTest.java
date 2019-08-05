package uk.gov.justice.digital.hmpps.oauth2server.timed;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DisableInactiveAuthUsersTest {
    @Mock
    private DisableInactiveAuthUsersService service;
    @Mock
    private TelemetryClient telemetryClient;
    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;

    private DisableInactiveAuthUsers disableInactiveAuthUsers;

    @Before
    public void setUp() {
        disableInactiveAuthUsers = new DisableInactiveAuthUsers(service, telemetryClient);
    }

    @Test
    public void findAndDisableInactiveAuthUsers_noData() {
        when(service.findAndDisableInactiveAuthUsers()).thenReturn(0);
        disableInactiveAuthUsers.findAndDisableInactiveAuthUsers();
        verify(service).findAndDisableInactiveAuthUsers();
    }

    @Test
    public void findAndDisableInactiveAuthUsers_processed() {
        when(service.findAndDisableInactiveAuthUsers()).thenReturn(10).thenReturn(3);
        disableInactiveAuthUsers.findAndDisableInactiveAuthUsers();
        verify(service, times(2)).findAndDisableInactiveAuthUsers();

        verify(telemetryClient).trackEvent(eq("DisableInactiveAuthUsersFinished"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).contains(entry("errors", "0"), entry("total", "13"));
    }

    @Test
    public void findAndDisableInactiveAuthUsers_manyProcessed() {
        when(service.findAndDisableInactiveAuthUsers())
                .thenReturn(10)
                .thenReturn(10)
                .thenReturn(10)
                .thenReturn(10)
                .thenReturn(3);
        disableInactiveAuthUsers.findAndDisableInactiveAuthUsers();
        verify(service, times(5)).findAndDisableInactiveAuthUsers();

        verify(telemetryClient).trackEvent(eq("DisableInactiveAuthUsersFinished"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).contains(entry("errors", "0"), entry("total", "43"));
    }

    @Test
    public void findAndDisableInactiveAuthUsers_oneFailure() {
        when(service.findAndDisableInactiveAuthUsers()).thenThrow(new RuntimeException("bob")).thenReturn(5);
        disableInactiveAuthUsers.findAndDisableInactiveAuthUsers();
        verify(service, times(2)).findAndDisableInactiveAuthUsers();

        verify(telemetryClient).trackEvent(eq("DisableInactiveAuthUsersFinished"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).contains(entry("errors", "1"), entry("total", "5"));
    }

    @Test
    public void findAndDisableInactiveAuthUsers_oneFailureTelemetry() {
        when(service.findAndDisableInactiveAuthUsers()).thenThrow(new RuntimeException("bob")).thenReturn(0);
        disableInactiveAuthUsers.findAndDisableInactiveAuthUsers();

        verify(telemetryClient).trackEvent(eq("DisableInactiveAuthUsersError"), isNull(), isNull());
    }

    @Test
    public void findAndDisableInactiveAuthUsers_manyFailures() {
        when(service.findAndDisableInactiveAuthUsers())
                .thenThrow(new RuntimeException("bob"))
                .thenThrow(new RuntimeException("bob"))
                .thenThrow(new RuntimeException("bob"));
        disableInactiveAuthUsers.findAndDisableInactiveAuthUsers();
        verify(service, times(3)).findAndDisableInactiveAuthUsers();

        verify(telemetryClient).trackEvent(eq("DisableInactiveAuthUsersFinished"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).contains(entry("errors", "3"), entry("total", "0"));
    }
}
