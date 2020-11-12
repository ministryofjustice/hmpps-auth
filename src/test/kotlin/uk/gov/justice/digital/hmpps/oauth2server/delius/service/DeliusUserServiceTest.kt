package uk.gov.justice.digital.hmpps.oauth2server.delius.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.oauth2server.config.DeliusRoleMappings
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.DeliusUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.UserDetails
import uk.gov.justice.digital.hmpps.oauth2server.delius.model.UserRole
import uk.gov.justice.digital.hmpps.oauth2server.resource.CommunityApiMockServer
import uk.gov.justice.digital.hmpps.oauth2server.resource.DeliusExtension
import uk.gov.justice.digital.hmpps.oauth2server.resource.IntegrationTest
import uk.gov.justice.digital.hmpps.oauth2server.security.DeliusAuthenticationServiceException

@ExtendWith(DeliusExtension::class)
class DeliusUserServiceTest : IntegrationTest() {
  lateinit var communityApi : CommunityApiMockServer

  @Autowired
  @Qualifier("deliusWebClient")
  lateinit var webClient : WebClient;

  @Autowired
  lateinit var objectMapper: ObjectMapper

  private lateinit var disabledDeliusService : DeliusUserService
  private lateinit var deliusService : DeliusUserService

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
      communityApi.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    fun `getDeliusUsersByEmail enabled`() {
      deliusService.getDeliusUsersByEmail("a@where.com")
      verify(webClient).get()
    }

    @Test
    fun `getDeliusUsersByEmail records returns all records`() {
//      whenever(restTemplate.getForObject<MutableList<UserDetails>>(anyString(), any(), anyString())).thenReturn(
//        createUserDetailsList(2)
//      )
      communityApi.stubFor(get("/secure/users/search/email/a%40where.com/details")
                  .willReturn(aResponse().withBody(objectMapper.writeValueAsString(createUserDetailsList(2)))))


      val user = deliusService.getDeliusUsersByEmail("a@where.com")
      assertThat(user).hasSize(2)
    }
//
//    @Test
//    fun `getDeliusUsersByEmail records returns mapped user`() {
//      whenever(restTemplate.getForObject<DeliusUserList>(anyString(), any(), anyString())).thenReturn(
//        createUserDetailsList(1)
//      )
//      val user = deliusService.getDeliusUsersByEmail("a@where.com")
//      assertThat(user).containsExactly(
//        DeliusUserPersonDetails(
//          username = "DELIUSSMITH",
//          userId = "12345",
//          firstName = "Delius",
//          surname = "Smith",
//          email = "a@where.com",
//          enabled = true,
//          roles = emptySet()
//        )
//      )
//    }
//
//    @Test
//    fun `getDeliusUsersByEmail converts ResourceAccessException and rethrows`() {
//      whenever(restTemplate.getForObject<DeliusUserList>(anyString(), any(), anyString())).thenThrow(
//        ResourceAccessException::class.java
//      )
//
//      assertThatThrownBy { deliusService.getDeliusUsersByEmail("a@where.com") }.isInstanceOf(
//        DeliusAuthenticationServiceException::class.java
//      )
//    }
//
//    @Test
//    fun `getDeliusUsersByEmail converts HttpServerErrorException and rethrows`() {
//      whenever(restTemplate.getForObject<DeliusUserList>(anyString(), any(), anyString())).thenThrow(
//        HttpServerErrorException::class.java
//      )
//
//      assertThatThrownBy { deliusService.getDeliusUsersByEmail("a@where.com") }.isInstanceOf(
//        DeliusAuthenticationServiceException::class.java
//      )
//    }
//
//    @Test
//    fun `getDeliusUsersByEmail handles HttpClientErrorException and returns empty list`() {
//      whenever(restTemplate.getForObject<DeliusUserList>(anyString(), any(), anyString())).thenThrow(
//        HttpClientErrorException::class.java
//      )
//
//      val user = deliusService.getDeliusUsersByEmail("a@where.com")
//      assertThat(user).isEmpty()
//    }
//
//    @Test
//    fun `getDeliusUsersByEmail handles random exceptions and returns empty list`() {
//      whenever(
//        restTemplate.getForObject<DeliusUserList>(
//          anyString(),
//          any(),
//          anyString()
//        )
//      ).thenThrow(RuntimeException::class.java)
//
//      val user = deliusService.getDeliusUsersByEmail("a@where.com")
//      assertThat(user).isEmpty()
//    }
//  }
//
//  @Nested
//  inner class GetDeliusUserByUsername {
//    @Test
//    fun `deliusUserByUsername disabled`() {
//      disabledDeliusService.getDeliusUserByUsername("DeliusSmith")
//      verifyZeroInteractions(restTemplate)
//    }
//
//    @Test
//    fun `deliusUserByUsername enabled`() {
//      deliusService.getDeliusUserByUsername("DeliusSmith")
//      verify(restTemplate).getForObject<UserDetails>(anyString(), any(), anyString())
//    }
//
//    @Test
//    fun `deliusUserByUsername test role mappings no roles granted`() {
//      whenever(restTemplate.getForObject<UserDetails>(anyString(), any(), anyString())).thenReturn(createUserDetails())
//      val optionalDetails = deliusService.getDeliusUserByUsername("DeliusSmith")
//      assertThat(optionalDetails).get().isEqualTo(
//        DeliusUserPersonDetails(
//          username = "DELIUSSMITH",
//          userId = "12345",
//          firstName = "Delius",
//          surname = "Smith",
//          email = "a@where.com",
//          enabled = true,
//          roles = emptySet()
//        )
//      )
//    }
//
//    @Test
//    fun `deliusUserByUsername test role mappings`() {
//      whenever(restTemplate.getForObject<UserDetails>(anyString(), any(), anyString())).thenReturn(
//        createUserDetails().copy(roles = listOf(UserRole("AROLE"), UserRole("bob")))
//      )
//      val optionalDetails = deliusService.getDeliusUserByUsername("DeliusSmith")
//      assertThat(optionalDetails).get().isEqualTo(
//        DeliusUserPersonDetails(
//          username = "DELIUSSMITH",
//          userId = "12345",
//          firstName = "Delius",
//          surname = "Smith",
//          email = "a@where.com",
//          enabled = true,
//          roles = setOf(SimpleGrantedAuthority("role1"), SimpleGrantedAuthority("role2"))
//        )
//      )
//    }
//
//    @Test
//    fun `deliusUserByUsername test role mappings multiple roles`() {
//      whenever(restTemplate.getForObject<UserDetails>(anyString(), any(), anyString())).thenReturn(
//        createUserDetails().copy(roles = listOf(UserRole("TEST_ROLE"), UserRole("AROLE"), UserRole("other")))
//      )
//      val optionalDetails = deliusService.getDeliusUserByUsername("DeliusSmith")
//      assertThat(optionalDetails).get().isEqualTo(
//        DeliusUserPersonDetails(
//          username = "DELIUSSMITH",
//          userId = "12345",
//          firstName = "Delius",
//          surname = "Smith",
//          email = "a@where.com",
//          enabled = true,
//          roles = setOf(
//            SimpleGrantedAuthority("role1"),
//            SimpleGrantedAuthority("role2"),
//            SimpleGrantedAuthority("role3")
//          )
//        )
//      )
//    }
//
//    @Test
//    fun `deliusUserByUsername test username upper case`() {
//      whenever(restTemplate.getForObject<UserDetails>(anyString(), any(), anyString())).thenReturn(createUserDetails())
//      val optionalDetails = deliusService.getDeliusUserByUsername("DELIUSSMITH")
//      assertThat(optionalDetails).get().isEqualTo(
//        DeliusUserPersonDetails(
//          username = "DELIUSSMITH",
//          userId = "12345",
//          firstName = "Delius",
//          surname = "Smith",
//          email = "a@where.com",
//          enabled = true,
//          roles = emptySet()
//        )
//      )
//    }
//
//    @Test
//    fun `deliusUserByUsername test email lower case`() {
//      whenever(restTemplate.getForObject<UserDetails>(anyString(), any(), anyString())).thenReturn(
//        createUserDetails().copy(email = "someWHERE@bob.COM")
//      )
//      val optionalDetails = deliusService.getDeliusUserByUsername("DeliusSmith")
//      assertThat(optionalDetails).get().isEqualTo(
//        DeliusUserPersonDetails(
//          username = "DELIUSSMITH",
//          userId = "12345",
//          firstName = "Delius",
//          surname = "Smith",
//          email = "somewhere@bob.com",
//          enabled = true,
//          roles = emptySet()
//        )
//      )
//    }
//
//    @Test
//    fun `getDeliusUserByUsername converts ResourceAccessException and rethrows`() {
//      whenever(restTemplate.getForObject<UserDetails>(anyString(), any(), anyString())).thenThrow(
//        ResourceAccessException::class.java
//      )
//
//      assertThatThrownBy { deliusService.getDeliusUserByUsername("any_username") }.isInstanceOf(
//        DeliusAuthenticationServiceException::class.java
//      )
//    }
//  }
//
  private fun createUserDetails(): UserDetails =
    UserDetails(
      userId = "12345",
      username = "DeliusSmith",
      surname = "Smith",
      firstName = "Delius",
      enabled = true,
      email = "a@where.com",
      roles = emptyList()
    )

  private fun createUserDetailsList(size: Int): DeliusUserList {
    val users = DeliusUserList()
    users.addAll(
      MutableList(size) {
        UserDetails(
          userId = "12345",
          username = "DeliusSmith",
          surname = "Smith",
          firstName = "Delius",
          enabled = true,
          email = "a@where.com",
          roles = emptyList()
        )
      }
    )
    return users
  }
//
//  @Nested
//  inner class AuthenticateUser {
//
//    @Test
//    fun `authenticateUser disabled`() {
//      disabledDeliusService.authenticateUser("user", "pass")
//      verifyZeroInteractions(restTemplate)
//    }
//
//    @Test
//    fun `authenticateUser enabled`() {
//      deliusService.authenticateUser("user", "pass")
//      verify(restTemplate).postForEntity<DeliusUserService.AuthUser>(anyString(), any(), any())
//    }
//  }
//
//  @Nested
//  inner class ChangePassword {
//    @Test
//    fun `changePassword disabled`() {
//      disabledDeliusService.changePassword("user", "pass")
//      verifyZeroInteractions(restTemplate)
//    }
//
//    @Test
//    fun `changePassword enabled`() {
//      deliusService.changePassword("user", "pass")
//      verify(restTemplate).postForEntity<Void>(anyString(), any(), any(), anyString())
//    }
  }
}
