package uk.gov.justice.digital.hmpps.oauth2server.nomis.model

enum class AccountStatus(
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
  OPEN(0, "OPEN", false, false, false, false),
  EXPIRED(1, "EXPIRED", true, false, false, false),
  EXPIRED_GRACE(2, "EXPIRED(GRACE)", true, false, true, false),
  LOCKED_TIMED(4, "LOCKED(TIMED)", false, true, false, true),
  LOCKED(8, "LOCKED", false, true, false, false),
  EXPIRED_LOCKED_TIMED(5, "EXPIRED & LOCKED(TIMED)", true, true, false, true),
  EXPIRED_GRACE_LOCKED_TIMED(6, "EXPIRED(GRACE) & LOCKED(TIMED)", true, true, true, true),
  EXPIRED_LOCKED(9, "EXPIRED & LOCKED", true, true, false, false),
  EXPIRED_GRACE_LOCKED(10, "EXPIRED(GRACE) & LOCKED", true, true, true, false);

  companion object {
    fun get(code: Int): AccountStatus = values().filter { it.code == code }.first()

    fun get(desc: String): AccountStatus = values().filter { it.desc == desc }.first()
  }
}
