package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.UserEmail;
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserEmailRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.StaffUserAccount;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffIdentifierRepository;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffUserAccountRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {
    @Mock
    private UserEmailRepository userEmailRepository;
    @Mock
    private StaffUserAccountRepository staffUserAccountRepository;
    @Mock
    private StaffIdentifierRepository staffIdentifierRepository;

    private UserService userService;

    @Before
    public void setUp() {
        userService = new UserService(staffUserAccountRepository, staffIdentifierRepository, userEmailRepository);
    }

    @Test
    public void findUser_AuthUser() {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(createUserEmailUser());

        final var user = userService.findUser("bob");

        assertThat(user).isPresent().get().extracting(UserPersonDetails::getUsername).isEqualTo("someuser");

        verify(userEmailRepository).findByUsernameAndMasterIsTrue("BOB");
    }

    @Test
    public void findUser_NomisUser() {
        when(userEmailRepository.findByUsernameAndMasterIsTrue(anyString())).thenReturn(Optional.empty());
        when(staffUserAccountRepository.findById(anyString())).thenReturn(getStaffUserAccountForBob());

        final var user = userService.findUser("bob");

        assertThat(user).isPresent().get().extracting(UserPersonDetails::getUsername).isEqualTo("nomisuser");

        verify(userEmailRepository).findByUsernameAndMasterIsTrue("BOB");
        verify(staffUserAccountRepository).findById("BOB");
    }

    private Optional<UserEmail> createUserEmailUser() {
        return Optional.of(new UserEmail("someuser"));
    }

    private Optional<StaffUserAccount> getStaffUserAccountForBob() {
        final var staffUserAccount = new StaffUserAccount();
        staffUserAccount.setUsername("nomisuser");
        final var staff = new Staff();
        staff.setFirstName("bOb");
        staff.setStatus("ACTIVE");
        staffUserAccount.setStaff(staff);
        final var detail = new AccountDetail("user", "OPEN", "profile");
        staffUserAccount.setAccountDetail(detail);
        return Optional.of(staffUserAccount);
    }
}
