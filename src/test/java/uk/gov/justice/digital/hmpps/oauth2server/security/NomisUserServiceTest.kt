package uk.gov.justice.digital.hmpps.oauth2server.security

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.crypto.password.PasswordEncoder
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetailsHelper.Companion.createSampleNomisUser
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.StaffUserAccountRepository
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.nomis
import javax.sql.DataSource

internal class NomisUserServiceTest {
  private val dataSource: DataSource = mock()
  private val passwordEncoder: PasswordEncoder = mock()
  private val staffUserAccountRepository: StaffUserAccountRepository = mock()
  private val userRepository: UserRepository = mock()
  private val nomisUserService: NomisUserService =
    NomisH2AlterUserService(dataSource, passwordEncoder, staffUserAccountRepository, userRepository)

  @Test
  fun `getNomisUsersByEmail no users found`() {
    assertThat(nomisUserService.getNomisUsersByEmail("EmAil")).isEmpty()

    verify(staffUserAccountRepository).findAllNomisUsersByEmailAddress("email")
    verify(userRepository).findByEmailAndSourceOrderByUsername("email", nomis)
    verifyNoMoreInteractions(staffUserAccountRepository)
  }

  @Test
  fun `getNomisUsersByEmail users found`() {
    val joe = getNomisUser("JOE")
    val fred = getNomisUser("FRED")
    val harry = getNomisUser("HARRY")
    val bob = getNomisUser("BOB")
    whenever(staffUserAccountRepository.findAllNomisUsersByEmailAddress(anyString())).thenReturn(
      listOf(
        joe,
        fred,
        harry
      )
    )
    whenever(userRepository.findByEmailAndSourceOrderByUsername(anyString(), any())).thenReturn(
      listOf(getUserFromAuth("JOE"), getUserFromAuth("BOB"))
    )
    whenever(staffUserAccountRepository.findAllById(any())).thenReturn(listOf(joe, bob))
    assertThat(nomisUserService.getNomisUsersByEmail("email")).containsExactlyInAnyOrder(joe, fred, harry, bob)

    verify(staffUserAccountRepository).findAllById(listOf("JOE", "BOB"))
  }

  @Test
  fun `getNomisUsersByEmail ignore unverified auth`() {
    val user = User.builder().username("joe").source(nomis).verified(false).build()

    whenever(userRepository.findByEmailAndSourceOrderByUsername(anyString(), any())).thenReturn(listOf(user))

    assertThat(nomisUserService.getNomisUsersByEmail("email")).isEmpty()

    verify(staffUserAccountRepository).findAllNomisUsersByEmailAddress("email")
    verifyNoMoreInteractions(staffUserAccountRepository)
  }

  private fun getNomisUser(username: String): NomisUserPersonDetails =
    createSampleNomisUser(staff = Staff(firstName = "bob", status = "INACTIVE", lastName = "last", staffId = 5), username = username)

  private fun getUserFromAuth(username: String) = User.builder().username(username).source(nomis).verified(true).build()
}
