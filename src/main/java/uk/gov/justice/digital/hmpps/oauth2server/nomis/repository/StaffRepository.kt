package uk.gov.justice.digital.hmpps.oauth2server.nomis.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.Staff

interface StaffRepository : CrudRepository<Staff, Long>
