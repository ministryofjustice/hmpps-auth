package uk.gov.justice.digital.hmpps.oauth2server.delius.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.oauth2server.config.DeliusRoleMappings
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.resource.CommunityApiMockServer
import uk.gov.justice.digital.hmpps.oauth2server.resource.DeliusExtension
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest
import uk.gov.justice.digital.hmpps.oauth2server.security.DeliusAuthenticationServiceException

@ExtendWith(DeliusExtension::class)
class DeliusUserServiceTest : IntegrationTest() {
  lateinit var communityApi: CommunityApiMockServer

  @Autowired
  @Qualifier("deliusWebClient")
  lateinit var webClient: WebClient

  @Autowired
  lateinit var objectMapper: ObjectMapper

  private lateinit var disabledDeliusService: DeliusUserService
  private lateinit var deliusService: DeliusUserService

  private val mappings = DeliusRoleMappings(
    mapOf(
      Pair("arole", listOf("role1", "role2")),
      Pair("test.role", listOf("role1", "role3"))
    )
  )

  @BeforeEach
  fun setUp() {
    disabledDeliusService = DeliusUserService(webClient, false, mappings)
    deliusService = DeliusUserService(webClient, true, mappings)
  }

  @Nested
  inner class GetDeliusUsersByEmail {
    @Test
    fun `getDeliusUsersByEmail disabled`() {
      disabledDeliusService.getDeliusUsersByEmail("a@where.com")
      communityApi.verify(0, anyRequestedFor(anyUrl()))
    }

    @Test
    fun `getDeliusUsersByEmail records returns all records`() {
      val user = deliusService.getDeliusUsersByEmail("multiple@where.com")
      assertThat(user).hasSize(2)
    }

    @Test
    fun `getDeliusUsersByEmail records returns mapped user`() {
      val user = deliusService.getDeliusUsersByEmail("single@where.com")
      assertThat(user).containsExactly(
        DeliusUserPersonDetails(
          username = "DELIUSSMITH",
          userId = "2500077027",
          firstName = "Delius",
          surname = "Smith",
          email = "single@where.com",
          enabled = true,
          roles = setOf(
            SimpleGrantedAuthority("role1"),
            SimpleGrantedAuthority("role3"),
          )
        )
      )
    }

    @Test
    fun `getDeliusUsersByEmail handles server error and rethrows`() {
      assertThatThrownBy { deliusService.getDeliusUsersByEmail("delius_server_error@where.com") }.isInstanceOf(
        DeliusAuthenticationServiceException::class.java
      )
    }

    @Test
    fun `getDeliusUsersByEmail handles client error and returns empty list`() {
      val user = deliusService.getDeliusUsersByEmail("delius_client_error@where.com")
      assertThat(user).isEmpty()
    }

    @Test
    fun `getDeliusUsersByEmail handles random exceptions and rethrows`() {
      assertThatThrownBy { deliusService.getDeliusUsersByEmail("delius_socket_error@where.com") }.isInstanceOf(
        DeliusAuthenticationServiceException::class.java
      )
    }
  }

  @Nested
  inner class GetDeliusUserByUsername {
    @Test
    fun `deliusUserByUsername disabled`() {
      disabledDeliusService.getDeliusUserByUsername("DeliusSmith")
      communityApi.verify(0, anyRequestedFor(anyUrl()))
    }

    @Test
    fun `deliusUserByUsername enabled`() {
      deliusService.getDeliusUserByUsername("DeliusSmith")
      communityApi.verify(getRequestedFor(urlEqualTo("/secure/users/DeliusSmith/details")))
    }

    @Test
    fun `deliusUserByUsername test role mappings no roles granted`() {
      val optionalDetails = deliusService.getDeliusUserByUsername("NO_ROLES")
      assertThat(optionalDetails).get().isEqualTo(
        DeliusUserPersonDetails(
          username = "NO_ROLES",
          userId = "2500077027",
          firstName = "Delius",
          surname = "Smith",
          email = "test@digital.justice.gov.uk",
          enabled = true,
          roles = emptySet()
        )
      )
    }

    @Test
    fun `deliusUserByUsername test role mappings`() {
      val optionalDetails = deliusService.getDeliusUserByUsername("DeliusSmith")
      assertThat(optionalDetails).get().isEqualTo(
        DeliusUserPersonDetails(
          username = "DELIUSSMITH",
          userId = "2500077027",
          firstName = "Delius",
          surname = "Smith",
          email = "test@digital.justice.gov.uk",
          enabled = true,
          roles = setOf(SimpleGrantedAuthority("role1"), SimpleGrantedAuthority("role3"))
        )
      )
    }

    @Test
    fun `deliusUserByUsername test username returned is upper cased`() {
      val optionalDetails = deliusService.getDeliusUserByUsername("deliussmith")
      assertThat(optionalDetails).get().isEqualTo(
        DeliusUserPersonDetails(
          username = "DELIUSSMITH",
          userId = "2500077027",
          firstName = "Delius",
          surname = "Smith",
          email = "test@digital.justice.gov.uk",
          enabled = true,
          roles = setOf(SimpleGrantedAuthority("role1"), SimpleGrantedAuthority("role3"))
        )
      )
    }

    @Test
    fun `deliusUserByUsername test email returned is lower cased`() {
      val optionalDetails = deliusService.getDeliusUserByUsername("DELIUS_MIXED_CASE")
      assertThat(optionalDetails).get().isEqualTo(
        DeliusUserPersonDetails(
          username = "DELIUS_MIXED_CASE",
          userId = "2500077027",
          firstName = "Delius",
          surname = "Smith",
          email = "test@digital.justice.gov.uk",
          enabled = true,
          roles = emptySet()
        )
      )
    }

    @Test
    fun `getDeliusUserByUsername returns empty optional if 404`() {
      val optionalDetails = deliusService.getDeliusUserByUsername("NON_EXISTENT")
      assertThat(optionalDetails).isEmpty
    }

    @Test
    fun `getDeliusUserByUsername returns empty optional if client error`() {
      val optionalDetails = deliusService.getDeliusUserByUsername("DELIUS_ERROR_CLIENT")
      assertThat(optionalDetails).isEmpty
    }

    @Test
    fun `getDeliusUserByUsername throws DeliusAuthenticationServiceException if server error`() {
      assertThatThrownBy { deliusService.getDeliusUserByUsername("DELIUS_ERROR_SERVER") }.isInstanceOf(
        DeliusAuthenticationServiceException::class.java
      )
    }

    @Test
    fun `getDeliusUserByUsername socket error results in DeliusAuthenticationServiceException`() {
      assertThatThrownBy { deliusService.getDeliusUserByUsername("DELIUS_SOCKET_ERROR") }.isInstanceOf(
        DeliusAuthenticationServiceException::class.java
      )
    }
  }

  @Nested
  inner class AuthenticateUser {

    @Test
    fun `authenticateUser disabled`() {
      disabledDeliusService.authenticateUser("user", "pass")
      communityApi.verify(0, anyRequestedFor(anyUrl()))
    }

    @Test
    fun `authenticateUser enabled`() {
      deliusService.authenticateUser("delius.smith", "pass")

      communityApi.verify(
        postRequestedFor(urlEqualTo("/secure/authenticate"))
          .withRequestBody(equalToJson("""{"username" : "delius.smith", "password" : "pass"}"""))
      )
    }
  }

  @Nested
  inner class ChangePassword {
    @Test
    fun `changePassword disabled`() {
      disabledDeliusService.changePassword("user", "pass")
      communityApi.verify(0, anyRequestedFor(anyUrl()))
    }

    @Test
    fun `changePassword enabled`() {
      deliusService.changePassword("DELIUS_PASSWORD_RESET", "helloworld2")
      communityApi.verify(
        postRequestedFor(urlEqualTo("/secure/users/DELIUS_PASSWORD_RESET/password"))
          .withRequestBody(equalToJson("""{"password" : "helloworld2"}"""))
      )
    }
  }
}
