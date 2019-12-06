package uk.gov.justice.digital.hmpps.oauth2server.delius.service

import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.config.DeliusRoleMappings
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.UserDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.UserRole

@RunWith(MockitoJUnitRunner::class)
class DeliusUserServiceTest {
  private val restTemplate: RestTemplate = mock()
  private lateinit var disabledDeliusService: DeliusUserService
  private lateinit var deliusService: DeliusUserService
  private val mappings = DeliusRoleMappings(mapOf(
      Pair("joe", listOf("role1", "role2")),
      Pair("fred", listOf("role1", "role3"))))

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
            surname = "Smith",
            firstName = "Delius",
            enabled = true,
            email = "a@where.com",
            username = "bob",
            roles = emptySet()))
  }

  @Test
  fun `deliusUserByUsername test role mappings`() {
    whenever(restTemplate.getForObject<UserDetails>(anyString(), any(), anyString())).thenReturn(
        createUserDetails().copy(roles = listOf(UserRole("joe", "desc 1"), UserRole("bob", "desc 2"))))
    val optionalDetails = deliusService.getDeliusUserByUsername("bob")
    assertThat(optionalDetails).get().isEqualTo(
        DeliusUserPersonDetails(
            surname = "Smith",
            firstName = "Delius",
            enabled = true,
            email = "a@where.com",
            username = "bob",
            roles = setOf(SimpleGrantedAuthority("role1"), SimpleGrantedAuthority("role2"))))
  }

  @Test
  fun `deliusUserByUsername test role mappings multiple roles`() {
    whenever(restTemplate.getForObject<UserDetails>(anyString(), any(), anyString())).thenReturn(
        createUserDetails().copy(roles = listOf(UserRole("fred", "desc 1"), UserRole("joe", "desc 2"), UserRole("other", "other desc"))))
    val optionalDetails = deliusService.getDeliusUserByUsername("bob")
    assertThat(optionalDetails).get().isEqualTo(
        DeliusUserPersonDetails(
            surname = "Smith",
            firstName = "Delius",
            enabled = true,
            email = "a@where.com",
            username = "bob",
            roles = setOf(SimpleGrantedAuthority("role1"), SimpleGrantedAuthority("role2"), SimpleGrantedAuthority("role3"))))
  }

  private fun createUserDetails(): UserDetails =
      UserDetails(surname = "Smith", firstName = "Delius", enabled = true, email = "a@where.com", roles = emptyList())

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
