package uk.gov.justice.digital.hmpps.oauth2server.delius.service

import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.config.DeliusRoleMappings
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.UserDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.UserRole
import uk.gov.justice.digital.hmpps.oauth2server.security.DeliusAuthenticationServiceException

@RunWith(MockitoJUnitRunner::class)
class DeliusUserServiceTest {
  private val restTemplate: RestTemplate = mock()
  private lateinit var disabledDeliusService: DeliusUserService
  private lateinit var deliusService: DeliusUserService
  private val mappings = DeliusRoleMappings(mapOf(
      Pair("arole", listOf("role1", "role2")),
      Pair("test.role", listOf("role1", "role3"))))

  @Before
  fun before() {
    disabledDeliusService = DeliusUserService(restTemplate, false, mappings)
    deliusService = DeliusUserService(restTemplate, true, mappings)
  }

  @Test
  fun `deliusUserByUsername disabled`() {
    disabledDeliusService.getDeliusUserByUsername("bob")
    verify(restTemplate, never()).getForObject<UserDetails>(anyString(), any(), anyString())
  }

  @Test
  fun `deliusUserByUsername enabled`() {
    deliusService.getDeliusUserByUsername("bob")
    verify(restTemplate).getForObject<UserDetails>(anyString(), any(), anyString())
  }

  @Test
  fun `deliusUserByUsername test role mappings no roles granted`() {
    whenever(restTemplate.getForObject<UserDetails>(anyString(), any(), anyString())).thenReturn(createUserDetails())
    val optionalDetails = deliusService.getDeliusUserByUsername("bob")
    assertThat(optionalDetails).get().isEqualTo(
        DeliusUserPersonDetails(
            username = "BOB",
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
        createUserDetails().copy(roles = listOf(UserRole("AROLE", "desc 1"), UserRole("bob", "desc 2"))))
    val optionalDetails = deliusService.getDeliusUserByUsername("bob")
    assertThat(optionalDetails).get().isEqualTo(
        DeliusUserPersonDetails(
            username = "BOB",
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
        createUserDetails().copy(roles = listOf(UserRole("TEST_ROLE", "desc 1"), UserRole("AROLE", "desc 2"), UserRole("other", "other desc"))))
    val optionalDetails = deliusService.getDeliusUserByUsername("bob")
    assertThat(optionalDetails).get().isEqualTo(
        DeliusUserPersonDetails(
            username = "BOB",
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
    val optionalDetails = deliusService.getDeliusUserByUsername("bob")
    assertThat(optionalDetails).get().isEqualTo(
        DeliusUserPersonDetails(
            username = "BOB",
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
    val optionalDetails = deliusService.getDeliusUserByUsername("BoB")
    assertThat(optionalDetails).get().isEqualTo(
        DeliusUserPersonDetails(
            username = "BOB",
            userId = "12345",
            firstName = "Delius",
            surname = "Smith",
            email = "somewhere@bob.com",
            enabled = true,
            roles = emptySet()))
  }

  @Test(expected = DeliusAuthenticationServiceException::class)
  fun `getDeliusUserByUsername converts ResourceAccessException and rethrows`() {
    whenever(restTemplate.getForObject<UserDetails>(anyString(), any(), anyString())).thenThrow(ResourceAccessException::class.java)

    deliusService.getDeliusUserByUsername("any_username")
  }

  private fun createUserDetails(): UserDetails =
      UserDetails(userId = "12345", surname = "Smith", firstName = "Delius", enabled = true, email = "a@where.com", roles = emptyList())

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
