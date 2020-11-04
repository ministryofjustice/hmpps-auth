package uk.gov.justice.digital.hmpps.oauth2server.auth.model

import com.google.common.collect.ImmutableList
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.oauth2server.security.AuthSource
import uk.gov.justice.digital.hmpps.oauth2server.utils.EmailHelper.format
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.JoinType
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

class UserFilter(
  name: String? = null,
  roleCode: String? = null,
  groupCode: String? = null,
) : Specification<User> {

  private val name: String? = if (name.isNullOrBlank()) null else name.trim()
  private val roleCode: String? = if (roleCode.isNullOrBlank()) null else roleCode.trim()
  private val groupCode: String? = if (groupCode.isNullOrBlank()) null else groupCode.trim()

  override fun toPredicate(
    root: Root<User>,
    query: CriteriaQuery<*>,
    cb: CriteriaBuilder
  ): Predicate {
    val andBuilder = ImmutableList.builder<Predicate>()
    andBuilder.add(cb.equal(root.get<Any>("source"), AuthSource.auth))
    if (!roleCode.isNullOrBlank()) {
      andBuilder.add(cb.equal(root.join<Any, Any>("authorities").get<Any>("roleCode"), roleCode.toUpperCase()))
    }
    if (!groupCode.isNullOrBlank()) {
      andBuilder.add(cb.equal(root.join<Any, Any>("groups").get<Any>("groupCode"), groupCode.toUpperCase()))
    }
    if (!name.isNullOrBlank()) {
      andBuilder.add(buildNamePredicate(root, cb))
    }
    return cb.and(*andBuilder.build().toTypedArray())
  }

  private fun buildNamePredicate(root: Root<User>, cb: CriteriaBuilder): Predicate {
    val orBuilder = ImmutableList.builder<Predicate>()
    val pattern = "%" + name!!.replace(',', ' ').replace(" [ ]*".toRegex(), "% %") + "%"
    orBuilder.add(cb.like(root.get("email"), format(pattern)))
    orBuilder.add(cb.like(root.get("username"), pattern.toUpperCase()))
    val personJoin = root.join<Any, Any>("person", JoinType.LEFT)
    orBuilder.add(
      cb.like(
        cb.lower(cb.concat(cb.concat(personJoin.get("firstName"), " "), personJoin.get("lastName"))),
        pattern.toLowerCase()
      )
    )
    orBuilder.add(
      cb.like(
        cb.lower(cb.concat(cb.concat(personJoin.get("lastName"), " "), personJoin.get("firstName"))),
        pattern.toLowerCase()
      )
    )
    return cb.or(*orBuilder.build().toTypedArray())
  }
}
