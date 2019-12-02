package uk.gov.justice.digital.hmpps.oauth2server.delius.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource

class DeliusUserPersonDetailsTest {
  @Test
  fun `to user copy username`() {
    val user = createDeliusUser().toUser()
    assertThat(user.username).isEqualTo("user")
  }

  @Test
  fun `to user copy email`() {
    val user = createDeliusUser().toUser()
    assertThat(user.email).isEqualTo("a@b.com")
  }

  @Test
  fun `to user verify email address`() {
    val user = createDeliusUser().toUser()
    assertThat(user.isVerified).isEqualTo(true)
  }

  @Test
  fun `to user delius source`() {
    val user = createDeliusUser().toUser()
    assertThat(user.source).isEqualTo(AuthSource.delius)
  }

  private fun createDeliusUser() = DeliusUserPersonDetails.builder().username("user").email("a@b.com").build()
}
