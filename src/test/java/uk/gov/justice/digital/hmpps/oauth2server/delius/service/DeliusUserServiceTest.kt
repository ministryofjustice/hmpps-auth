package uk.gov.justice.digital.hmpps.oauth2server.delius.service

import com.nhaarman.mockitokotlin2.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.config.DeliusRoleMappings
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.UserDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.UserRole
import uk.gov.justice.digital.hmpps.oauth2server.security.DeliusAuthenticationServiceException
import java.lang.RuntimeException

class DeliusUserServiceTest {
  private val restTemplate: RestTemplate = mock()
  private val mappings = DeliusRoleMappings(mapOf(
      Pair("arole", listOf("role1", "role2")),
      Pair("test.role", listOf("role1", "role3"))))
  private val disabledDeliusService = DeliusUserService(restTemplate, false, mappings)
  private val deliusService = DeliusUserService(restTemplate, true, mappings)

  @Test
  fun `getDeliusUserByEmail disabled`() {
    disabledDeliusService.getDeliusUserByEmail("a@where.com")
    verify(restTemplate, never()).getForObject<UserDetails>(anyString(), any(), anyString())
  }

  @Test
  fun `getDeliusUserByEmail enabled`() {
    deliusService.getDeliusUserByEmail("a@where.com")
    verify(restTemplate).getForObject<UserDetails>(anyString(), any(), anyString())
  }

  @Test
  fun `getDeliusUserByEmail multiple records returns null`() {
    whenever(restTemplate.getForObject<MutableList<UserDetails>>(anyString(), any(), anyString())).thenReturn(createUserDetailsList(2))
    val user = deliusService.getDeliusUserByEmail("a@where.com")
    assertThat(user).isNull()
  }

  @Test
  fun `getDeliusUserByEmail no records returns null`() {
    whenever(restTemplate.getForObject<MutableList<UserDetails>>(anyString(), any(), anyString())).thenReturn(createUserDetailsList(0))
    val user = deliusService.getDeliusUserByEmail("a@where.com")
    assertThat(user).isNull()
  }

  @Test
  fun `getDeliusUserByEmail single records returns user`() {
    whenever(restTemplate.getForObject<MutableList<UserDetails>>(anyString(), any(), anyString())).thenReturn(createUserDetailsList(1))
    val user = deliusService.getDeliusUserByEmail("a@where.com")
    assertThat(user).isEqualTo(
        DeliusUserPersonDetails(
            username = "DELIUSSMITH",
            userId = "12345",
            firstName = "Delius",
            surname = "Smith",
            email = "a@where.com",
            enabled = true,
            roles = emptySet()))
  }

  @Test
  fun `getDeliusUserByEmail converts ResourceAccessException and rethrows`() {
    whenever(restTemplate.getForObject<MutableList<UserDetails>>(anyString(), any(), anyString())).thenThrow(ResourceAccessException::class.java)

    assertThatThrownBy { deliusService.getDeliusUserByEmail("a@where.com") }.isInstanceOf(DeliusAuthenticationServiceException::class.java)
  }

  @Test
  fun `getDeliusUserByEmail converts HttpServerErrorException and rethrows`() {
    whenever(restTemplate.getForObject<MutableList<UserDetails>>(anyString(), any(), anyString())).thenThrow(HttpServerErrorException::class.java)

    assertThatThrownBy { deliusService.getDeliusUserByEmail("a@where.com") }.isInstanceOf(DeliusAuthenticationServiceException::class.java)
  }

  @Test
  fun `getDeliusUserByEmail handles HttpClientErrorException and returns null`() {
    whenever(restTemplate.getForObject<MutableList<UserDetails>>(anyString(), any(), anyString())).thenThrow(HttpClientErrorException::class.java)

    val user = deliusService.getDeliusUserByEmail("a@where.com")
    assertThat(user).isNull()
  }

  @Test
  fun `getDeliusUserByEmail handles random exceptions and returns null`() {
    whenever(restTemplate.getForObject<MutableList<UserDetails>>(anyString(), any(), anyString())).thenThrow(RuntimeException::class.java)

    val user = deliusService.getDeliusUserByEmail("a@where.com")
    assertThat(user).isNull()
  }

  @Test
  fun `deliusUserByUsername disabled`() {
    disabledDeliusService.getDeliusUserByUsername("DeliusSmith")
    verify(restTemplate, never()).getForObject<UserDetails>(anyString(), any(), anyString())
  }

  @Test
  fun `deliusUserByUsername enabled`() {
    deliusService.getDeliusUserByUsername("DeliusSmith")
    verify(restTemplate).getForObject<UserDetails>(anyString(), any(), anyString())
  }

  @Test
  fun `deliusUserByUsername test role mappings no roles granted`() {
    whenever(restTemplate.getForObject<UserDetails>(anyString(), any(), anyString())).thenReturn(createUserDetails())
    val optionalDetails = deliusService.getDeliusUserByUsername("DeliusSmith")
    assertThat(optionalDetails).get().isEqualTo(
        DeliusUserPersonDetails(
            username = "DELIUSSMITH",
            userId = "12345",
            firstName = "Delius",
            surname = "Smith",
            email = "a@where.com",
            enabled = true,
            roles = emptySet()))
  }

  @Test
  fun `deliusUserByUsername test role mappings`() {
    whenever(restTemplate.getForObject<UserDetails>(anyString(), any(), anyString())).thenReturn(
        createUserDetails().copy(roles = listOf(UserRole("AROLE"), UserRole("bob"))))
    val optionalDetails = deliusService.getDeliusUserByUsername("DeliusSmith")
    assertThat(optionalDetails).get().isEqualTo(
        DeliusUserPersonDetails(
            username = "DELIUSSMITH",
            userId = "12345",
            firstName = "Delius",
            surname = "Smith",
            email = "a@where.com",
            enabled = true,
            roles = setOf(SimpleGrantedAuthority("role1"), SimpleGrantedAuthority("role2"))))
  }

  @Test
  fun `deliusUserByUsername test role mappings multiple roles`() {
    whenever(restTemplate.getForObject<UserDetails>(anyString(), any(), anyString())).thenReturn(
        createUserDetails().copy(roles = listOf(UserRole("TEST_ROLE"), UserRole("AROLE"), UserRole("other"))))
    val optionalDetails = deliusService.getDeliusUserByUsername("DeliusSmith")
    assertThat(optionalDetails).get().isEqualTo(
        DeliusUserPersonDetails(
            username = "DELIUSSMITH",
            userId = "12345",
            firstName = "Delius",
            surname = "Smith",
            email = "a@where.com",
            enabled = true,
            roles = setOf(SimpleGrantedAuthority("role1"), SimpleGrantedAuthority("role2"), SimpleGrantedAuthority("role3"))))
  }

  @Test
  fun `deliusUserByUsername test username upper case`() {
    whenever(restTemplate.getForObject<UserDetails>(anyString(), any(), anyString())).thenReturn(createUserDetails())
    val optionalDetails = deliusService.getDeliusUserByUsername("DELIUSSMITH")
    assertThat(optionalDetails).get().isEqualTo(
        DeliusUserPersonDetails(
            username = "DELIUSSMITH",
            userId = "12345",
            firstName = "Delius",
            surname = "Smith",
            email = "a@where.com",
            enabled = true,
            roles = emptySet()))
  }

  @Test
  fun `deliusUserByUsername test email lower case`() {
    whenever(restTemplate.getForObject<UserDetails>(anyString(), any(), anyString())).thenReturn(
        createUserDetails().copy(email = "someWHERE@bob.COM"))
    val optionalDetails = deliusService.getDeliusUserByUsername("DeliusSmith")
    assertThat(optionalDetails).get().isEqualTo(
        DeliusUserPersonDetails(
            username = "DELIUSSMITH",
            userId = "12345",
            firstName = "Delius",
            surname = "Smith",
            email = "somewhere@bob.com",
            enabled = true,
            roles = emptySet()))
  }

  @Test
  fun `getDeliusUserByUsername converts ResourceAccessException and rethrows`() {
    whenever(restTemplate.getForObject<UserDetails>(anyString(), any(), anyString())).thenThrow(ResourceAccessException::class.java)

    assertThatThrownBy { deliusService.getDeliusUserByUsername("any_username") }.isInstanceOf(DeliusAuthenticationServiceException::class.java)
  }

  private fun createUserDetails(): UserDetails =
      UserDetails(userId = "12345", username = "DeliusSmith", surname = "Smith", firstName = "Delius", enabled = true, email = "a@where.com", roles = emptyList())

  private fun createUserDetailsList(size: Int): MutableList<UserDetails> {
      return MutableList(size) {
        UserDetails(userId = "12345", username = "DeliusSmith", surname = "Smith", firstName = "Delius", enabled = true, email = "a@where.com", roles = emptyList())
      }
  }

  @Test
  fun `authenticateUser disabled`() {
    disabledDeliusService.authenticateUser("user", "pass")
    verify(restTemplate, never()).postForEntity<DeliusUserService.AuthUser>(anyString(), any(), any())
  }

  @Test
  fun `authenticateUser enabled`() {
    deliusService.authenticateUser("user", "pass")
    verify(restTemplate).postForEntity<DeliusUserService.AuthUser>(anyString(), any(), any())
  }

  @Test
  fun `changePassword disabled`() {
    disabledDeliusService.changePassword("user", "pass")
    verify(restTemplate, never()).postForEntity<Void>(anyString(), any(), any(), anyString())
  }

  @Test
  fun `changePassword enabled`() {
    deliusService.changePassword("user", "pass")
    verify(restTemplate).postForEntity<Void>(anyString(), any(), any(), anyString())
  }
}
