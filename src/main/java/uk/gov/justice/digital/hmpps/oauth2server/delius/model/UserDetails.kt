package uk.gov.justice.digital.hmpps.oauth2server.delius.model

data class UserDetails(val surname: String,
                       val firstName: String,
                       val email: String,
                       val locked: Boolean,
                       val roles: List<UserRole>)

data class UserRole(val name: String, val description: String)
