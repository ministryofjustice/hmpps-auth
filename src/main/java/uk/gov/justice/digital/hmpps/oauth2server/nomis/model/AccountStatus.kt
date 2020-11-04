package uk.gov.justice.digital.hmpps.oauth2server.nomis.model

enum class AccountStatus( // 1010
  val code: Int,
  val desc: String,
  val isExpired: Boolean,
  val isLocked: Boolean,
  val isGracePeriod: Boolean,
  /**
   * whether the user has locked themselves out by getting password incorrect in c-nomis
   */
  val isUserLocked: Boolean
) {
  OPEN(0, "OPEN", false, false, false, false), // 0000
  EXPIRED(1, "EXPIRED", true, false, false, false), // 0001
  EXPIRED_GRACE(2, "EXPIRED(GRACE)", true, false, true, false), // 0010
  LOCKED_TIMED(4, "LOCKED(TIMED)", false, true, false, true), // 0100
  LOCKED(8, "LOCKED", false, true, false, false), // 1000
  EXPIRED_LOCKED_TIMED(5, "EXPIRED & LOCKED(TIMED)", true, true, false, true), // 0101
  EXPIRED_GRACE_LOCKED_TIMED(6, "EXPIRED(GRACE) & LOCKED(TIMED)", true, true, true, true), // 0110
  EXPIRED_LOCKED(9, "EXPIRED & LOCKED", true, true, false, false), // 1001
  EXPIRED_GRACE_LOCKED(10, "EXPIRED(GRACE) & LOCKED", true, true, true, false);

  companion object {
    fun get(code: Int): AccountStatus = values().filter { it.code == code }.first()

    fun get(desc: String): AccountStatus = values().filter { it.desc == desc }.first()
  }
}
