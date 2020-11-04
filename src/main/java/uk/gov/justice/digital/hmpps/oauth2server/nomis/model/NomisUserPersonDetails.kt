package uk.gov.justice.digital.hmpps.oauth2server.nomis.model

import org.apache.commons.lang3.StringUtils
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.oauth2server.auth.model.User
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails
import java.time.LocalDateTime
import java.util.EnumSet
import java.util.Objects
import java.util.Set
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.PrimaryKeyJoinColumn
import javax.persistence.SecondaryTable
import javax.persistence.Table

@Entity
@Table(name = "STAFF_USER_ACCOUNTS")
@SecondaryTable(name = "SYS.USER$", pkJoinColumns = [PrimaryKeyJoinColumn(name = "NAME")])
class NomisUserPersonDetails(
  @Id
  @Column(name = "USERNAME", nullable = false)
  private val username: String,
  @OneToOne(cascade = [CascadeType.ALL])
  @PrimaryKeyJoinColumn
  val accountDetail: AccountDetail,
  @ManyToOne
  @JoinColumn(name = "STAFF_ID")
  val staff: Staff,
  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "USERNAME")
  val roles: List<UserCaseloadRole> = listOf()
) : UserPersonDetails {

  @Column(name = "SPARE4", table = "SYS.USER$")
  private var password: String? = null

  @Column(name = "STAFF_USER_TYPE", nullable = false)
  var type: String? = null

  @Column(name = "WORKING_CASELOAD_ID")
  var activeCaseLoadId: String? = null

  constructor(
    username: String,
    password: String?,
    staff: Staff,
    type: String?,
    activeCaseLoadId: String?,
    roles: List<UserCaseloadRole>,
    accountDetail: AccountDetail
  ) : this(username, accountDetail, staff, roles) {
    this.password = password
    this.type = type
    this.activeCaseLoadId = activeCaseLoadId
  }

  fun filterRolesByCaseload(caseload: String): List<UserCaseloadRole> = roles.stream()
    .filter { (id) -> id.caseload == caseload }
    .collect(Collectors.toList())

  override fun getName(): String = staff.name

  override fun getFirstName(): String = staff.getFirstName()

  val lastName: String
    get() = staff.lastName

  override fun getUserId(): String = staff.staffId.toString()

  override fun isAdmin(): Boolean = accountDetail.accountProfile === AccountProfile.TAG_ADMIN

  override fun getAuthSource(): String = "nomis"

  override fun toUser(): User = User.builder().username(username).source(AuthSource.nomis).build()

  override fun getAuthorities(): Collection<GrantedAuthority?> {
    val roles = filterRolesByCaseload("NWEB").stream()
      .filter { obj: UserCaseloadRole? -> Objects.nonNull(obj) }
      .map { role: UserCaseloadRole ->
        SimpleGrantedAuthority(
          "ROLE_" + StringUtils.upperCase(
            role.role.code.replace(
              '-',
              '_'
            )
          )
        )
      }
    return Stream.concat(roles, Set.of(SimpleGrantedAuthority("ROLE_PRISON")).stream()).collect(Collectors.toSet())
  }

  override fun isAccountNonExpired(): Boolean {
    return true
  }

  override fun isAccountNonLocked(): Boolean =
    EnumSet.of(AccountStatus.OPEN, AccountStatus.EXPIRED, AccountStatus.EXPIRED_GRACE)
      .contains(accountDetail.status)

  override fun isCredentialsNonExpired(): Boolean {
    val statusNonExpired =
      !EnumSet.of(AccountStatus.EXPIRED, AccountStatus.EXPIRED_LOCKED, AccountStatus.EXPIRED_LOCKED_TIMED).contains(
        accountDetail.status
      )
    val passwordExpiry = accountDetail.passwordExpiry
    return statusNonExpired && (passwordExpiry == null || passwordExpiry.isAfter(LocalDateTime.now()))
  }

  override fun isEnabled(): Boolean {
    return EnumSet.of(AccountStatus.OPEN, AccountStatus.EXPIRED, AccountStatus.EXPIRED_GRACE)
      .contains(accountDetail.status)
  }

  override fun eraseCredentials() {
    password = null
  }

  override fun getUsername(): String {
    return username
  }

  override fun getPassword(): String {
    return password!!
  }

  fun setPassword(password: String?) {
    this.password = password
  }

  override fun toString(): String {
    return "NomisUserPersonDetails(username=" + getUsername() + ", type=" + type + ")"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as NomisUserPersonDetails

    if (username != other.username) return false

    return true
  }

  override fun hashCode(): Int {
    return username.hashCode()
  }
}
