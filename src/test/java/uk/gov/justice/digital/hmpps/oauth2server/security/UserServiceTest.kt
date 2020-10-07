package uk.gov.justice.digital.hmpps.oauth2server.security

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.core.userdetails.UsernameNotFoundException
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Contact
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.ContactType
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.Person
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.azure.service.AzureUserService
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.service.DeliusUserService
import uk.gov.justice.digital.hmpps.oauth2server.maintain.AuthUserService
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.AccountDetail
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff
import uk.gov.justice.digital.hmpps.oauth2server.verify.VerifyEmailService
import java.util.*

@Suppress("UsePropertyAccessSyntax")
class UserServiceTest {
  private val nomisUserService: NomisUserService = mock()
  private val authUserService: AuthUserService = mock()
  private val deliusUserService: DeliusUserService = mock()
  private val azureUserService: AzureUserService = mock()
  private val userRepository: UserRepository = mock()
  private val verifyEmailService: VerifyEmailService = mock()
  private val userService = UserService(nomisUserService, authUserService, deliusUserService, azureUserService, userRepository, verifyEmailService)

  @Nested
  inner class FindMasterUserPersonDetails {
    @Test
    fun `findMasterUserPersonDetails auth user`() {
      whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(createUser())
      val user = userService.findMasterUserPersonDetails("   bob   ")
      assertThat(user).isPresent.get().extracting { it.username }.isEqualTo("someuser")
    }

    @Test
    fun `findMasterUserPersonDetails nomis user`() {
      whenever(nomisUserService.getNomisUserByUsername(anyString())).thenReturn(staffUserAccountForBob)
      val user = userService.findMasterUserPersonDetails("bob")
      assertThat(user).isPresent.get().extracting { it.username }.isEqualTo("nomisuser")
    }

    @Test
    fun `findMasterUserPersonDetails delius user`() {
      whenever(deliusUserService.getDeliusUserByUsername(anyString())).thenReturn(deliusUserAccountForBob)
      val user = userService.findMasterUserPersonDetails("bob")
      assertThat(user).isPresent.get().extracting { it.username }.isEqualTo("deliusUser")
    }
  }

  @Nested
  inner class GetEmailAddressFromNomis {
    @Test
    fun `getEmailAddressFromNomis no email addresses`() {
      whenever(verifyEmailService.getExistingEmailAddressesForUsername(any())).thenReturn(listOf())
      val optionalAddress = userService.getEmailAddressFromNomis("joe")
      assertThat(optionalAddress).isEmpty
    }

    @Test
    fun `getEmailAddressFromNomis not a justice email`() {
      whenever(verifyEmailService.getExistingEmailAddressesForUsername(any())).thenReturn(listOf("a@b.gov.uk"))
      val optionalAddress = userService.getEmailAddressFromNomis("joe")
      assertThat(optionalAddress).isEmpty
    }

    @Test
    fun `getEmailAddressFromNomis one justice email`() {
      whenever(verifyEmailService.getExistingEmailAddressesForUsername(any())).thenReturn(listOf("Bob.smith@hmps.gsi.gov.uk", "Bob.smith@justice.gov.uk"))
      val optionalAddress = userService.getEmailAddressFromNomis("joe")
      assertThat(optionalAddress).hasValue("Bob.smith@justice.gov.uk")
    }

    @Test
    fun `getEmailAddressFromNomis multiple justice emails`() {
      whenever(verifyEmailService.getExistingEmailAddressesForUsername(any())).thenReturn(listOf(
          "Bob.smith@hmps.gsi.gov.uk",
          "Bob.smith@justice.gov.uk",
          "Bob.smith2@justice.gov.uk"
      ))
      val optionalAddress = userService.getEmailAddressFromNomis("joe")
      assertThat(optionalAddress).isEmpty
    }
  }

  @Nested
  inner class GetOrCreateUser {

    @Test
    fun `getOrCreateUser user exists already`() {
      val user = User.of("joe")
      whenever(userRepository.findByUsername("JOE")).thenReturn(Optional.of(user))
      val newUserOpt = userService.getOrCreateUser("joe")
      assertThat(newUserOpt).hasValueSatisfying {
        assertThat(it.username).isEqualTo("joe")
      }
    }

    @Test
    fun `getOrCreateUser migrate from NOMIS`() {
      whenever(userRepository.findByUsername(any())).thenReturn(Optional.empty())
      whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.empty())
      whenever(nomisUserService.getNomisUserByUsername("joe")).thenReturn(Optional.of(NomisUserPersonDetails.builder().username("joe").build()))
      whenever(verifyEmailService.getExistingEmailAddressesForUsername(any())).thenReturn(listOf())
      whenever(userRepository.save<User>(any())).thenAnswer { it.getArguments()[0] }

      val newUser = userService.getOrCreateUser("joe")
      assertThat(newUser).hasValueSatisfying {
        assertThat(it.username).isEqualTo("joe")
      }
    }

    @Test
    fun `getOrCreateUser migrate from NOMIS with email`() {
      whenever(userRepository.findByUsername(any())).thenReturn(Optional.empty())
      whenever(authUserService.getAuthUserByUsername(anyString())).thenReturn(Optional.empty())
      whenever(nomisUserService.getNomisUserByUsername("joe")).thenReturn(Optional.of(NomisUserPersonDetails.builder().username("joe").build()))
      whenever(verifyEmailService.getExistingEmailAddressesForUsername(any())).thenReturn(listOf("a@b.justice.gov.uk"))
      whenever(userRepository.save<User>(any())).thenAnswer { it.getArguments()[0] }

      val newUser = userService.getOrCreateUser("joe")
      assertThat(newUser).hasValueSatisfying {
        assertThat(it.username).isEqualTo("joe")
        assertThat(it.email).isEqualTo("a@b.justice.gov.uk")
        assertThat(it.isVerified).isTrue()
        assertThat(it.authSource).isEqualTo(AuthSource.nomis.name)
      }
    }
  }

  @Nested
  inner class FindUser {
    @Test
    fun findUser() {
      val user = createUser()
      whenever(userRepository.findByUsername(anyString())).thenReturn(user)
      val found = userService.findUser("bob")
      assertThat(found).isSameAs(user)
      verify(userRepository).findByUsername("BOB")
    }
  }


  @Nested
  inner class GetUser {
    @Test
    fun `getUser found`() {
      val user = createUser()
      whenever(userRepository.findByUsername(anyString())).thenReturn(user)
      val found = userService.getUser("bob")
      assertThat(found).isSameAs(user.orElseThrow())
      verify(userRepository).findByUsername("BOB")
    }

    @Test
    fun `getUser not found`() {
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.empty())
      assertThatThrownBy { userService.getUser("bob") }.isInstanceOf(UsernameNotFoundException::class.java)
    }
  }

  @Nested
  inner class HasVerifiedMfaMethod {
    @Test
    fun `hasVerifiedMfaMethod success`() {
      val user = User.builder().username("joe").email("someemail").verified(true).build()
      assertThat(userService.hasVerifiedMfaMethod(user)).isTrue()
    }

    @Test
    fun `hasVerifiedMfaMethod no email`() {
      val user = User.builder().username("joe").verified(true).build()
      assertThat(userService.hasVerifiedMfaMethod(user)).isFalse()
    }

    @Test
    fun `hasVerifiedMfaMethod not verified`() {
      val user = User.builder().username("joe").email("someemail").build()
      assertThat(userService.hasVerifiedMfaMethod(user)).isFalse()
    }
  }

  @Nested
  inner class IsSameAsCurrentVerifiedMobile {

    @Test
    fun `isSameAsCurrentVerifiedMobile not verified`() {
      val user = User.builder().mobile("07700900001").mobileVerified(false).build()
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      val returnValue = userService.isSameAsCurrentVerifiedMobile("someuser", "")
      assertThat(returnValue).isFalse()
    }

    @Test
    fun `isSameAsCurrentVerifiedMobile new different mobile number`() {
      val user = User.builder().mobile("07700900001").mobileVerified(true).build()
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      val returnValue = userService.isSameAsCurrentVerifiedMobile("someuser", "07700900000")
      assertThat(returnValue).isFalse()
    }

    @Test
    fun `isSameAsCurrentVerifiedMobile new different mobile number with whitespace`() {
      val user = User.builder().mobile("07700900001").mobileVerified(true).build()
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      val returnValue = userService.isSameAsCurrentVerifiedMobile("someuser", "0770 090 0000")
      assertThat(returnValue).isFalse()
    }

    @Test
    fun `isSameAsCurrentVerifiedMobile same mobile number`() {
      val user = User.builder().mobile("07700900000").mobileVerified(true).build()
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      val returnValue = userService.isSameAsCurrentVerifiedMobile("someuser", "07700900000")
      assertThat(returnValue).isTrue()
    }

    @Test
    fun `isSameAsCurrentVerifiedMobile same mobile number with whitespace`() {
      val user = User.builder().mobile("07700900000").mobileVerified(true).build()
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      val returnValue = userService.isSameAsCurrentVerifiedMobile("someuser", "0770 090 0000")
      assertThat(returnValue).isTrue()
    }
  }

  @Nested
  inner class isSameAsCurrentVerifiedEmail {
    @Test
    fun `isSameAsCurrentVerifiedEmail not verified primary email`() {
      val user = User.builder().email("someemail").verified(false).build()
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      val returnValue = userService.isSameAsCurrentVerifiedEmail("someuser", "someemail", User.EmailType.PRIMARY)
      assertThat(returnValue).isFalse()
    }

    @Test
    fun `isSameAsCurrentVerifiedEmail new different email address primary email`() {
      val user = User.builder().email("someemail").verified(true).build()
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      val returnValue = userService.isSameAsCurrentVerifiedEmail("someuser", "somenewemail", User.EmailType.PRIMARY)
      assertThat(returnValue).isFalse()
    }

    @Test
    fun `isSameAsCurrentVerifiedEmail same address primary email`() {
      val user = User.builder().email("someemail").verified(true).build()
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      val returnValue = userService.isSameAsCurrentVerifiedEmail("someuser", "someemail", User.EmailType.PRIMARY)
      assertThat(returnValue).isTrue()
    }

    @Test
    fun `isSameAsCurrentVerifiedEmail not verified secondary email`() {
      val user = User.builder().contacts(setOf(Contact(ContactType.SECONDARY_EMAIL, "someemail", false))).build()
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      val returnValue = userService.isSameAsCurrentVerifiedEmail("someuser", "someemail", User.EmailType.SECONDARY)
      assertThat(returnValue).isFalse()
    }

    @Test
    fun `isSameAsCurrentVerifiedEmail new different email address secondary email`() {
      val user = User.builder().contacts(setOf(Contact(ContactType.SECONDARY_EMAIL, "someemail", true))).build()
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      val returnValue = userService.isSameAsCurrentVerifiedEmail("someuser", "somenewemail", User.EmailType.SECONDARY)
      assertThat(returnValue).isFalse()
    }

    @Test
    fun `isSameAsCurrentVerifiedEmail same address secondary email`() {
      val user = User.builder().contacts(setOf(Contact(ContactType.SECONDARY_EMAIL, "someemail", true))).build()
      whenever(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user))
      val returnValue = userService.isSameAsCurrentVerifiedEmail("someuser", "someemail", User.EmailType.SECONDARY)
      assertThat(returnValue).isTrue()
    }
  }

  @Nested
  inner class FindPrisonUsersByFirstAndLastNames {
    @Test
    fun `no matches`() {
      whenever(nomisUserService.findPrisonUsersByFirstAndLastNames("first", "last")).thenReturn(listOf())
      whenever(authUserService.findAuthUsersByUsernames(listOf())).thenReturn(listOf())
      whenever(verifyEmailService.getExistingEmailAddressesForUsernames(listOf())).thenReturn(mapOf())

      assertThat(userService.findPrisonUsersByFirstAndLastNames("first", "last")).isEmpty()
    }

    @Test
    fun `prison users only`() {
      val staff = Staff.builder().firstName("first").lastName("last").build()

      whenever(authUserService.findAuthUsersByUsernames(listOf())).thenReturn(listOf())

      val updBuilder = NomisUserPersonDetails.builder().staff(staff)

      whenever(nomisUserService.findPrisonUsersByFirstAndLastNames("first", "last")).thenReturn(
          listOf(
              updBuilder.username("U1").build(),
              updBuilder.username("U2").build(),
              updBuilder.username("U3").build(),
          ))

      whenever(verifyEmailService.getExistingEmailAddressesForUsernames(anyList()))
          .thenReturn(
              mapOf(
                  Pair("U1", setOf("u1@justice.gov.uk", "u1@somethingelse.gov.uk")),

                  // Two matching e-mail suffixes results in no e-mail address.
                  Pair("U3", setOf("u3@justice.gov.uk", "another-u3@justice.gov.uk"))
              ))

      assertThat(userService.findPrisonUsersByFirstAndLastNames("first", "last"))
          .extracting("username", "email", "verified", "person.firstName", "person.lastName")
          .containsExactlyInAnyOrder(
              tuple("U1", "u1@justice.gov.uk", true, "First", "Last"),
              tuple("U2", null, false, "First", "Last"),
              tuple("U3", null, false, "First", "Last")
          )
      verify(verifyEmailService).getExistingEmailAddressesForUsernames(listOf("U1", "U2", "U3"))
    }

    @Test
    fun `Prison users matched in auth`() {
      val updBuilder = NomisUserPersonDetails
          .builder()
          .staff(Staff
              .builder()
              .firstName("xxx")
              .lastName("xxx")
              .build())

      whenever(nomisUserService.findPrisonUsersByFirstAndLastNames("first", "last")).thenReturn(
          listOf(
              updBuilder.username("U1").build(),
              updBuilder.username("U2").build(),
              updBuilder.username("U3").build(),
          ))

      val userBuilder = User
          .builder()
          .verified(true)
          .person(Person("First", "Last"))
          .source(AuthSource.nomis)

      whenever(authUserService.findAuthUsersByUsernames(anyList())).thenReturn(
          listOf(
              userBuilder.username("U1").email("u1@b.com").build(),
              userBuilder.username("U2").email("u2@b.com").build(),
              userBuilder.username("U3").email("u3@b.com").verified(false).build()
          ))

      assertThat(userService.findPrisonUsersByFirstAndLastNames("first", "last"))
          .extracting("username", "email", "verified", "person.firstName", "person.lastName")
          .containsExactlyInAnyOrder(
              tuple("U1", "u1@b.com", true, "First", "Last"),
              tuple("U2", "u2@b.com", true, "First", "Last"),
              tuple("U3", "u3@b.com", false, "First", "Last")
          )

      verify(verifyEmailService).getExistingEmailAddressesForUsernames(listOf())
    }

    @Test
    fun `Prison users partially matched in auth`() {
      val updBuilder = NomisUserPersonDetails
          .builder()
          .staff(Staff
              .builder()
              .firstName("NFirst")
              .lastName("NLast")
              .build())

      whenever(nomisUserService.findPrisonUsersByFirstAndLastNames("first", "last")).thenReturn(
          listOf(
              updBuilder.username("U1").build(),
              updBuilder.username("U2").build(),
              updBuilder.username("U3").build(),
              updBuilder.username("U4").build(),
          ))

      val userBuilder = User
          .builder()
          .verified(true)
          .person(Person("AFirst", "ALast"))
          .source(AuthSource.nomis)

      whenever(authUserService.findAuthUsersByUsernames(anyList())).thenReturn(
          listOf(
              userBuilder.username("U1").email("u1@b.com").build(),
              // User U2 in auth, but no email - so search NOMIS for e-mail for this user
              userBuilder.username("U2").email(null).build(),
              // User U3 found in auth, but source is not nomis
              userBuilder.username("U3").email("u3@b.com").source(AuthSource.auth).build()
          ))

      whenever(verifyEmailService.getExistingEmailAddressesForUsernames(anyList()))
          .thenReturn(
              mapOf(
                  Pair("U2", setOf("u2@justice.gov.uk", "u2@somethingelse.gov.uk")),
                  Pair("U3", setOf("u3@justice.gov.uk")),
              ))



      assertThat(userService.findPrisonUsersByFirstAndLastNames("first", "last"))
          .extracting("username", "email", "verified", "person.firstName", "person.lastName")
          .containsExactlyInAnyOrder(
              // e-mail from auth service user
              tuple("U1", "u1@b.com", true, "AFirst", "ALast"),

              // U2 found in auth has correct auth source but no email.  Therefore take email from NOMIS. Is this right?
              tuple("U2", "u2@justice.gov.uk", true, "Nfirst", "Nlast"),

              // U3 from NOMIS - matching auth user has wrong auth source.  Is this right?
              tuple("U3", "u3@justice.gov.uk", true, "Nfirst", "Nlast"),


              tuple("U4", null, false, "Nfirst", "Nlast")
          )

      verify(verifyEmailService).getExistingEmailAddressesForUsernames(listOf("U2", "U3", "U4"))
    }
  }

  private fun createUser() = Optional.of(User.of("someuser"))

  private val staffUserAccountForBob: Optional<NomisUserPersonDetails>
    get() {
      val staffUserAccount = NomisUserPersonDetails()
      staffUserAccount.username = "nomisuser"
      val staff = Staff()
      staff.firstName = "bOb"
      staff.status = "ACTIVE"
      staffUserAccount.staff = staff
      val detail = AccountDetail("user", "OPEN", "profile", null)
      staffUserAccount.accountDetail = detail
      return Optional.of(staffUserAccount)
    }

  private val deliusUserAccountForBob =
      Optional.of(DeliusUserPersonDetails("deliusUser", "12345", "Delius", "Smith", "a@b.com", true, false, setOf()))
}
