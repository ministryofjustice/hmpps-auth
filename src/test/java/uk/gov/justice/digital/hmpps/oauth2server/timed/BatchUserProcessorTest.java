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
public class BatchUserProcessorTest {
    @Mock
    private DisableInactiveAuthUsersService service;
    @Mock
    private TelemetryClient telemetryClient;
    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;

    private BatchUserProcessor disableInactiveAuthUsers;

    @Before
    public void setUp() {
        disableInactiveAuthUsers = new DisableInactiveAuthUsers(service, telemetryClient);
    }

    @Test
    public void findAndProcessInBatches_noData() {
        when(service.processInBatches()).thenReturn(0);
        disableInactiveAuthUsers.findAndProcessInBatches();
        verify(service).processInBatches();
    }

    @Test
    public void findAndProcessInBatches_processed() {
        when(service.processInBatches()).thenReturn(10).thenReturn(3);
        disableInactiveAuthUsers.findAndProcessInBatches();
        verify(service, times(2)).processInBatches();

        verify(telemetryClient).trackEvent(eq("DisableInactiveAuthUsersFinished"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).contains(entry("errors", "0"), entry("total", "13"));
    }

    @Test
    public void findAndProcessInBatches_manyProcessed() {
        when(service.processInBatches())
                .thenReturn(10)
                .thenReturn(10)
                .thenReturn(10)
                .thenReturn(10)
                .thenReturn(3);
        disableInactiveAuthUsers.findAndProcessInBatches();
        verify(service, times(5)).processInBatches();

        verify(telemetryClient).trackEvent(eq("DisableInactiveAuthUsersFinished"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).contains(entry("errors", "0"), entry("total", "43"));
    }

    @Test
    public void findAndProcessInBatches_oneFailure() {
        when(service.processInBatches()).thenThrow(new RuntimeException("bob")).thenReturn(5);
        disableInactiveAuthUsers.findAndProcessInBatches();
        verify(service, times(2)).processInBatches();

        verify(telemetryClient).trackEvent(eq("DisableInactiveAuthUsersFinished"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).contains(entry("errors", "1"), entry("total", "5"));
    }

    @Test
    public void findAndProcessInBatches_oneFailureTelemetry() {
        when(service.processInBatches()).thenThrow(new RuntimeException("bob")).thenReturn(0);
        disableInactiveAuthUsers.findAndProcessInBatches();

        verify(telemetryClient).trackEvent(eq("DisableInactiveAuthUsersError"), isNull(), isNull());
    }

    @Test
    public void findAndProcessInBatches_manyFailures() {
        when(service.processInBatches())
                .thenThrow(new RuntimeException("bob"))
                .thenThrow(new RuntimeException("bob"))
                .thenThrow(new RuntimeException("bob"));
        disableInactiveAuthUsers.findAndProcessInBatches();
        verify(service, times(3)).processInBatches();

        verify(telemetryClient).trackEvent(eq("DisableInactiveAuthUsersFinished"), mapCaptor.capture(), isNull());
        assertThat(mapCaptor.getValue()).contains(entry("errors", "3"), entry("total", "0"));
    }
}
