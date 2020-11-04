package uk.gov.justice.digital.hmpps.oauth2server.nomis.model

class NomisUserPersonDetailsHelper {

  companion object {
    fun createSampleNomisUser(
      profile: String = "TAG_GENERAL",
      staff: Staff = Staff(firstName = "bob", status = "ACTIVE", lastName = "Smith", staffId = 1),
      username: String = "bob",
      accountStatus: String = "OPEN"
    ): NomisUserPersonDetails {
      val detail = AccountDetail("user", accountStatus, profile, null)
      return NomisUserPersonDetails(username = username, staff = staff, accountDetail = detail)
    }
  }
}
