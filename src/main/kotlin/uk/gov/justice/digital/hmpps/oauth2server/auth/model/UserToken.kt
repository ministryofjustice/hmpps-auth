package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "USER_TOKEN")
class UserToken(
  @Id
  @Column(name = "token", nullable = false)
  val token: String,

  @Column(name = "token_type", nullable = false)
  @Enumerated(EnumType.STRING)
  val tokenType: TokenType,

  @Column(name = "token_expiry", nullable = false)
  var tokenExpiry: LocalDateTime,

  @ManyToOne
  @JoinColumn(name = "user_id")
  val user: User,
) {
  companion object {
    private fun generateIntAsString(): String = try {
      val random = SecureRandom.getInstance("DRBG").nextInt(1000000)
      String.format("%06d", random)
    } catch (e: NoSuchAlgorithmException) {
      throw RuntimeException(e)
    }
  }

  internal constructor(tokenType: TokenType, user: User) : this(
    if (tokenType == TokenType.MFA_CODE || tokenType == TokenType.MOBILE) generateIntAsString() else UUID.randomUUID()
      .toString(),
    tokenType,
    LocalDateTime.now(),
    user,
  ) {
    resetExpiry()
  }

  fun resetExpiry() {
    val now = LocalDateTime.now()
    tokenExpiry =
      if (tokenType == TokenType.ACCOUNT || tokenType == TokenType.CHANGE || tokenType == TokenType.MFA) now.plusMinutes(20) else now.plusDays(1)
  }

  fun hasTokenExpired(): Boolean = tokenExpiry.isBefore(LocalDateTime.now())

  override fun toString(): String =
    "UserToken(token=$token, tokenType=$tokenType, tokenExpiry=$tokenExpiry)"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as UserToken

    if (token != other.token) return false
    if (user != other.user) return false

    return true
  }

  override fun hashCode(): Int {
    var result = token.hashCode()
    result = 31 * result + user.hashCode()
    return result
  }

  enum class TokenType(val description: String) {
    RESET("ResetPassword"),
    VERIFIED("VerifiedPassword"),
    CHANGE("ChangePassword"),
    ACCOUNT("ChangeAccountDetails"),
    MFA("MFA"),
    MFA_CODE("MFACode"),
    SECONDARY("SecondEmailVerifyCode"),
    MOBILE("MobileVerifyCode");
  }
}
