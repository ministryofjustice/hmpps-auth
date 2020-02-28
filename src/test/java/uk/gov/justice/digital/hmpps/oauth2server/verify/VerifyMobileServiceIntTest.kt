package uk.gov.justice.digital.hmpps.oauth2server.verify

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockito_kotlin.mock
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.service.notify.NotificationClientApi

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
@Transactional(transactionManager = "authTransactionManager")
open class VerifyMobileServiceIntTest {
  @Autowired
  private lateinit var verifyMobileService: VerifyMobileService
  @Autowired
  private lateinit var userRepository: UserRepository
  private val telemetryClient: TelemetryClient = mock()
  private val notificationClient: NotificationClientApi = mock()

  @BeforeEach
  fun setUp() {
    verifyMobileService = VerifyMobileService(userRepository, null, telemetryClient, notificationClient, "templateId")
  }

  @Test
  fun existingMobile_NotFound() {
    val mobile = verifyMobileService.getMobile("CA_USER")
    assertThat(mobile).isEmpty()
  }

  @Test
  fun mobileNumberSetToNotVerified() {
    val userBefore = userRepository.findByUsername("AUTH_CHANGE_MOBILE_VERIFIED")
    assertTrue(userBefore.get().isMobileVerified)
    verifyMobileService.requestVerification("AUTH_CHANGE_MOBILE_VERIFIED", "07700 900322")
    val userAfter = userRepository.findByUsername("AUTH_CHANGE_MOBILE_VERIFIED")
    assertFalse(userAfter.get().isMobileVerified)
  }

  @Test
  fun mobileNumber_WhiteSpaceRemoved() {
    verifyMobileService.requestVerification("AUTH_CHANGE_MOBILE_VERIFIED", "07700 900323")
    val userAfter = userRepository.findByUsername("AUTH_CHANGE_MOBILE_VERIFIED")
    assertThat(userAfter.get().mobile).isEqualTo("07700900323")
  }

  @Test
  fun mobileNumber_WhiteSpaceRemovedForInternationalUKNumber() {
    verifyMobileService.requestVerification("AUTH_CHANGE_MOBILE_VERIFIED", "+44 7700 900323")
    val userAfter = userRepository.findByUsername("AUTH_CHANGE_MOBILE_VERIFIED")
    assertThat(userAfter.get().mobile).isEqualTo("+447700900323")
  }
}
