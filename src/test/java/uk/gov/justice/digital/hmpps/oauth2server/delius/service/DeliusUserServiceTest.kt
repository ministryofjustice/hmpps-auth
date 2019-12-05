package uk.gov.justice.digital.hmpps.oauth2server.delius.service

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.web.client.RestTemplate
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.UserDetails

@RunWith(MockitoJUnitRunner::class)
class DeliusUserServiceTest {
  private val restTemplate: RestTemplate = mock()
  private lateinit var disabledDeliusService: DeliusUserService
  private lateinit var deliusService: DeliusUserService

  @Before
  fun before() {
    disabledDeliusService = DeliusUserService(restTemplate, false)
    deliusService = DeliusUserService(restTemplate, true)
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
    verify(restTemplate, never()).postForEntity<Void>(anyString(), any(), any(), any())
  }

  @Test
  fun `changePassword enabled`() {
    deliusService.changePassword("user", "pass")
    verify(restTemplate).postForEntity<Void>(anyString(), any(), any(), any())
  }
}
