package uk.gov.justice.digital.hmpps.oauth2server.verify

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserRepository
import uk.gov.justice.digital.hmpps.oauth2server.auth.repository.UserTokenRepository
import uk.gov.service.notify.NotificationClientApi

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional(transactionManager = "authTransactionManager")
class VerifyMobileServiceIntTest {
  @Autowired
  private lateinit var userRepository: UserRepository
  @Autowired
  private lateinit var userTokenRepository: UserTokenRepository
  private val telemetryClient: TelemetryClient = mock()
  private val notificationClient: NotificationClientApi = mock()
  private lateinit var verifyMobileService: VerifyMobileService

  @BeforeEach
  fun setUp() {
    verifyMobileService = VerifyMobileService(userRepository, userTokenRepository, telemetryClient, notificationClient, "templateId")
  }

  @Test
  fun existingMobile_NotFound() {
    val mobile = verifyMobileService.getMobile("CA_USER")
    assertThat(mobile).isEmpty()
  }

  @Test
  fun mobileNumberSetToNotVerified() {
    val userBefore = userRepository.findByUsername("AUTH_CHANGE_MOBILE_VERIFIED").orElseThrow()
    assertThat(userBefore.isMobileVerified).isTrue()
    verifyMobileService.changeMobileAndRequestVerification("AUTH_CHANGE_MOBILE_VERIFIED", "07700 900322")
    val userAfter = userRepository.findByUsername("AUTH_CHANGE_MOBILE_VERIFIED").orElseThrow()
    assertThat(userAfter.isMobileVerified).isFalse()
  }

  @Test
  fun mobileNumber_WhiteSpaceRemoved() {
    verifyMobileService.changeMobileAndRequestVerification("AUTH_CHANGE_MOBILE_VERIFIED", "07700 900323")
    val userAfter = userRepository.findByUsername("AUTH_CHANGE_MOBILE_VERIFIED").orElseThrow()
    assertThat(userAfter.mobile).isEqualTo("07700900323")
  }

  @Test
  fun mobileNumber_WhiteSpaceRemovedForInternationalUKNumber() {
    verifyMobileService.changeMobileAndRequestVerification("AUTH_CHANGE_MOBILE_VERIFIED", "+44 7700 900323")
    val userAfter = userRepository.findByUsername("AUTH_CHANGE_MOBILE_VERIFIED").orElseThrow()
    assertThat(userAfter.mobile).isEqualTo("+447700900323")
  }
}
