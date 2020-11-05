package uk.gov.justice.digital.hmpps.oauth2server.model

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.NomisUserPersonDetails
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource.Companion.fromNullableString
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails

@Suppress("DEPRECATION")
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description = "User Details")
data class UserDetail(
  @ApiModelProperty(
    required = true,
    value = "Username",
    example = "DEMO_USER1",
    position = 1
  ) val username: String,

) {

  @ApiModelProperty(
    required = true,
    value = "Active",
    example = "false",
    position = 2
  )
  var active: Boolean? = null

  @ApiModelProperty(
    required = true,
    value = "Name",
    example = "John Smith",
    position = 3
  )
  var name: String? = null

  @ApiModelProperty(
    required = true,
    value = "Authentication Source",
    notes = "auth for auth users, nomis for nomis authenticated users",
    example = "nomis",
    position = 4
  )
  var authSource: AuthSource? = null

  @Deprecated("")
  @ApiModelProperty(
    value = "Staff Id",
    notes = "Deprecated, use userId instead",
    example = "231232",
    position = 5
  )

  var staffId: Long? = null

  @Deprecated("")
  @ApiModelProperty(
    value = "Current Active Caseload",
    notes = "Deprecated, retrieve from elite2 API rather than auth",
    example = "MDI",
    position = 6
  )

  var activeCaseLoadId: String? = null

  @ApiModelProperty(
    value = "User Id",
    notes = "Unique identifier for user, will be UUID for auth users or staff ID for nomis users",
    example = "231232",
    position = 7
  )
  var userId: String? = null

  constructor(
    username: String,
    active: Boolean?,
    name: String?,
    authSource: AuthSource?,
    staffId: Long?,
    activeCaseLoadId: String?,
    userId: String?
  ) : this(username) {
    this.active = active
    this.name = name
    this.authSource = authSource
    this.staffId = staffId
    this.activeCaseLoadId = activeCaseLoadId
    this.userId = userId
  }

  companion object {
    fun fromPerson(u: UserPersonDetails): UserDetail {

      val authSource = fromNullableString(u.authSource)
      val staffId: Long?
      val activeCaseLoadId: String?
      if (authSource === AuthSource.nomis) {
        val staffUserAccount = u as NomisUserPersonDetails
        staffId = staffUserAccount.staff.staffId
        activeCaseLoadId = staffUserAccount.activeCaseLoadId
      } else {
        staffId = null
        activeCaseLoadId = null
      }
      return UserDetail(
        username = u.username,
        active = u.isEnabled,
        name = u.name,
        authSource = authSource,
        userId = u.userId,
        staffId = staffId,
        activeCaseLoadId = activeCaseLoadId
      )
    }

    fun fromUsername(username: String): UserDetail {
      return UserDetail(username = username)
    }
  }
}
