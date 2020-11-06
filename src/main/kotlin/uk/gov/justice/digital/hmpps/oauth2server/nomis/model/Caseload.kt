package uk.gov.justice.digital.hmpps.oauth2server.nomis.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "CASELOADS")
data class Caseload(
  @Id
  @Column(name = "CASELOAD_ID", nullable = false)
  private val id: String,
  @Column(name = "DESCRIPTION", nullable = false)
  private val name: String
)
