package uk.gov.justice.digital.hmpps.oauth2server.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.justice.digital.hmpps.oauth2server.oasys.model.OasysUser;
import uk.gov.justice.digital.hmpps.oauth2server.oasys.repository.OasysUserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OasysNomisUserServiceTest {

    @Mock
    private OasysUserRepository oasysUserRepository;
    private OasysUserService oasysUserService;

    @Before
    public void setUp() {
        oasysUserService = new OasysUserService(oasysUserRepository);
    }

    @Test
    public void findUser_OasysUser() {
        when(oasysUserRepository.findById(anyString())).thenReturn(getOasysUserAccountForBob());
        final var user = oasysUserService.findUser("oasysuser");
        assertThat(user).isPresent().get().extracting(UserPersonDetails::getUsername).isEqualTo("oasysuser");
        verify(oasysUserRepository).findById("oasysuser");
    }

    private Optional<OasysUser> getOasysUserAccountForBob() {
        return Optional.of(OasysUser.builder().userForename1("bOb").oasysUserCode("oasysuser").build());
    }
}
