package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource

class UserTest {
  @Test
  fun `test create token overwrites previous`() {
    val user = User.of("user")
    user.createToken(UserToken.TokenType.RESET)
    val changeToken = user.createToken(UserToken.TokenType.CHANGE)
    val resetToken = user.createToken(UserToken.TokenType.RESET)
    assertThat(user.tokens).containsOnly(changeToken, resetToken)
    assertThat(user.tokens).extracting<String, RuntimeException> { obj: UserToken -> obj.token }.containsOnly(changeToken.token, resetToken.token)
  }

  @Test
  fun toUser() {
    val user = User.of("user")
    assertThat(user.toUser()).isSameAs(user)
  }

  @Test
  fun `to user auth source`() {
    val user = User.builder().username("user").source(AuthSource.auth).build().toUser()
    assertThat(user.source).isEqualTo(AuthSource.auth)
  }
}
