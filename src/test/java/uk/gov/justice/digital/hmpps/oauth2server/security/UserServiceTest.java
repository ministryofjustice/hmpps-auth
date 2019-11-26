package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository;
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffIdentifierRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {
    @Mock
    private NomisUserService nomisUserService;
    @Mock
    private AuthUserService authUserService;
    @Mock
    private StaffIdentifierRepository staffIdentifierRepository;
    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @Before
    public void setUp() {
        userService = new UserService(nomisUserService, authUserService, staffIdentifierRepository, userRepository);
    }

    @Test
    public void findMasterUserPersonDetails_AuthUser() {
        when(authUserService.getAuthUserByUsername(anyString())).thenReturn(createUser());

        final var user = userService.findMasterUserPersonDetails("   bob   ");

        assertThat(user).isPresent().get().extracting(UserPersonDetails::getUsername).isEqualTo("someuser");
    }

    @Test
    public void findMasterUserPersonDetails_NomisUser() {
        when(nomisUserService.getNomisUserByUsername(anyString())).thenReturn(getStaffUserAccountForBob());

        final var user = userService.findMasterUserPersonDetails("bob");

        assertThat(user).isPresent().get().extracting(UserPersonDetails::getUsername).isEqualTo("nomisuser");
    }

    @Test
    public void findUserEmail() {
        final var user = createUser();
        when(userRepository.findByUsername(anyString())).thenReturn(user);

        final var found = userService.findUser("bob");

        assertThat(found).isSameAs(user);
        verify(userRepository).findByUsername("BOB");
    }

    private Optional<User> createUser() {
        return Optional.of(User.of("someuser"));
    }

    private Optional<StaffUserAccount> getStaffUserAccountForBob() {
        final var staffUserAccount = new StaffUserAccount();
        staffUserAccount.setUsername("nomisuser");
        final var staff = new Staff();
        staff.setFirstName("bOb");
        staff.setStatus("ACTIVE");
        staffUserAccount.setStaff(staff);
        final var detail = new AccountDetail("user", "OPEN", "profile", null);
        staffUserAccount.setAccountDetail(detail);
        return Optional.of(staffUserAccount);
    }
}
