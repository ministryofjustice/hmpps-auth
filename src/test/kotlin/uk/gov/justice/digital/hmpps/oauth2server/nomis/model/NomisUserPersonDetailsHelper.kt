package uk.gov.justice.digital.hmpps.oauth2server.nomis.model

class NomisUserPersonDetailsHelper {

  companion object {
    fun createSampleNomisUser(
      profile: String = "TAG_GENERAL",
      staff: Staff = Staff(firstName = "bob", status = "ACTIVE", lastName = "Smith", staffId = 1),
      username: String = "bob",
      accountStatus: String = "OPEN",
      activeCaseLoadId: String? = null
    ): NomisUserPersonDetails {
      val detail = AccountDetail("user", accountStatus, profile, null)
      val personDetails = NomisUserPersonDetails(username = username, staff = staff, accountDetail = detail)
      personDetails.activeCaseLoadId = activeCaseLoadId
      return personDetails
    }
  }
}
