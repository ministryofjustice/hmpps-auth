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
  val roleCodes: List<String>? = null,
  val groupCodes: List<String>? = null,
  val status: Status = Status.ALL,
  val userSources: List<AuthSource>? = null,
) : Specification<User> {

  private val name: String? = if (name.isNullOrBlank()) null else name.trim()

  override fun toPredicate(
    root: Root<User>,
    query: CriteriaQuery<*>,
    cb: CriteriaBuilder
  ): Predicate {
    val andBuilder = ImmutableList.builder<Predicate>()

    if (!userSources.isNullOrEmpty()) {
      andBuilder.add(buildMultipleSourcesPredicate(root, cb, userSources))
    } else {
      andBuilder.add(cb.equal(root.get<Any>("source"), AuthSource.auth))
    }
    if (!roleCodes.isNullOrEmpty()) {
      val roleBuilder = ImmutableList.builder<Predicate>()
      roleCodes.forEach {
        roleBuilder.add(cb.equal(root.join<Any, Any>("authorities").get<Any>("roleCode"), it.trim().toUpperCase()))
      }
      andBuilder.add(cb.or(*roleBuilder.build().toTypedArray()))
    }
    if (!groupCodes.isNullOrEmpty()) {
      val groupBuilder = ImmutableList.builder<Predicate>()
      groupCodes.forEach {
        groupBuilder.add(cb.equal(root.join<Any, Any>("groups").get<Any>("groupCode"), it.trim().toUpperCase()))
      }
      andBuilder.add(cb.or(*groupBuilder.build().toTypedArray()))
    }
    if (!name.isNullOrBlank()) {
      andBuilder.add(buildNamePredicate(root, cb))
    }
    if (status != Status.ALL) {
      andBuilder.add(cb.equal(root.get<Any>("enabled"), status == Status.ACTIVE))
    }
    query.distinct(true)
    val personJoin = root.join<Any, Any>("person", JoinType.INNER)
    query.orderBy(cb.asc(personJoin.get<Any>("firstName")), cb.asc(personJoin.get<Any>("lastName")))
    return cb.and(*andBuilder.build().toTypedArray())
  }

  private fun buildMultipleSourcesPredicate(root: Root<User>, cb: CriteriaBuilder, sources: List<AuthSource>): Predicate {
    val orBuilder = ImmutableList.builder<Predicate>()
    for (source in sources) {
      orBuilder.add(cb.equal(root.get<Any>("source"), source))
    }
    return cb.or(*orBuilder.build().toTypedArray())
  }

  private fun buildNamePredicate(root: Root<User>, cb: CriteriaBuilder): Predicate {
    val orBuilder = ImmutableList.builder<Predicate>()
    val pattern = "%" + name!!.replace(',', ' ').replace(" [ ]*".toRegex(), "% %") + "%"
    orBuilder.add(cb.like(root.get("email"), format(pattern)))
    orBuilder.add(cb.like(root.get("username"), pattern.toUpperCase()))
    val personJoin = root.join<Any, Any>("person", JoinType.INNER)
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

  enum class Status {
    ACTIVE, INACTIVE, ALL
  }
}
