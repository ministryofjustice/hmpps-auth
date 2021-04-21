package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "OAUTH_CLIENT_DEPLOYMENT_DETAILS")
data class ClientDeployment(

  @Id
  @Column(name = "base_client_id", nullable = false)
  val baseClientId: String,

  @Enumerated(EnumType.STRING)
  @Column(name = "client_type")
  val type: ClientType? = null,

  @Column(name = "team")
  val team: String? = null,

  @Column(name = "team_contact")
  val teamContact: String? = null,

  @Column(name = "team_slack")
  val teamSlack: String? = null,

  @Enumerated(EnumType.STRING)
  val hosting: Hosting? = null,

  val namespace: String? = null,

  val deployment: String? = null,

  @Column(name = "secret_name")
  val secretName: String? = null,

  @Column(name = "client_id_key")
  val clientIdKey: String? = null,

  @Column(name = "secret_key")
  val secretKey: String? = null,
)

enum class Hosting(val description: String) {
  CLOUDPLATFORM("Cloud Platform"), OTHER("Other")
}

enum class ClientType(val description: String) {
  PERSONAL("Personal token"), SERVICE("Service token")
}
