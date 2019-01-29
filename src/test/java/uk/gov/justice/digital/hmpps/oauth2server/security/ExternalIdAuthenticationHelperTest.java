package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.userdetails.UserDetailsService;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExternalIdAuthenticationHelperTest {
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private UserService userService;

    private ExternalIdAuthenticationHelper helper;

    @Before
    public void setUp() {
        helper = new ExternalIdAuthenticationHelper(userService, userDetailsService);
    }

    @Test
    public void getUserDetails_eraseCredentials() {
        final var userDetails = new UserDetailsImpl("user", "name", "pass",
                true, true, true, true, Collections.emptyList());

        when(userService.getUserByUsername(anyString())).thenReturn(getStaffUserAccountForBob());
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);

        final var details = helper.getUserDetails(Map.of("username", "bobuser"));

        assertThat(details.getPassword()).isNull();
    }

    @Test
    public void getUserDetails_notFound() {
        final var userDetails = new UserDetailsImpl("user", "name", "pass",
                true, true, true, true, Collections.emptyList());

        when(userService.getUserByUsername(anyString())).thenReturn(getStaffUserAccountForBob());
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(null);

        final var details = helper.getUserDetails(Map.of("username", "bobuser"));

        assertThat(details).isNull();
    }

    private Optional<StaffUserAccount> getStaffUserAccountForBob() {
        final var staffUserAccount = new StaffUserAccount();
        staffUserAccount.setUsername("staffuser");
        final var staff = new Staff();
        staff.setFirstName("bOb");
        staffUserAccount.setStaff(staff);
        return Optional.of(staffUserAccount);
    }
}
