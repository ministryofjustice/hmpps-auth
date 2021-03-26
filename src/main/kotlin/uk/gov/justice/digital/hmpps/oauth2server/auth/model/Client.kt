@file:Suppress("DEPRECATION")

package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Suppress("JpaDataSourceORMInspection")
@Entity
@Table(name = "oauth_client_details")
data class Client(
  @Id
  @Column(name = "client_id", nullable = false)
  val id: String
)
